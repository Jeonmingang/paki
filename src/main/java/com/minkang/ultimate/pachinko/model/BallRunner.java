
package com.minkang.ultimate.pachinko.model;

import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.IntConsumer;

public class BallRunner {
    private final org.bukkit.plugin.Plugin plugin;
    private final Machine machine;
    private final int targetSlot;
    private final IntConsumer onFinish;

    public BallRunner(org.bukkit.plugin.Plugin plugin, Machine machine, int targetSlot, IntConsumer onFinish){
        this.plugin = plugin; this.machine = machine; this.targetSlot = targetSlot; this.onFinish = onFinish;
    }

    public void run(){
        // simple parabolic animation from base to chosen slot (1..7), center=4
        final Location start = machine.getBase().clone().add(0.5, 1.2, 0.5);
        final double targetX = (targetSlot - 4) * 0.4; // slots across X axis
        final int totalTicks = 22;

        final ArmorStand as = (ArmorStand) start.getWorld().spawnEntity(start, EntityType.ARMOR_STAND);
        as.setVisible(false); as.setMarker(true); as.setGravity(false); as.setSmall(true);
        as.setCustomNameVisible(false);

        new BukkitRunnable(){
            int t=0;
            @Override public void run(){
                t++;
                double progress = (double)t / totalTicks;
                double x = progress * targetX;
                double y = 1.2 + (Math.sin(progress*Math.PI) * 1.2);
                Location cur = machine.getBase().clone().add(0.5 + x, y, 0.5);
                as.teleport(cur);
                try { cur.getWorld().spawnParticle(Particle.CRIT, cur, 1); } catch (Throwable ignored){}
                if (t >= totalTicks){
                    as.remove();
                    if (onFinish!=null) onFinish.accept(targetSlot);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }
}
