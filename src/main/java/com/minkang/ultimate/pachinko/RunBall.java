package com.minkang.ultimate.pachinko;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

public class RunBall {
    private final Main plugin;
    private final Player player;
    private final Machine machine;
    private final Random rnd = new Random();

    public RunBall(Main plugin, Player player, Machine machine){
        this.plugin = plugin;
        this.player = player;
        this.machine = machine;
    }

    public void begin(){
        // roll: decide if we hit center this time
        int pCenter = plugin.getConfig().getInt("global-probability.center-hit", 50);
        boolean center = rnd.nextInt(100) < Math.max(0, Math.min(100, pCenter));

        // suspense chat
        String chat = plugin.getConfig().getString("messages.slot-line","&7슬롯 굴림: {a}-{b}-{c}").replace("&","§");
        int tick = plugin.getConfig().getInt("slot.suspense-delay-ticks", 10);
        int lastMin = plugin.getConfig().getInt("slot.suspense.last-extra-min", 20);
        int lastMax = plugin.getConfig().getInt("slot.suspense-last-extra-max", 40);
        String loopKey = plugin.getConfig().getString("slot.suspense-loop-sound", "BLOCK_NOTE_BLOCK_HAT");
        String stopKey = plugin.getConfig().getString("slot.suspense.stop-sound", "UI_BUTTON_CLICK");
        int a = 1 + rnd.nextInt(9);
        int b = 1 + rnd.nextInt(9);
        int c = center ? 5 : (1 + rnd.nextInt(9)); // 시각용 숫자
        
        try{ player.playSound(player.getLocation(), Sound.valueOf(loopKey), 1.0f, 1.0f);}catch(Throwable ignored){}

        new BukkitRunnable(){
            int step = 0;
            @Override public void run(){
                step++;
                if (step==1){
                    player.sendMessage(chat.replace("{a}", String.valueOf(a)).replace("{b}", "?").replace("{c}", "?"));
                } else if (step==2){
                    player.sendMessage(chat.replace("{a}", String.valueOf(a)).replace("{b}", String.valueOf(b)).replace("{c}", "?"));
                } else if (step==3){
                    int extra = Math.max(0, lastMin);
                    if (lastMax > lastMin) extra = lastMin + rnd.nextInt(lastMax - lastMin + 1);
                    new BukkitRunnable(){
                        @Override public void run(){
                            player.sendMessage(chat.replace("{a}", String.valueOf(a)).replace("{b}", String.valueOf(b)).replace("{c}", String.valueOf(c)));
                            try{ player.playSound(player.getLocation(), Sound.valueOf(stopKey), 1.0f, 1.0f);}catch(Throwable ignored){}
                            playPathAnimation(center);
                        }
                    }.runTaskLater(plugin, extra);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, Math.max(1, tick));
    }

    private void playPathAnimation(boolean center){
        // Ghost ball start
        ItemStack ghostItem = plugin.createBallItemWith(machine, 1);
        org.bukkit.Location start = machine.getStartBlockLocation(plugin.getRegistry()).clone().add(0.5, 1.0, 0.5);
        Item item = player.getWorld().dropItem(start, ghostItem);
        item.setPickupDelay(32767);
        item.setGravity(false);
        // compute target hopper column (visual only)
        int cols = Math.max(1, machine.cols);
        int mid = cols/2;
        int targetCol = center ? mid : rndNonCenter(cols, mid);
        // top Y above hopper row
        int topY = machine.base.getBlockY() + machine.rows + 1;
        org.bukkit.Location top = machine.base.clone().add(targetCol + 0.5, (double) (topY - machine.base.getBlockY()) + 1.2, 0.5);

        new BukkitRunnable(){
            int phase = 0; // 0: rise, 1: move X, 2: drop-in
            int ticks = 0;
            @Override public void run(){
                if (!item.isValid()){ cancel(); return; }
                ticks++;
                if (phase==0){
                    item.teleport(item.getLocation().add(0, 0.22, 0));
                    if (item.getLocation().getY() >= top.getY()){
                        phase = 1;
                    }
                } else if (phase==1){
                    double dx = top.getX() - item.getLocation().getX();
                    double step = Math.signum(dx) * 0.22;
                    if (Math.abs(dx) <= 0.25){ phase = 2; }
                    else item.teleport(item.getLocation().add(step, 0, 0));
                } else {
                    item.teleport(item.getLocation().add(0, -0.25, 0));
                    if (ticks > 50){
                        item.remove();
                        onResult(center, targetCol);
                        cancel();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private int rndNonCenter(int cols, int mid){
        if (cols<=1) return 0;
        int v = rnd.nextInt(cols-1);
        return v >= mid ? v+1 : v;
    }

    private void onResult(boolean center, int targetCol){
        try{
            if (center){
                // 럭키 세션 처리
                if (plugin.getLucky().isInLucky(player) && plugin.getLucky().isSessionMachine(player, machine)){
                    plugin.getLucky().handleClick(player, machine);
                }else{
                    plugin.getLucky().startLucky(player, machine);
                }
            }else{
                String fmt = plugin.getConfig().getString("messages.miss-center-hopper","&7꽝! &f{slot}번 호퍼");
                fmt = fmt.replace("&","§").replace("{slot}", String.valueOf(targetCol+1));
                player.sendMessage(fmt);
            }
        }catch(Throwable ignored){}
    }
}
