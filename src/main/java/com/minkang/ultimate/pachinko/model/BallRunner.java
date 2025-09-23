
package com.minkang.ultimate.pachinko.model;
import com.minkang.ultimate.pachinko.util.Text;

import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.IntConsumer;

public class BallRunner {
    private Sound resolveSound(String primary, String fallback1, String fallback2){
        try { return Sound.valueOf(primary); } catch (Throwable ignored){}
        try { return Sound.valueOf(fallback1); } catch (Throwable ignored){}
        try { return Sound.valueOf(fallback2); } catch (Throwable ignored){}
        return Sound.BLOCK_NOTE_BLOCK_PLING;
    }

    private final org.bukkit.plugin.Plugin plugin;
    private final Machine machine;
    private final int targetSlot; // 1..7, 4=center
    private final IntConsumer onFinish;

    public BallRunner(org.bukkit.plugin.Plugin plugin, Machine machine, int targetSlot, IntConsumer onFinish){
        this.plugin = plugin; this.machine = machine; this.targetSlot = targetSlot; this.onFinish = onFinish;
    }

    public void run(){
        // 2-phase: move to slot X at mid-height, then ascend to hopper line (panel top) and drop

        final Location start = machine.getBase().clone().add(0.5, 1.2, 0.5);
        final double targetX = (targetSlot - 4) * 0.4; // slot -3..+3 -> X offset
        final int totalTicks = 28;

        final ArmorStand as = (ArmorStand) start.getWorld().spawnEntity(start, EntityType.ARMOR_STAND);
        as.setVisible(false); as.setMarker(true); as.setGravity(false); as.setSmall(true);
        as.setCustomNameVisible(false);

        new BukkitRunnable(){
            int t=0; int phase=0;
            @Override public void run(){
                t++;
                double progress = (double)t / totalTicks;
                double x = targetX * Math.min(1.0, progress*1.4);
                double y;
                double zoff = 1.5;
                if (progress < 0.6){
                    y = 1.2 + (Math.sin(progress/0.6*Math.PI) * 1.2);
                }else{
                    // ascend to hopper at panel top (base y + 8)
                    double k = (progress-0.6)/0.4; if (k>1) k=1;
                    y = 1.2 + (1.2) + k * (7.0); // rise ~7 blocks
                }
                Location cur = machine.getBase().clone().add(0.5 + x, y, zoff);
                as.teleport(cur);
                try { cur.getWorld().spawnParticle(Particle.CRIT, cur, 1); } catch (Throwable ignored){}
                if (t >= totalTicks){
                    as.remove();
                    if (onFinish!=null) onFinish.accept(targetSlot);
                    try { start.getWorld().playSound(cur, resolveSound("BLOCK_HOPPER_INSIDE","BLOCK_HOPPER","ENTITY_ITEM_PICKUP"), 0.7f, 1.0f);} catch(Throwable ignored){}
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }
}
