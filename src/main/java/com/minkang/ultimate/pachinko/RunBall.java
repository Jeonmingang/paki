package com.minkang.ultimate.pachinko;

import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class RunBall {
    private final Main plugin;
    private final Player player;
    private final Machine machine;
    private final Random rnd = new Random();
    private int rows, cols, interval, leftBias;
    private int[] payout;
    private int task = -1;
    private int r = 0, c;
    private ArmorStand ball;

    public RunBall(Main plugin, Player p, Machine machine){
        this.plugin = plugin;
        this.player = p;
        this.machine = machine;
        rows = plugin.getConfig().getInt("machine.rows",8);
        cols = plugin.getConfig().getInt("machine.cols",7);
        interval = Math.max(1, plugin.getConfig().getInt("machine.tick-interval",2));
        leftBias = Math.min(100, Math.max(0, plugin.getConfig().getInt("machine.left-bias",52)));
        java.util.List<Integer> list = plugin.getConfig().getIntegerList("machine.hole-payout");
        payout = list.stream().mapToInt(Integer::intValue).toArray();
        c = cols/2;
    }

    public void begin(){
        World w = machine.base.getWorld();
        Location spawn = machine.base.clone().add(c+0.5, 0.3, 0.0);
        Material vis = plugin.getBallMaterial();
        ball = w.spawn(spawn, ArmorStand.class, as -> {
            as.setGravity(false); as.setMarker(true); as.setVisible(false); as.setSmall(true);
            as.setHelmet(new ItemStack(vis));
        });
        task = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, interval, interval);
    }

    private void finish(int gain){
        if (ball!=null && !ball.isDead()) {
            ball.remove();
            if (gain>0){
                int per = plugin.getConfig().getInt("payout-stream.balls-per-win",5);
                int total = Math.min(plugin.getConfig().getInt("payout-stream.max-total-balls",400), gain * per);
                Material mat = plugin.getBallMaterial();
                int every = Math.max(1, plugin.getConfig().getInt("payout-stream.drop-interval-ticks",1));
                final int[] count = {0};
                Location mouth = machine.getMouthLocation(plugin.getRegistry());
                Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                    if (count[0] >= total){ task.cancel(); return; }
                    player.getWorld().dropItemNaturally(mouth, new ItemStack(mat,1));
                    count[0]++;
                }, 0L, every);
            }
        }
    }

    private void tick(){
        if (r >= rows){
            Bukkit.getScheduler().cancelTask(task);
            int gain = (c < payout.length ? payout[c] : 0);
            finish(gain);
            return;
        }
        double x = c + 0.5;
        double y = r + 0.3;
        Location to = machine.base.clone().add(x, y, 0.0);
        ball.teleport(to);
        if (r % 2 == 1){
            int roll = rnd.nextInt(100);
            boolean goLeft = roll < leftBias;
            if (goLeft){ if (c>0) c = c-1; }
            else { if (c<cols-1) c = c+1; }
        }
        r++;
    }
}
