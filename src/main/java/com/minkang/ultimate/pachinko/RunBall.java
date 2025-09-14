
package com.minkang.ultimate.pachinko;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class RunBall {

    private static final Random rnd = new Random();

    /** Simulate a slot roll and (optionally) a spinning effect. Returns the 3 numbers. */
    public static void runSlots(final Main plugin, final Player p, final Runnable onJackpotCenter, final Runnable onJackpotOther){
        final boolean spin = plugin.getConfig().getBoolean("spin-effect", true);

        final int targetA = 1 + rnd.nextInt(9);
        final int targetB = 1 + rnd.nextInt(9);
        final int targetC = 1 + rnd.nextInt(9);

        if (!spin){
            showLine(plugin, p, targetA, targetB, targetC);
            // define jackpot condition
            if (targetA==7 && targetB==7 && targetC==7){
                // determine if "center hole" or not (50/50)
                boolean center = rnd.nextBoolean();
                if (center) onJackpotCenter.run(); else onJackpotOther.run();
            }
            return;
        }

        // spin effect: numbers increase then stop one by one horizontally
        new BukkitRunnable(){
            int step = 0;
            int a=0,b=0,c=0;
            @Override public void run(){
                step++;
                if (step<=20){ a = 1 + rnd.nextInt(9); }
                if (step<=35){ b = 1 + rnd.nextInt(9); }
                if (step<=50){ c = 1 + rnd.nextInt(9); }
                showLine(plugin, p, (step<=20)?a:targetA, (step<=35)?b:targetB, (step<=50)?c:targetC);
                if (step>=50){
                    cancel();
                    if (targetA==7 && targetB==7 && targetC==7){
                        boolean center = rnd.nextBoolean();
                        if (center) onJackpotCenter.run(); else onJackpotOther.run();
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }

    private static void showLine(Main plugin, Player p, int a, int b, int c){
        String fmt = plugin.getConfig().getString("messages.slot-line", "&7슬롯: &f[{a}] &7- &f[{b}] &7- &f[{c}]");
        String msg = fmt.replace("{a}", String.valueOf(a)).replace("{b}", String.valueOf(b)).replace("{c}", String.valueOf(c)).replace("&","§");
        p.sendMessage(msg);
        p.sendTitle("", msg, 0, 10, 0);
    }
}
