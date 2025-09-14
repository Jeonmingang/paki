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
        this.plugin = plugin;
        this.player = player;
        this.machine = machine;
    }

    public void begin(){
        // roll: decide if we hit center this time
        int pCenter = plugin.getConfig().getInt("global-probability.center-hit", 50);
        boolean center = rnd.nextInt(100) < Math.max(0, Math.min(100, pCenter));

        // suspense params
        String chat = plugin.getConfig().getString("messages.slot-line","&7슬롯 굴림: {a}-{b}-{c}").replace("&","§");
        int tick = plugin.getConfig().getInt("slot.suspense-delay-ticks", 10);
        int lastMin = plugin.getConfig().getInt("slot.suspense.last-extra-min", 20);
        int lastMax = plugin.getConfig().getInt("slot.suspense-last-extra-max", 40);
        String loopKey = plugin.getConfig().getString("slot.suspense-loop-sound", "BLOCK_NOTE_BLOCK_HAT");
        String stopKey = plugin.getConfig().getString("slot.suspense.stop-sound", "UI_BUTTON_CLICK");
        int a = 1 + rnd.nextInt(9);
        int b = 1 + rnd.nextInt(9);
        int c = center ? 5 : (1 + rnd.nextInt(9)); // 임의, 중앙이면 5로 보이게

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
                    // add extra suspense delay then finish
                    int extra = Math.max(0, lastMin);
                    if (lastMax > lastMin) extra = lastMin + rnd.nextInt(lastMax - lastMin + 1);
                    new BukkitRunnable(){
                        @Override public void run(){
                            player.sendMessage(chat.replace("{a}", String.valueOf(a)).replace("{b}", String.valueOf(b)).replace("{c}", String.valueOf(c)));
                            try{ player.playSound(player.getLocation(), Sound.valueOf(stopKey), 1.0f, 1.0f);}catch(Throwable ignored){}
                            onResult(center);
                        }
                    }.runTaskLater(plugin, extra);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, Math.max(1, tick));
    }

    private void onResult(boolean center){
        try{
            if (center){
                // if already in lucky => click proceeds; else start new lucky
                if (plugin.getLucky().isInLucky(player) && plugin.getLucky().isSessionMachine(player, machine)){
                    plugin.getLucky().handleClick(player, machine);
                }else{
                    plugin.getLucky().startLucky(player, machine);
                }
            }else{
                // not center: just inform (no stage progress)
                String miss = plugin.getConfig().getString("messages.miss-center","&7중앙 실패!").replace("&","§");
                player.sendMessage(miss);
            }
        }catch(Throwable ignored){}
    }
}
