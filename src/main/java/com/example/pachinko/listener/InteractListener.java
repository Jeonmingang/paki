package com.example.pachinko.listener;

import com.example.pachinko.PachinkoPlugin;
import com.example.pachinko.manager.MachineManager;
import com.example.pachinko.manager.RankingManager;
import com.example.pachinko.model.Machine;
import com.example.pachinko.model.StageDef;
import com.example.pachinko.util.ActionBarUtil;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Hopper;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.UUID;

public class InteractListener implements Listener {

    private final PachinkoPlugin plugin;
    private final MachineManager mm;

    public InteractListener(PachinkoPlugin plugin) {
        this.plugin = plugin;
        this.mm = plugin.machines();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block b = e.getClickedBlock();
        if (b == null) return;

        Material type = b.getType();
        if (type != Material.GOLD_BLOCK && type != Material.COAL_BLOCK) return;

        Machine m = mm.byBottom(b);
        if (m == null) return;

        Player p = e.getPlayer();
        e.setCancelled(true);

        // Occupancy check
        if (m.isOccupied() && !p.getUniqueId().equals(m.getOccupant())) {
            p.sendMessage(plugin.msg("in-use").replace("%player%", Bukkit.getOfflinePlayer(m.getOccupant()).getName()));
            return;
        }
        if (!m.isOccupied()) {
            m.setOccupant(p.getUniqueId());
            p.sendMessage(plugin.msg("taken-over"));
        }
        m.touch();

        if (type == Material.COAL_BLOCK) {
            // 추첨
            handleDraw(p, m);
        } else if (type == Material.GOLD_BLOCK) {
            // 금블럭: 구슬 소비 + 상승
            handleGoldClick(p, m);
        }
    }

    private boolean consumeExclusiveBall(Player p, Machine m) {
        if (!mm.isRequireExclusive() || m.getExclusiveBall() == null) return true;
        ItemStack need = m.getExclusiveBall().clone();
        for (int slot = 0; slot < p.getInventory().getSize(); slot++) {
            ItemStack cur = p.getInventory().getItem(slot);
            if (cur == null) continue;
            if (cur.isSimilar(need) && cur.getAmount() >= 1) {
                cur.setAmount(cur.getAmount() - 1);
                p.getInventory().setItem(slot, cur.getAmount() > 0 ? cur : null);
                p.updateInventory();
                return true;
            }
        }
        p.sendMessage(plugin.msg("need-token"));
        return false;
    }

    private void handleGoldClick(Player p, Machine m) {
        if (mm.isRequireExclusive() && m.getExclusiveBall() != null) {
            if (!consumeExclusiveBall(p, m)) return;
        }

        // 스테이지 상태면 다이아블럭에서 전용 구슬 배출
        if (m.getStage() > 0 && m.getExclusiveBall() != null) {
            Location drop = m.getRightBase().clone().add(0.5, 1.2, 0.5);
            ItemStack prize = m.getExclusiveBall().clone();
            Item it = p.getWorld().dropItem(drop, prize);
            it.setPickupDelay(0);
            m.incCupProduced();
            // 버스트 판정
            StageDef s = mm.getStageDef(m.getStage());
            if (s != null && m.getCupProduced() >= s.cup && !m.isAdvancedThisStage()) {
                // 실패 기록
                plugin.ranking().recordBust(p.getUniqueId(), p.getName(), m.getCupProduced());
                p.sendMessage(plugin.msg("stage-bust"));
                m.leaveStage();
            }
        }

        // 구슬 상승 (동시 가능)
        spawnBall(p, m);
    }

    private void handleDraw(Player p, Machine m) {
        if (m.getDrawTokens() <= 0) {
            p.sendMessage(ChatColor.GRAY + "추첨 포인트가 없습니다. 중앙에 넣어 적립하세요. (" + m.getDrawTokens() + "/" + mm.getDrawMaxTokens() + ")");
            return;
        }
        m.consumeDrawToken();
        p.sendMessage(plugin.msg("draw-start"));

        // 숫자 3개 스핀
        final int[] digits = new int[]{ new Random().nextInt(10), new Random().nextInt(10), new Random().nextInt(10) };
        final int[] stops = new int[]{ mm.getSpinFirstStop(), mm.getSpinSecondStop(), mm.getSpinTotal() };
        final int period = 2;

        new BukkitRunnable() {
            int tick = 0;
            boolean stopped1=false, stopped2=false, stopped3=false;
            boolean decided=false;
            int finalDigit = new Random().nextInt(10);
            boolean matched = false;

            @Override
            public void run() {
                tick += period;
                if (!stopped1) digits[0] = (digits[0] + 1) % 10;
                if (!stopped2) digits[1] = (digits[1] + 1) % 10;
                if (!stopped3) digits[2] = (digits[2] + 1) % 10;

                // 긴장감 연출: ActionBar
                ActionBarUtil.send(p, ChatColor.AQUA + " [ " + digits[0] + " | " + digits[1] + " | " + digits[2] + " ] ");

                if (tick >= stops[0] && !stopped1) {
                    stopped1 = true;
                    if (!decided) {
                        matched = Math.random() < mm.getDrawMatchProbability();
                        decided = true;
                        if (matched) { digits[0] = finalDigit; }
                    }
                }
                if (tick >= stops[1] && !stopped2) {
                    stopped2 = true;
                    if (matched) digits[1] = finalDigit;
                }
                if (tick >= stops[2] && !stopped3) {
                    stopped3 = true;
                    if (matched) digits[2] = finalDigit;
                }
                if (stopped1 && stopped2 && stopped3) {
                    cancel();
                    if (matched) {
                        // 스테이지 진입
                        int firstStage = mm.firstStageId();
                        m.enterStage(firstStage);
                        StageDef s = mm.getStageDef(firstStage);
                        if (s != null && s.enterBroadcast != null && !s.enterBroadcast.isEmpty()) {
                            String msg = s.enterBroadcast.replace("%player%", p.getName()).replace("%stage%", String.valueOf(firstStage));
                            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', msg));
                        } else {
                            Bukkit.broadcastMessage(ChatColor.YELLOW + p.getName() + " 님이 스테이지 " + firstStage + " 에 진입했습니다!");
                        }
                        p.sendMessage(plugin.msg("stage-enter").replace("%stage%", ""+firstStage));
                    } else {
                        p.sendMessage(plugin.msg("draw-fail"));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, period);
    }

    private void spawnBall(Player p, Machine m) {
        World w = p.getWorld();
        Location start = m.getMidBase().clone().add(0.5, 1.2, 0.5);
        ArmorStand stand = (ArmorStand) w.spawnEntity(start, EntityType.ARMOR_STAND);
        stand.setMarker(true);
        stand.setSmall(true);
        stand.setInvisible(true);
        stand.setGravity(false);
        stand.setHelmet(new ItemStack(Material.SLIME_BLOCK)); // 구슬 느낌용

        int lane = mm.chooseLane(m);
        int height = mm.getHeight();
        Location topLoc = m.getTopHopper(lane).clone().add(0.5, 0.7, 0.5);

        int period = mm.getAscendTicks();
        final double targetY = m.getMidBase().getY() + height;
        new BukkitRunnable() {
            @Override
            public void run() {
                Location cur = stand.getLocation().clone();
                cur.add(0, 0.3, 0);
                stand.teleport(cur);
                if (stand.getLocation().getY() >= targetY) {
                    // 수평이동 후 수납
                    new BukkitRunnable() {
                        int t=0;
                        @Override
                        public void run() {
                            t++;
                            Location c = stand.getLocation();
                            Vector v = topLoc.clone().subtract(c).toVector().multiply(0.25);
                            stand.teleport(c.clone().add(v));
                            if (c.distanceSquared(topLoc) < 0.05) {
                                this.cancel();
                                // 연출: 호퍼 내부로 빨려 들어감
                                if (mm.isShowIntoHopper()) {
                                    stand.remove();
                                } else {
                                    stand.remove();
                                }
                                // 실제 아이템 수납
                                Block hopperBlock = m.getTopHopper(lane).getBlock();
                                if (hopperBlock.getState() instanceof Hopper) {
                                    Inventory inv = ((Hopper) hopperBlock.getState()).getInventory();
                                    inv.addItem(new ItemStack(Material.SLIME_BALL, 1));
                                }
                                // 결과 처리
                                if (lane == 1) {
                                    // 중앙
                                    if (m.getStage() <= 0) {
                                        m.addDrawToken(mm.getDrawMaxTokens());
                                        ActionBarUtil.send(p, plugin.msg("lane-center")
                                                .replace("%cur%", ""+m.getDrawTokens())
                                                .replace("%max%", ""+mm.getDrawMaxTokens()));
                                    } else {
                                        // 스테이지 중: 다음 스테이지 진입/승급 추첨
                                        StageDef s = mm.getStageDef(m.getStage());
                                        if (s != null) {
                                            if (Math.random() < s.nextStageChance) {
                                                StageDef next = mm.nextStage(s.id);
                                                if (next != null) {
                                                    int from = s.id;
                                                    m.enterStage(next.id);
                                                    m.setAdvancedThisStage(true);
                                                    // 방송
                                                    String msg = s.upBroadcast != null ? s.upBroadcast : "";
                                                    if (!msg.isEmpty()) {
                                                        msg = msg.replace("%player%", p.getName()).replace("%from%", ""+from).replace("%to%", ""+next.id);
                                                        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', msg));
                                                    } else {
                                                        Bukkit.broadcastMessage(ChatColor.GOLD + p.getName() + " 님이 스테이지 " + from + " → " + next.id + " 승급!");
                                                    }
                                                } else {
                                                    // 마지막 스테이지에서 성공 -> 유지
                                                    ActionBarUtil.send(p, ChatColor.GOLD + "최종 스테이지 유지!");
                                                    m.setAdvancedThisStage(true);
                                                }
                                            }
                                        }
                                    }
                                } else if (lane == 0) {
                                    ActionBarUtil.send(p, plugin.msg("lane-left"));
                                } else {
                                    ActionBarUtil.send(p, plugin.msg("lane-right"));
                                }
                            }
                        }
                    }.runTaskTimer(plugin, 0L, period);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, period);
    }
}
