package com.minkang.ultimate.pachinko.service;

import com.minkang.ultimate.pachinko.PachinkoPlugin;
import com.minkang.ultimate.pachinko.model.Machine;
import com.minkang.ultimate.pachinko.util.Msg;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PlayService implements Listener {

    private final PachinkoPlugin plugin;
    private final Random random = new Random();

    public PlayService(PachinkoPlugin plugin) {
        this.plugin = plugin;
        // Idle lock release task
        new BukkitRunnable() {
            @Override public void run() {
                long now = System.currentTimeMillis();
                for (Machine m : plugin.machines().all().values()) {
                    if (m.getOccupier() != null && now - m.getLastInteract() > 120_000L) {
                        m.setOccupier(null);
                        m.setStage(0);
                        m.setTokens(0);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;
        Block b = e.getClickedBlock();
        Material t = b.getType();
        if (t != Material.GOLD_BLOCK && t != Material.COAL_BLOCK) return;
        Machine m = plugin.machines().getByBase(b);
        if (m == null) return;

        Player p = e.getPlayer();
        e.setCancelled(true);

        // Occupancy
        if (m.getOccupier() == null) m.setOccupier(p.getUniqueId());
        if (!p.getUniqueId().equals(m.getOccupier())) {
            p.sendMessage(ChatColor.RED + "이 기계는 사용 중입니다.");
            return;
        }
        m.setLastInteract(System.currentTimeMillis());

        if (t == Material.GOLD_BLOCK) {
            handleGoldClick(p, m);
        } else if (t == Material.COAL_BLOCK) {
            handleDrawClick(p, m);
        }
    }

    private void handleGoldClick(Player p, Machine m) {
        // Require exclusive ball (consume 1) if configured & stage==0 or stage>0 (always)
        boolean require = plugin.getConfig().getBoolean("machine.require-exclusive-ball", true);
        ItemStack required = m.getExclusiveBall();
        if (require && required != null) {
            int slot = findSimilar(p.getInventory().getContents(), required);
            if (slot < 0) {
                p.sendMessage(ChatColor.RED + "전용 구슬이 필요합니다.");
                return;
            } else {
                ItemStack in = p.getInventory().getItem(slot);
                in.setAmount(in.getAmount() - 1);
                p.getInventory().setItem(slot, in.getAmount() > 0 ? in : null);
            }
        }

        // Lane selection based on stage
        String lane = pickLane(m.getStage());
        if ("left".equals(lane)) {
            Msg.action(p, "§7좌측 슬롯에 들어감");
            p.playSound(p.getLocation(), Sound.BLOCK_HOPPER_LOCK, 1f, 1.2f);
        } else if ("right".equals(lane)) {
            Msg.action(p, "§7우측 슬롯에 들어감");
            p.playSound(p.getLocation(), Sound.BLOCK_HOPPER_LOCK, 1f, 0.8f);
        } else {
            if (m.getStage() == 0) {
                int max = plugin.getConfig().getInt("draw.max-tokens", 5);
                m.setTokens(Math.min(max, m.getTokens() + 1));
                Msg.action(p, "§a중앙! 추첨 §f" + m.getTokens() + "/" + max);
            } else {
                // Stage mode: drop exclusive ball and attempt promotion
                if (m.getExclusiveBall() != null) {
                    Item drop = m.getDiamondBase().getWorld().dropItem(m.getDiamondBase().clone().add(0.5, 1.2, 0.5), m.getExclusiveBall().clone());
                    drop.setPickupDelay(10);
                }
                m.addStageDrops(1);
                if (tryPromote(m)) {
                    int from = m.getStage();
                    m.addPromotion();
                    m.setStage(from + 1);
                    broadcast(plugin.getConfig().getString("stages." + (from-1) + ".up-broadcast",
                            "&6%player% 님이 &b%from% &7→ &d%to% &6 승급!")
                            .replace("%player%", p.getName())
                            .replace("%from%", String.valueOf(from))
                            .replace("%to%", String.valueOf(from+1)));
                } else {
                    // check burst: cup reached with zero promotions
                    int stageIndex = Math.max(1, m.getStage());
                    ConfigurationSection s = findStage(stageIndex);
                    if (s != null) {
                        int cup = s.getInt("cup", 50);
                        if (m.getStageDrops() >= cup && m.getPromotions() == 0) {
                            plugin.machines().recordBurst(p.getUniqueId(), p.getName(), m.getStageDrops());
                            p.sendMessage(ChatColor.RED + "버스트! 기록이 랭킹에 등록되었습니다.");
                            reset(m);
                        }
                    }
                }
            }
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.4f);
        }
    }

    private void handleDrawClick(Player p, Machine m) {
        if (m.getTokens() <= 0) {
            p.sendMessage(ChatColor.GRAY + "적립 토큰이 없습니다. 중앙 적중으로 토큰을 채우세요.");
            return;
        }
        m.addToken(-1);
        int total = plugin.getConfig().getInt("draw.spin.total-ticks", 80);
        int firstStop = plugin.getConfig().getInt("draw.spin.first-stop", 40);
        int secondStop = plugin.getConfig().getInt("draw.spin.second-stop", 60);
        double prob = plugin.getConfig().getDouble("draw.match-probability", 0.1);

        // Simple spin animation in action bar
        new BukkitRunnable() {
            int tick = 0;
            Integer a=null,b=null,c=null;
            boolean s1=false,s2=false,s3=false;
            boolean done=false;
            @Override public void run() {
                tick++;
                if (!s1) a = random.nextInt(9);
                if (!s2) b = random.nextInt(9);
                if (!s3) c = random.nextInt(9);
                Msg.action(p, "§f[ §e" + a + " §7| §e" + b + " §7| §e" + c + " §f]");
                if (tick >= firstStop) s1 = true;
                if (tick >= secondStop) s2 = true;
                if (tick >= total) {
                    s3 = true;
                    if (!done) {
                        done = true;
                        boolean match = random.nextDouble() < prob;
                        if (match) {
                            enterStage(p, m, 1);
                        } else {
                            p.sendMessage(ChatColor.GRAY + "불일치! 남은 토큰: " + m.getTokens());
                        }
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void enterStage(Player p, Machine m, int stage) {
        m.setStage(stage);
        ConfigurationSection s = findStage(stage);
        String bc = "&e%player% 님이 &a스테이지 %stage% &e에 진입!";
        if (s != null) bc = s.getString("enter-broadcast", bc);
        broadcast(bc.replace("%player%", p.getName()).replace("%stage%", String.valueOf(stage)));
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    private boolean tryPromote(Machine m) {
        ConfigurationSection s = findStage(m.getStage());
        if (s == null) return false;
        double chance = s.getDouble("next-stage-chance", 0.2);
        return random.nextDouble() < chance;
    }

    private String pickLane(int stage) {
        if (stage <= 0) {
            ConfigurationSection base = plugin.getConfig().getConfigurationSection("probability.base.lane");
            double l = base.getDouble("left", 0.33);
            double c = base.getDouble("center", 0.34);
            double r = base.getDouble("right", 0.33);
            return choose(l, c, r);
        } else {
            ConfigurationSection s = findStage(stage);
            if (s == null) return "center";
            ConfigurationSection lane = s.getConfigurationSection("lane");
            double l = lane.getDouble("left", 0.33);
            double c = lane.getDouble("center", 0.34);
            double r = lane.getDouble("right", 0.33);
            return choose(l,c,r);
        }
    }

    private String choose(double l, double c, double r) {
        double sum = l + c + r;
        double x = random.nextDouble() * sum;
        if (x < l) return "left";
        if (x < l + c) return "center";
        return "right";
        }

    private ConfigurationSection findStage(int id) {
        List<?> list = plugin.getConfig().getList("stages");
        if (list == null) return null;
        for (int i = 0; i < list.size(); i++) {
            Object o = list.get(i);
            if (o instanceof ConfigurationSection) {
                ConfigurationSection cs = (ConfigurationSection) o;
                if (cs.getInt("id", i+1) == id) return cs;
            }
        }
        // When using ConfigurationSection inside list, Bukkit flattens differently; provide fallback
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("stages");
        if (root != null) {
            for (String key : root.getKeys(false)) {
                ConfigurationSection cs = root.getConfigurationSection(key);
                if (cs.getInt("id") == id) return cs;
            }
        }
        return null;
    }

    private void broadcast(String msg) {
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }

    private void reset(Machine m) {
        m.setStage(0);
        m.setTokens(0);
        m.setOccupier(null);
    }

    private int findSimilar(ItemStack[] arr, ItemStack target) {
        if (arr == null || target == null) return -1;
        for (int i=0;i<arr.length;i++) {
            ItemStack it = arr[i];
            if (it != null && it.isSimilar(target)) return i;
        }
        return -1;
    }
}
