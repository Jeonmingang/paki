
package com.minkang.ultimate.pachinko.model;

import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class BallRunner {
    private final Plugin plugin;
    private final Machine m;
    private final int targetSlot;

    public BallRunner(Plugin plugin, Machine m, int targetSlot) {
        this.plugin = plugin;
        this.m = m;
        this.targetSlot = targetSlot;
    }

    public void start() {
        final World w = m.getBase().getWorld();
        final Location start = m.getBase().clone().add(0.5, 0.2, 0.5);
        final Location end = m.getHopperLocations().get(targetSlot - 1).clone().add(0.5, -0.4, 0.5);
        final double durationTicks = 40.0;
        final Vector step = end.toVector().subtract(start.toVector()).multiply(1.0 / durationTicks);

        final ArmorStand as = (ArmorStand) w.spawnEntity(start, EntityType.ARMOR_STAND);
        as.setMarker(true);
        as.setVisible(false);
        as.setSmall(true);
        as.setGravity(false);
        as.getEquipment().setHelmet(new ItemStack(Material.SLIME_BLOCK));

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!as.isValid()) { cancel(); return; }
                Location cur = as.getLocation().add(step);
                as.teleport(cur);
                try { w.spawnParticle(Particle.REDSTONE, cur, 1, new Particle.DustOptions(Color.LIME, 1.0f)); } catch (Throwable ignored) {}
                t++;
                if (t >= durationTicks) {
                    as.remove();
                    w.playSound(end, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
