package com.minkang.pachinko.game;

import com.minkang.pachinko.PachinkoPlugin;
import com.minkang.pachinko.util.ItemSerializer;
import com.minkang.pachinko.util.Text;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.*;

public class Machine {
    private void showTokenBar(org.bukkit.entity.Player p, Settings s){
        p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
            new net.md_5.bungee.api.chat.TextComponent(com.minkang.pachinko.util.Text.color("&b추첨 &f"+tokens+"/"+s.getMaxTokens())));
    }


    private final int id;
    private final World world;
    private final Location coalBase, goldBase, diamondBase;
    private final Location[] hoppers = new Location[7];
    private Location indicator;

    private UUID occupant;
    private long lastBallTime = 0L;
    private long lastAction;
    private int tokens;
    
    public int getTokens(){ return tokens; }
private boolean spinning;
    private int stage; // 0 = idle, >=1 = stage
    private int dropCount; // per stage
    private boolean upgraded; // whether at least one upgrade happened in this stage

    private ItemStack exclusiveBall;

    public Machine(int id, Location coal, Location gold, Location diamond, Location[] hs, Location indicator) {
        this.id = id; this.world = gold.getWorld();
        this.coalBase = coal.clone(); this.goldBase = gold.clone(); this.diamondBase = diamond.clone();
        for (int i=0;i<7;i++) this.hoppers[i] = hs[i].clone();
        this.indicator = indicator.clone();
    }

    public int getId() { return id; }
    public UUID getOccupant() { return occupant; }

    public void setIndicator(Location l) { this.indicator = l.clone(); }
    public void setExclusiveBall(ItemStack item) { this.exclusiveBall = item==null?null:item.clone(); }

    public boolean isInteractionBlock(Location l) {
        return l.equals(coalBase) || l.equals(goldBase) || l.equals(diamondBase);
    }

    public void saveTo(YamlConfiguration y) {
        String p = "machines."+id+".";
        y.set(p+"coal", toString(coalBase));
        y.set(p+"gold", toString(goldBase));
        y.set(p+"diamond", toString(diamondBase));
        for (int i=0;i<7;i++) y.set(p+"hopper."+i, toString(hoppers[i]));
        y.set(p+"indicator", toString(indicator));
        try {
            if (exclusiveBall != null) y.set(p+"exclusiveBall", ItemSerializer.toBase64(exclusiveBall));
        } catch (IOException ignored) {}
    }

    public static Machine from(YamlConfiguration y, int id) {
        String p = "machines."+id+".";
        if (!y.contains(p+"gold")) return null;
        Location coal = fromString(y.getString(p+"coal"));
        Location gold = fromString(y.getString(p+"gold"));
        Location diamond = fromString(y.getString(p+"diamond"));
        Location[] hs = new Location[7];
        for (int i=0;i<7;i++) hs[i] = fromString(y.getString(p+"hopper."+i));
        Location ind = fromString(y.getString(p+"indicator"));
        Machine m = new Machine(id, coal, gold, diamond, hs, ind);
        if (y.contains(p+"exclusiveBall")) {
            try { m.exclusiveBall = ItemSerializer.fromBase64(y.getString(p+"exclusiveBall")); } catch (IOException ignored) {}
        }
        return m;
    }

    private static String toString(Location l) {
        return l.getWorld().getName()+","+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ()+","+l.getYaw()+","+l.getPitch();
    }
    private static Location fromString(String s) {
        if (s==null) return null;
        String[] p = s.split(",");
        World w = Bukkit.getWorld(p[0]);
        return new Location(w, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]), Float.parseFloat(p[4]), Float.parseFloat(p[5]));
    }

    private void touch() { lastAction = System.currentTimeMillis(); }

    private boolean assignIfFree(Player p) {
        if (occupant == null) {
            occupant = p.getUniqueId(); touch();
            if (indicator != null) indicator.getBlock().setType(Material.EMERALD_BLOCK);
            return true;
        }
        return occupant.equals(p.getUniqueId());
    }

    public void release() {
        occupant = null;
        spinning = false;
        tokens = 0;
        stage = 0; dropCount = 0; upgraded = false;
        if (indicator != null) indicator.getBlock().setType(Material.STONE_BRICKS);
    }

    public void onClickGold(Player p, Settings s) {
        this.lastBallTime = System.currentTimeMillis();
        if (!assignIfFree(p)) { p.sendMessage(ChatColor.RED+"다른 플레이어가 사용 중입니다."); return; }
        touch();

        // require exclusive ball if configured
        if (s.isRequireExclusive()) {
            if (exclusiveBall == null) { p.sendMessage(ChatColor.RED+"전용 구슬이 설정되지 않았습니다."); return; }
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType()==Material.AIR || !hand.isSimilar(exclusiveBall)) {
                p.sendMessage(ChatColor.RED+"전용 구슬이 필요합니다."); return;
            }
            // consume one
            hand.setAmount(hand.getAmount()-1);
            if (hand.getAmount()<=0) p.getInventory().setItemInMainHand(null);
        }

        // visual drop on stage: diamond drop per click
        if (stage>0 && exclusiveBall!=null) {
            ItemStack drop = exclusiveBall.clone(); drop.setAmount(1);
            world.dropItem(diamondBase.clone().add(0.5,1,0.5), drop);
            dropCount++;
        }

        // choose lane index (-3..+3) mapped to 0..6
        int lane = pickLane(s);
        animateBall(p, s, lane);
    }

    private int pickLane(Settings s) {
        double[] weights = (stage==0) ? s.getBaseLanes7()
                : s.getStages().get(Math.max(0, Math.min(stage-1, s.getStages().size()-1))).lanes7;
        double r = Math.random(); double acc=0;
        for (int i=0;i<7;i++){ acc+=weights[i]; if (r<=acc) return i; }
        return 3;
    }

    private void animateBall(Player p, Settings s, int lane) {
        // Build the item to show (exclusive if exists else SLIME_BALL)
        ItemStack vis = exclusiveBall!=null? exclusiveBall.clone() : new ItemStack(Material.SLIME_BALL,1);
        ItemMeta meta = vis.getItemMeta();
        if (meta!=null) { meta.setDisplayName(Text.color("&b구슬")); vis.setItemMeta(meta); }

        Location start = goldBase.clone().add(0.5, 1, 0.5);
        Location target = hoppers[lane].clone().add(0.5, 0.0, 0.5);
        Item entity = world.dropItem(start, vis);
        entity.setPickupDelay(Integer.MAX_VALUE);

        int ascend = Math.max(1, s.getAscendTicks());
        new BukkitRunnable() {
            int t=0;
            @Override public void run() {
                t++;
                Vector dir = target.clone().subtract(entity.getLocation()).toVector().multiply(0.3);
                entity.setVelocity(dir);
                if (entity.getLocation().distanceSquared(target) < 0.2) {
                    entity.remove();
                    onBallArrive(p, s, lane);
                    cancel();
                }
                if (t > 200) { entity.remove(); cancel(); }
            }
        }.runTaskTimer(PachinkoPlugin.get(), 0L, ascend);
    }

    private void onBallArrive(Player p, Settings s, int lane) {
        if (lane==3) {
            if (stage==0) {
                tokens = Math.min(s.getMaxTokens(), tokens+1);
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Text.color("&b중앙! 추첨 &f"+tokens+"/"+s.getMaxTokens())));
                // immediate entry chance on center (stage 0)
                if (Math.random() < s.getEntryChanceOnCenter()) {
                    stage = 1; dropCount=0; upgraded=false;
                    Settings.Stage stEnter = s.getStages().get(0);
                    Bukkit.broadcastMessage(Text.color(stEnter.enterBroadcast.replace("%player%", p.getName()).replace("%stage%", String.valueOf(stage))));
                    p.sendTitle(Text.color(s.getFxWinTitle()), "", 10, 40, 10);
                }
                if (s.isAutoConsume()) {
                    Bukkit.getScheduler().runTaskLater(PachinkoPlugin.get(), () -> onClickCoal(p, s), s.getAutoConsumeDelay());
                }
            } else {
                // Stage upgrade trial
                Settings.Stage st = s.getStages().get(Math.max(0, Math.min(stage-1, s.getStages().size()-1)));
                if (Math.random() < st.nextChance) {
                    int from = stage;
                    stage = Math.min(stage+1, s.getStages().size());
                    upgraded = true;
                    Bukkit.broadcastMessage(Text.color(st.upBroadcast
                            .replace("%player%", p.getName())
                            .replace("%from%", String.valueOf(from))
                            .replace("%to%", String.valueOf(stage))));
                } else {
                    // failed upgrade - nothing
                }
                // check cup burst
                Settings.Stage st2 = s.getStages().get(Math.max(0, Math.min(stage-1, s.getStages().size()-1)));
                if (!upgraded && dropCount >= st2.cup) {
                    burst(p);
                }
            }
        } else {
            String side = lane<3? "좌측" : "우측";
            p.sendMessage(Text.color("&7#"+(lane+1)+" 슬롯(호퍼)에 들어감"));
        }
    }

    public void onClickCoal(Player p, Settings s) {
        if (!assignIfFree(p)) { p.sendMessage(ChatColor.RED+"다른 플레이어가 사용 중입니다."); return; }
        if (tokens <= 0) { p.sendMessage(ChatColor.GRAY+"보류가 없습니다."); return; }
        if (spinning) return;
        tokens--;

        spinning = true;
        final int total = s.getSpinTotal();
        final int stop1 = s.getSpinFirst();
        final int stop2 = s.getSpinSecond();
        Random rng = new Random();

        new BukkitRunnable() {
            int t=0; int a=0,b=0,c=0; boolean s1=false,s2=false;
            @Override public void run() {
                t++;
                if (!s1) a = rng.nextInt(10);
                if (!s2) b = rng.nextInt(10);
                c = rng.nextInt(10);

                if (t==stop1) s1=true;
                if (t==stop2) s2=true;

                p.sendTitle(Text.color("&f&l"+a+" &7| &f&l"+b+" &7| &f&l"+c), "", 0, 10, 0);

                if (t>=total) {
                    spinning=false;
                    boolean hit = Math.random() < s.getMatchProb();
                    if (hit) {
                        stage = 1; dropCount=0; upgraded=false;
                        Settings.Stage st = s.getStages().get(0);
                        Bukkit.broadcastMessage(Text.color(st.enterBroadcast.replace("%player%", p.getName()).replace("%stage%", String.valueOf(stage))));
                        p.sendTitle(Text.color(s.getFxWinTitle()), "", 10, 40, 10);
                    } else {
                        world.playSound(goldBase, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.8f);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(PachinkoPlugin.get(), 0L, 2L);
    }

    public void onClickDiamond(Player p, Settings s, RankingManager ranking) {
        // no-op direct; diamond is used for stage drop visuals only.
    }

    private void burst(Player p) {
        p.sendTitle(Text.color(PachinkoPlugin.get().getSettings().getFxBurstTitle()), "", 10, 40, 10);
        PachinkoPlugin.get().getRankingManager().recordBurst(p.getUniqueId(), dropCount);
        release();
    }

    private void announceLane(org.bukkit.entity.Player p, int laneIndex) {
        // 1~7 기준 중앙=4
        String side = (laneIndex < 4) ? "왼쪽" : (laneIndex > 4 ? "오른쪽" : "중앙");
        p.sendMessage("§7" + laneIndex + "번 슬롯(" + side + ")에 들어감");
    }
    

    public void checkIdleTimeout(int timeoutSeconds) {
        if (occupant == null) return;
        long now = System.currentTimeMillis();
        if (lastBallTime == 0L) lastBallTime = now; // set on first occupy
        if ((now - lastBallTime) >= timeoutSeconds * 1000L) {
            occupant = null;
        }
    }
    
}