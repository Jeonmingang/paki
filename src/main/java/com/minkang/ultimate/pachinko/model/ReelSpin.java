
package com.minkang.ultimate.pachinko.model;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.util.Text;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class ReelSpin {
    private final Random random = new Random();
    private final Machine machine;
    private final Player actor;

    public ReelSpin(Machine m, Player p){
        this.machine = m; this.actor = p;
    }

    private double stageTripleChance(){
        List<Map<?,?>> stages = Main.get().getConfig().getMapList("stages");
        int idx = machine.getStageIndex();
        if (idx>=0 && idx<stages.size()){
            Object tc = stages.get(idx).get("tripleMatchChance");
            if (tc instanceof Number) return ((Number)tc).doubleValue();
        }
        return Main.get().getConfig().getDouble("centerSlot.tripleMatchChance", 0.09);
    }

    private double effectiveTripleChance(double base){
        if (!Main.get().getConfig().getBoolean("fever.enabled", true)) return base;
        if (machine.inFever()){
            double mul = Main.get().getConfig().getDouble("fever.tripleMultiplier", 2.0);
            if (mul < 1.0) mul = 1.0;
            double val = base * mul;
            return val > 1.0 ? 1.0 : val;
        }
        return base;
    }

    public void runOne(){ runOne(null); }

    // Run exactly one spin with on-screen 3-digit animation
    public void runOne(final Runnable onFinish){
        if (machine.isDrawingNow()) return;
        machine.setDrawingNow(true);

        final int digitTick = Main.get().getConfig().getInt("centerSlot.spin.digitTickInterval", 3);
        final int totalTicks = Main.get().getConfig().getInt("centerSlot.spin.durationTicks", 60);
        final double tripleChance = Main.get().getManager().effectiveJackpotChance(machine, stageTripleChance());

        new BukkitRunnable(){
            int tick=0;
            int A=0,B=0,C=0;
            @Override public void run(){
                tick++;
                if (tick % digitTick == 0){
                    A = random.nextInt(10);
                    B = random.nextInt(10);
                    C = random.nextInt(10);
                    if (actor!=null && actor.isOnline()){
                        String mid = "&f[ &e"+A+" &f| &e"+B+" &f| &e"+C+" &f]";
                        actor.sendTitle(Text.color("&b추첨 진행"), Text.color(mid), 0, 6, 0);
                        try { actor.playSound(actor.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.7f); } catch (Throwable ignored){}
                    }
                }
                if (tick >= totalTicks){
                    cancel();
                    boolean triple = random.nextDouble() < tripleChance;
                    if (triple){
                        int d = random.nextInt(10);
                        A=B=C=d;
                    }else{
                        A = random.nextInt(10);
                        B = (A + random.nextInt(9) + 1) % 10;
                        C = random.nextInt(10);
                        if (C==A && C==B) C = (C+1)%10;
                    }

                    if (actor!=null && actor.isOnline()){
                        String sub = "&f[ &e"+A+" &f| &e"+B+" &f| &e"+C+" &f]";
                        actor.sendTitle(Text.color(triple? "&6&l★ 트리플! ★" : "&a결과"), Text.color(sub), 2, 24, 6);
                        try { actor.playSound(actor.getLocation(), triple? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9f, triple? 1.0f : 1.4f); } catch (Throwable ignored){}
                    }

                    int per = 1, cap = 64;
                    List<Map<?,?>> stages = Main.get().getConfig().getMapList("stages");
                    if (machine.getStageIndex()>=0 && machine.getStageIndex()<stages.size()){
                        Object o1 = stages.get(machine.getStageIndex()).get("payoutPerHit"); if (o1 instanceof Number) per = ((Number)o1).intValue();
                        Object o2 = stages.get(machine.getStageIndex()).get("payoutCap"); if (o2 instanceof Number) cap = ((Number)o2).intValue();
                    }
                    if (triple) {
                        machine.addPayout(per, cap);
                        Main.get().getManager().onTripleWin(actor, machine, per);
                        Main.get().getManager().onJackpot(actor, machine);
                        machine.resetMissStreak();
                    }else{
                        machine.incMissStreak();
                        int trigger = Main.get().getConfig().getInt("fever.triggerMissStreak", 10);
                        if (!machine.inFever() && machine.getMissStreak() >= trigger){
                            Main.get().getManager().startFever(actor, machine);
                        }
                    }
                    if (machine.inFever()){ machine.decFever(); }


                    // ST/時短 회수 차감 및 종료 체크
                    if (machine.inST()){ machine.decST(); if (!machine.inST()) Main.get().getManager().endSpecialModes(actor, machine); }
                    if (machine.inJidan()){ machine.decJidan(); if (!machine.inJidan()) Main.get().getManager().endSpecialModes(actor, machine); }

                    machine.setDrawingNow(false);
                    if (onFinish!=null) try{ onFinish.run(); }catch(Throwable ignored){}
                }
            }
        }.runTaskTimer(Main.get(), 1L, 1L);
    }

    public void runBatch(int n){
        if (n<=0) return;
        if (machine.isDrawingNow()) return;
        if (!machine.consumeOneSpin()) return; // consume 1
        runOne(new Runnable(){
            @Override public void run(){
                int left = machine.getPendingSpins();
                if (left>0 && (n-1)>0){
                    new org.bukkit.scheduler.BukkitRunnable(){
                        @Override public void run(){ new ReelSpin(machine, actor).runBatch(n-1); }
                    }.runTaskLater(Main.get(), 10L);
                }
            }
        });
    }
}
