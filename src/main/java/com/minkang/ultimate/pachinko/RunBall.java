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
    private int task = -1;
    private int r = 0, c;
    private ArmorStand ball;

    public RunBall(Main plugin, Player p, Machine machine){
        this.plugin = plugin;
        this.player = p;
        this.machine = machine;
        rows = machine.rows;
        cols = machine.cols;
        interval = Math.max(1, plugin.getConfig().getInt("machine-default.tick-interval",4));
        leftBias = Math.min(100, Math.max(0, plugin.getConfig().getInt("machine-default.left-bias",52)));
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

    private void doSlotSequential(boolean isCenter){
        if (!isCenter) return;
        if (!plugin.getConfig().getBoolean("slot.enabled", true)) return;
        final int symbols = Math.max(2, plugin.getConfig().getInt("slot.symbols",10));
        final int delay = Math.max(1, plugin.getConfig().getInt("slot.suspense-delay-ticks",15));
        final int[] nums = new int[]{-1,-1,-1};
        final String prefix = plugin.getConfig().getString("slot.chat-prefix","&a[슬롯] ").replace("&","§");

        Bukkit.getScheduler().runTaskLater(plugin, ()->{
            nums[0]=rnd.nextInt(symbols);
            player.sendMessage(prefix + "첫번째: " + nums[0]);
            Bukkit.getScheduler().runTaskLater(plugin, ()->{
                nums[1]=rnd.nextInt(symbols);
                player.sendMessage(prefix + "두번째: " + nums[1]);
                Bukkit.getScheduler().runTaskLater(plugin, ()->{
                    nums[2]=rnd.nextInt(symbols);
                    player.sendMessage(prefix + plugin.getConfig().getString("messages.slot-rolling")
                            .replace("{a}", String.valueOf(nums[0]))
                            .replace("{b}", String.valueOf(nums[1]))
                            .replace("{c}", String.valueOf(nums[2]))
                            .replace("&","§"));
                    boolean allEq = (nums[0]==nums[1] && nums[1]==nums[2]);
                    if (allEq && plugin.getConfig().getBoolean("slot.jackpot-when-all-equal", true)){
                        player.sendMessage(prefix + plugin.getConfig().getString("messages.slot-jackpot").replace("&","§"));
                        plugin.getLucky().startLucky(player, machine);
                    } else {
                        player.sendMessage(prefix + plugin.getConfig().getString("messages.slot-fail").replace("&","§"));
                    }
                }, delay);
            }, delay);
        }, delay);
    }

    private void finish(){
        if (ball!=null && !ball.isDead()) {
            ball.remove();
            boolean isCenter = (c == (cols/2));
            doSlotSequential(isCenter);
            if (plugin.getLucky().isInLucky(player) && isCenter){
                plugin.getLucky().onCenterDuringLucky(player, machine);
            }
        }
    }

    private void tick(){
        if (r >= rows){
            Bukkit.getScheduler().cancelTask(task);
            finish();
            return;
        }
        double x = c + 0.5;
        double y = r + 0.3;
        Location to = machine.base.clone().add(x, y, 0.0);
        ball.teleport(to);
        if (plugin.getConfig().getBoolean("effects.particles-on-move", false)){
            to.getWorld().spawnParticle(Particle.CRIT, to, 2, .02,.02,.02,.01);
        }
        if (r % 2 == 1){
            try {
                Sound s = Sound.valueOf(plugin.getConfig().getString("effects.hit-sound","BLOCK_NOTE_BLOCK_PLING"));
                to.getWorld().playSound(to, s, 0.3f, 1.2f);
            } catch (Exception ignored){}
            int roll = rnd.nextInt(100);
            boolean goLeft = roll < leftBias;
            if (goLeft){ if (c>0) c = c-1; }
            else { if (c<cols-1) c = c+1; }
        }
        r++;
    }
}
