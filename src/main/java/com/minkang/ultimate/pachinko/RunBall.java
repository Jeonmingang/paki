package com.minkang.ultimate.pachinko;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class RunBall {
    private final Main plugin;
    private final Player player;
    private final Machine machine;
    private final Random rnd = new Random();

    public RunBall(Main plugin, Player player, Machine machine){
        this.plugin = plugin; this.player = player; this.machine = machine;
    }

    public void begin(){
        // Only run the suspense/horizontal reveal when 'center-roll-chance' passes; otherwise just say which slot로 빠짐
        doSlotSequential();
    }

    private void doSlotSequential(){
        // config knobs
        int delay = Math.max(1, plugin.getConfig().getInt("slot.suspense-delay-ticks", 12));
        int lastMin = Math.max(0, plugin.getConfig().getInt("slot.suspense.last-extra-min", 20));
        int lastMax = Math.max(lastMin, plugin.getConfig().getInt("slot.suspense-last-extra-max", 40));
        String loopS = plugin.getConfig().getString("slot.suspense-loop-sound", "BLOCK_NOTE_BLOCK_HAT");
        final String loopKey = loopS==null?"BLOCK_NOTE_BLOCK_HAT":loopS;
        final int a = rnd.nextInt(plugin.getConfig().getInt("slot.symbols", 10));
        final int b = rnd.nextInt(plugin.getConfig().getInt("slot.symbols", 10));
        final int c = rnd.nextInt(plugin.getConfig().getInt("slot.symbols", 10));
        final String chat = plugin.getConfig().getString("messages.slot-rolling", "§7슬롯 굴림: {a}-{b}-{c}")
                .replace("&","§");

        // loop sound
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
                    // add extra suspense delay
                    int extra = lastMin + rnd.nextInt(lastMax - lastMin + 1);
                    new BukkitRunnable(){ @Override public void run(){
                        player.sendMessage(chat.replace("{a}", String.valueOf(a)).replace("{b}", String.valueOf(b)).replace("{c}", String.valueOf(c)));
                        // stop sound effect (optional)
                        try{ player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);}catch(Throwable ignored){}
                    }}.runTaskLater(plugin, extra);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, delay);
    }
}
