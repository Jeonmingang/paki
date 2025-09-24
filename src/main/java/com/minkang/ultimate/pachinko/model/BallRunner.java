package com.minkang.ultimate.pachinko.model;

import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import com.minkang.ultimate.pachinko.util.ItemUtil;

import java.util.function.IntConsumer;

/**
 * 시각적으로 '구슬' 아이템이 유리를 타고 올라가서 호퍼로 들어가는 애니메이션만 담당.
 * 다른 게임 로직에는 관여하지 않음 (콜백 onFinish로 기존 흐름 유지).
 */
public class BallRunner {
    private final org.bukkit.plugin.Plugin plugin;
    private final Machine machine;
    private final int targetSlot; // 1..7, 4=center
    private final IntConsumer onFinish;

    public BallRunner(org.bukkit.plugin.Plugin plugin, Machine machine, int targetSlot, IntConsumer onFinish){
        this.plugin = plugin; this.machine = machine; this.targetSlot = targetSlot; this.onFinish = onFinish;
    }

    private Sound resolveSound(String s1, String s2, String s3){
        try { return Sound.valueOf(s1); } catch (Throwable ignored) {}
        try { return Sound.valueOf(s2); } catch (Throwable ignored) {}
        try { return Sound.valueOf(s3); } catch (Throwable ignored) {}
        return Sound.BLOCK_NOTE_BLOCK_PLING;
    }

    public void run(){
        // 시작 위치: 기계 베이스 앞쪽(유리 관 입구)
        Location start = machine.getBase().clone().add(0.5, 1.2, 0.5);
        final double targetX = (targetSlot - 4) * 0.4; // -3..+3 -> 좌/우 오프셋
        final int totalTicks = 28; // 전체 애니메이션 길이 (2틱 주기 실행 → 약 1.4초)

        // 표시할 "구슬" 아이템 결정 (기계 전용 구슬 > 전역 defaultBall)
        ItemStack ball = machine.getMachineBallItem();
        if (ball == null){
            try { ball = ItemUtil.readItem(plugin.getConfig().getConfigurationSection("defaultBall")); }
            catch(Throwable ignored){}
        }
        if (ball == null) ball = new ItemStack(Material.SLIME_BALL);

        // 실제로 눈에 보이는 아이템 엔티티 생성 (픽업 불가, 중력 비활성)
        final Item item = start.getWorld().dropItem(start, ball);
        try { item.setPickupDelay(Integer.MAX_VALUE); } catch (Throwable ignored) {}
        try { item.setGravity(false); } catch (Throwable ignored) {}
        try { item.setCustomNameVisible(false);} catch (Throwable ignored) {}

        new BukkitRunnable(){
            int t=0;
            @Override public void run(){
                t++;
                double progress = (double)t / totalTicks;

                // X는 목표 슬롯 쪽으로 완만하게 이동
                double x = targetX * Math.min(1.0, progress*1.4);

                // Y는 초반에 살짝 튀어오른 후 위 패널(hopper 라인)까지 상승
                double y;
                if (progress < 0.6){
                    y = 1.2 + (Math.sin(progress/0.6*Math.PI) * 1.2);
                }else{
                    double k = (progress-0.6)/0.4; if (k>1) k=1;
                    y = 1.2 + 1.2 + k * 7.0; // 약 7블럭 상승
                }

                double zoff = 1.5;
                Location cur = machine.getBase().clone().add(0.5 + x, y, zoff);

                // 이동
                try { item.teleport(cur); } catch (Throwable ignored){}

                // 약간의 파티클은 유지(보조 효과) — 필요 시 config로 끌 수 있음
                try { cur.getWorld().spawnParticle(Particle.CRIT, cur, 1); } catch (Throwable ignored){}

                if (t >= totalTicks){
                    try { item.remove(); } catch (Throwable ignored){}
                    if (onFinish != null) onFinish.accept(targetSlot);
                    try { start.getWorld().playSound(cur,
                            resolveSound("BLOCK_HOPPER_INSIDE","BLOCK_HOPPER","ENTITY_ITEM_PICKUP"),
                            0.7f, 1.0f);} catch (Throwable ignored){}
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }
}
