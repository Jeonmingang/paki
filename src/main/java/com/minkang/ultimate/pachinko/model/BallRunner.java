
package com.minkang.ultimate.pachinko.model;

import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.function.IntConsumer;

/** 단순 연출용 구슬(갑옷 거치대) 이동 후 완료 콜백 */
public class BallRunner {
    private final Plugin plugin;
    private final Machine machine;
    private final int targetSlot;
    private final IntConsumer onFinish;

    public BallRunner(Plugin plugin, Machine machine, int targetSlot, IntConsumer onFinish){
        this.plugin = plugin;
        this.machine = machine;
        this.targetSlot = targetSlot;
        this.onFinish = onFinish;
    }

    public void start(){
        World w = machine.getBase().getWorld();
        // 시작 위치: 상단 중앙 바로 아래
        Location start = machine.getBase().clone().add(0.5, 7.8, 0.5);
        ArmorStand as = (ArmorStand) w.spawnEntity(start, EntityType.ARMOR_STAND);
        as.setGravity(false); as.setInvisible(true); as.setMarker(true); as.setSmall(true);
        ItemStack head = new ItemStack(Material.SLIME_BALL);
        as.getEquipment().setHelmet(head);

        // 목표 X: 슬롯 1~7 → -3 ~ +3
        double targetX = (targetSlot - 4); // -3..+3
        double totalTicks = 40; // 약 2초
        new BukkitRunnable(){
            int t = 0;
            @Override public void run() {
                if (!as.isValid()){ cancel(); return; }
                t++;
                double progress = t / totalTicks;
                double x = (1.0-progress) * 0.0 + progress * targetX;
                double y = 7.8 - progress * 6.2;
                Location cur = machine.getBase().clone().add(0.5 + x, y, 0.5);
                as.teleport(cur);
                try { w.spawnParticle(Particle.CRIT, cur, 1); } catch (Throwable ignored){}
                if (t >= totalTicks){
                    as.remove();
                    if (onFinish != null) onFinish.accept(targetSlot);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }
}
