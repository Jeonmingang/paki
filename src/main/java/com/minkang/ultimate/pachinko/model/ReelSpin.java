
package com.minkang.ultimate.pachinko.model;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.Random;

/** 숫자판(3자리) 연출: 한 번에 하나씩만, 끝날 때 결과를 적용 */
public class ReelSpin {
    private final Random random = new Random();
    private final Machine m;
    private final Player actor;

    public ReelSpin(Machine m, Player actor){
        this.m = m; this.actor = actor;
    }

    public void runBatch(){
        final int spins = Math.max(1, m.getPendingSpins());
        m.setPendingSpins(0);
        m.setDrawingNow(true);

        final int batchDelay = Main.get().getConfig().getInt("centerSlot.batchDelayTicks", 60);
        new BukkitRunnable(){
            int done = 0;
            @Override public void run(){
                if (done >= spins){ m.setDrawingNow(false); cancel(); return; }
                doOne();
                done++;
            }
            void doOne(){
                final int digitTick = Main.get().getConfig().getInt("centerSlot.spin.digitTickInterval", 3);
                final int duration = Main.get().getConfig().getInt("centerSlot.spin.durationTicks", 60);
                new BukkitRunnable(){
                    int tick = 0;
                    int a=0,b=0,c=0;
                    @Override public void run(){
                        tick++;
                        double tripleChance = Main.get().getConfig().getDouble("centerSlot.tripleMatchChance", 0.09);
                        if (tick >= duration){
                            cancel();
                            boolean triple;
                            if (random.nextDouble() < tripleChance){
                                int d = random.nextInt(10);
                                a=b=c=d;
                                triple = true;
                            }else{
                                // 트리플이 되지 않도록 보정
                                a = random.nextInt(10); b = random.nextInt(10);
                                do { c = random.nextInt(10); } while (a==b && b==c);
                                triple = false;
                            }
                            String bar = ChatColor.GOLD + "🎰 " + a + "  " + b + "  " + c;
                            if (actor!=null && actor.isOnline()){
                                actor.sendTitle("§e숫자판 추첨", bar, 0, 30, 10);
                            }
                            if (triple){
                                Bukkit.broadcastMessage(Text.color("&e" + (actor!=null?actor.getName():"알 수 없음") + " &7님이 숫자 3개 일치!"));
                                handleTriple(actor, m);
                            }else{
                                Text.msg(actor, "&7아쉽게도 숫자가 맞지 않았습니다.");
                            }
                        }else{
                            a = random.nextInt(10); b = random.nextInt(10); c = random.nextInt(10);
                            String bar = ChatColor.GOLD + "🎰 " + a + "  " + b + "  " + c;
                            if (actor!=null && actor.isOnline()){
                                actor.sendTitle("§e숫자판 추첨", bar, 0, digitTick, 0);
                                try { actor.playSound(actor.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f); } catch (Throwable t){}
                            }
                        }
                    }
                }.runTaskTimer(Main.get(), 1L, digitTick);
            }
        }.runTaskTimer(Main.get(), 0L, batchDelay);
    }

    private void handleTriple(Player p, Machine m){
        List<Map<?,?>> stages = Main.get().getConfig().getMapList("stages");
        if (!m.isInStage() || m.getStageIndex() < 0){
            m.setInStage(true);
            m.setStageIndex(1); // 1단계로 진입
            if (p!=null){
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                p.sendTitle(Text.color("&e스테이지 진입!"), Text.color(stageName(m, stages)), 10, 40, 10);
            }
            return;
        }
        int idx = m.getStageIndex();
        if (idx >= stages.size()-1){
            // 최종 단계 보상
            int pay = 16 + random.nextInt(16);
            addPayoutWithCap(p, m, pay);
            Text.msg(p, "&6최종 스테이지 당첨! &a보상 구슬 +" + pay + " &7(다이아 블럭에서 수령)");
            return;
        }
        double chance = 0.10;
        Object ch = stages.get(idx).get("advanceChance");
        if (ch instanceof Number) chance = ((Number)ch).doubleValue();
        if (random.nextDouble() < chance){
            m.setStageIndex(idx+1);
            if (p!=null){
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.1f);
                p.sendTitle(Text.color("&e스테이지 상승!"), Text.color(stageName(m, stages)), 10, 40, 10);
            }
        }else{
            Text.msg(p, "&7다음 단계 진입 실패. 현재 단계 유지.");
        }
    }

    private String stageName(Machine m, List<Map<?,?>> stages){
        int idx = m.getStageIndex();
        if (idx < 0 || idx >= stages.size()) return Main.get().getConfig().getString("hud.normalName","&7일반 모드");
        Object n = stages.get(idx).get("name");
        return n==null? "&7(스테이지)" : String.valueOf(n);
    }

    private void addPayoutWithCap(Player p, Machine m, int add){
        List<Map<?,?>> stages = Main.get().getConfig().getMapList("stages");
        int idx = Math.max(0, Math.min(m.getStageIndex(), stages.size()-1));
        int cap = 9999;
        Object o = stages.get(idx).get("payoutCap");
        if (o instanceof Number) cap = ((Number)o).intValue();
        int cur = m.getPendingPayout();
        int allowed = Math.max(0, cap - cur);
        int give = Math.min(allowed, add);
        m.addPayout(give);
    }
}
