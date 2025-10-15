
package com.minkang.pachinko.game;

import com.minkang.pachinko.util.Text;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;
import java.util.UUID;

public class Machine {

    private final int id;
    private final World world;
    private final Location coalBase, goldBase, diamondBase;
    private final Location[] hoppers = new Location[7];
    private Location indicator;

    private UUID occupant;
    private long lastBallTime = 0L;
    private int tokens;
    private boolean spinning;
    private int stage; // 0 idle
    private int dropCount;
    private boolean upgraded;
    private ItemStack exclusiveBall;

    public Machine(int id, Location coal, Location gold, Location diamond, Location[] hs, Location indicator) {
        this.id = id; this.world = gold.getWorld();
        this.coalBase = coal.clone(); this.goldBase = gold.clone(); this.diamondBase = diamond.clone();
        for (int i=0;i<7;i++) this.hoppers[i] = hs[i].clone();
        this.indicator = indicator.clone();
    }

    public int getId() { return id; }
    public UUID getOccupant() { return occupant; }
    public int getTokens() { return tokens; }

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
        for (int i=0;i<7;i++) y.set(p+"hopper"+i, toString(hoppers[i]));
        y.set(p+"indicator", toString(indicator));
        if (exclusiveBall != null) y.set(p+"exclusiveBall", com.minkang.pachinko.util.ItemSerializer.toBase64(exclusiveBall));
    }

    private static String toString(Location l) {
        return l.getWorld().getName()+","+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ();
    }

    private boolean assignIfFree(Player p) {
        if (occupant != null && !occupant.equals(p.getUniqueId())) return false;
        occupant = p.getUniqueId();
        return true;
    }

    public void onClickGold(Player p, Settings s) {
        if (!assignIfFree(p)) { p.sendMessage(ChatColor.RED+"다른 플레이어가 사용 중입니다."); return; }
        lastBallTime = System.currentTimeMillis();

        // 스테이지 중이면 다이아 드롭 1개
        if (stage>0 && exclusiveBall!=null) {
            ItemStack drop = exclusiveBall.clone(); drop.setAmount(1);
            world.dropItem(diamondBase.clone().add(0.5,1,0.5), drop);
            dropCount++;
        }

        int lane = pickLane(s); // 0..6, 3가 중앙
        // 간단한 지연만 두고 도착 판정 (연출 생략)
        int delay = Math.max(1, s.getAscendTicks());
        new BukkitRunnable(){ @Override public void run(){ onBallArrive(p, s, lane); } }.runTaskLater(com.minkang.pachinko.PachinkoPlugin.getInstance(), delay);
    }

    private void onBallArrive(Player p, Settings s, int lane){
        if (lane == 3) { // 중앙
            if (stage==0) {
                tokens = Math.min(s.getMaxTokens(), tokens+1);
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Text.color("&b중앙! 추첨 &f"+tokens+"/"+s.getMaxTokens())));
                // 즉시 스테이지 진입 시도
                if (Math.random() < s.getEntryChanceOnCenter()) {
                    stage = 1; dropCount=0; upgraded=false;
                    Settings.Stage stEnter = s.getStages().isEmpty()? null : s.getStages().get(0);
                    if (stEnter != null) {
                        Bukkit.broadcastMessage(Text.color(stEnter.enterBroadcast.replace("%player%", p.getName()).replace("%stage%", String.valueOf(stage))));
                    }
                }
            } else {
                // 스테이지 중 중앙 적중 → 승급 시도
                Settings.Stage st = s.getStages().get(Math.max(0, Math.min(stage-1, s.getStages().size()-1)));
                if (Math.random() < st.nextChance) {
                    int from = stage;
                    stage++;
                    upgraded = true;
                    Bukkit.broadcastMessage(Text.color(st.upBroadcast.replace("%player%", p.getName()).replace("%from%", String.valueOf(from)).replace("%to%", String.valueOf(stage))));
                }
                // 컵 체크 (실패한 상태에서만)
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
                a = rng.nextInt(10);
                if (t>=stop1) { s1=true; }
                else { b = rng.nextInt(10); }
                if (t>=stop2) { s2=true; }
                else { c = rng.nextInt(10); }

                p.sendTitle(Text.color("&f&l"+a+" &7| &f&l"+b+" &7| &f&l"+c), "", 0, 10, 0);

                if (t>=total) {
                    // 최종 고정
                    a=rng.nextInt(10); b=rng.nextInt(10); c=rng.nextInt(10);
                    p.sendTitle(Text.color("&f&l"+a+" &7| &f&l"+b+" &7| &f&l"+c), "", 0, 20, 10);
                    boolean match3 = (rng.nextDouble() < s.getDrawMatchProbability());
                    if (match3) {
                        if (stage==0) {
                            stage = 1; dropCount=0; upgraded=false;
                            Settings.Stage stEnter = s.getStages().isEmpty()? null : s.getStages().get(0);
                            if (stEnter != null) {
                                Bukkit.broadcastMessage(Text.color(stEnter.enterBroadcast.replace("%player%", p.getName()).replace("%stage%", String.valueOf(stage))));
                            }
                        }
                    }
                    spinning=false;
                    cancel();
                    // 자동 연속 소비
                    if (s.isAutoConsume() && tokens>0) {
                        new BukkitRunnable(){ @Override public void run(){ onClickCoal(p, s); } }.runTaskLater(com.minkang.pachinko.PachinkoPlugin.getInstance(), s.getAutoConsumeDelayTicks());
                    } else {
                        // 남은 추첨횟수 액션바 갱신
                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Text.color("&b추첨 &f"+tokens+"/"+s.getMaxTokens())));
                    }
                }
            }
        }.runTaskTimer(com.minkang.pachinko.PachinkoPlugin.getInstance(), 1L, 1L);
    }

    public void onClickDiamond(Player p, Settings s, RankingManager ranking) {
        // 별도 기능 없음 (향후 랭킹 등 확장)
    }

    private void burst(Player p){
        p.sendTitle(Text.color("&c버스트!"), "", 0, 30, 10);
        stage = 0; dropCount=0; upgraded=false;
    }

    private int pickLane(Settings s){
        double[] weights = (stage==0) ? s.getBaseLanes7()
                : s.getStages().get(Math.max(0, Math.min(stage-1, s.getStages().size()-1))).lanes7;
        double sum=0; for (double w:weights) sum+=w; double r=Math.random()*sum;
        for (int i=0;i<weights.length;i++){ r-=weights[i]; if (r<=0) return i; }
        return 3;
    }
}
