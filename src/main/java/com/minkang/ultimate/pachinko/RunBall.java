package com.minkang.ultimate.pachinko;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.Random;

public class RunBall {
    private final Main plugin;
    private final Player player;
    private final Location baseFront;
    private final Random rnd = new Random();
    private int rows, cols, interval, leftBias;
    private int[] payout;
    private int task = -1;
    private int r = 0, c;
    private ArmorStand ball;

    public RunBall(Main plugin, Player p, Location baseFront){
        this.plugin = plugin;
        this.player = p;
        this.baseFront = baseFront.clone();
        FileConfiguration cfg = plugin.getConfig();
        rows = cfg.getInt("machine.rows",12);
        cols = cfg.getInt("machine.cols",9);
        interval = Math.max(1, cfg.getInt("machine.tick-interval",2));
        leftBias = Math.min(100, Math.max(0, cfg.getInt("machine.left-bias",52)));
        java.util.List<Integer> list = cfg.getIntegerList("machine.hole-payout");
        payout = list.stream().mapToInt(Integer::intValue).toArray();
        c = cols/2;
    }

    public void begin(){
        World w = baseFront.getWorld();
        Location spawn = baseFront.clone().add(c+0.5, 0.3, 0.0);
        ball = w.spawn(spawn, ArmorStand.class, as -> {
            as.setGravity(false); as.setMarker(true); as.setVisible(false); as.setSmall(true);
            as.setHelmet(new ItemStack(Material.SLIME_BALL));
        });
        task = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> tick(), interval, interval);
    }

    private void finish(int gain){
        if (ball!=null && !ball.isDead()) ball.remove();
        plugin.getStore().addMedal(player.getUniqueId(), gain);
        String msg = plugin.getConfig().getString("messages.result").replace("{medal}", String.valueOf(gain))
                .replace("{total}", String.valueOf(plugin.getStore().getMedals(player.getUniqueId()))).replace("&","§");
        player.sendMessage(msg);
        if (gain>0){
            Location mouth = baseFront.clone().add(c+0.0, rows+0.3, 0.0); // 하단 쪽에서 토출
            plugin.getDispenser().streamBalls(mouth, player, gain);
        }
    }

    private void tick(){
        boolean end = r >= rows;
        if (end){
            Bukkit.getScheduler().cancelTask(task);
            int gain = (c < payout.length ? payout[c] : 0);
            finish(gain);
            return;
        }
        double x = c + 0.5;
        double y = r + 0.3;
        Location to = baseFront.clone().add(x, y, 0.0);
        ball.teleport(to);
        boolean pinRow = (r % 2 == 1);
        if (pinRow){
            int roll = rnd.nextInt(100);
            boolean goLeft = roll < leftBias;
            if (goLeft){
                if (c>0) c = c-1;
            } else {
                if (c<cols-1) c = c+1;
            }
        }
        r++;
    }
}