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
        interval = Math.max(1, plugin.getConfig().getInt("machine.tick-interval",4));
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
            Location pos = ball.getLocation().clone();
            ball.remove();

            boolean isCenter = (c == (cols/2));

            // 슬롯: Lucky가 아닐 때 가운데면 3연 뽑기
            if (!plugin.getLucky().isInLucky(player)){
                boolean slotEnabled = plugin.getConfig().getBoolean("slot.enabled", true);
                boolean centerOnly = plugin.getConfig().getBoolean("slot.center-only", true);
                boolean doSlot = slotEnabled && (!centerOnly || isCenter);
                if (doSlot){
                    int symbols = Math.max(2, plugin.getConfig().getInt("slot.symbols",10));
                    int a = rnd.nextInt(symbols), b = rnd.nextInt(symbols), cc = rnd.nextInt(symbols);
                    String prefix = plugin.getConfig().getString("slot.chat-prefix","&a[슬롯] ").replace("&","§");
                    if (plugin.getConfig().getBoolean("slot.show-each-roll", true)){
                        player.sendMessage(prefix + plugin.getConfig().getString("messages.slot-rolling").replace("{a}", String.valueOf(a)).replace("{b}", String.valueOf(b)).replace("{c}", String.valueOf(cc)).replace("&","§"));
                    }
                    boolean allEq = (a==b && b==cc);
                    if (allEq && plugin.getConfig().getBoolean("slot.jackpot-when-all-equal", true)){
                        player.sendMessage(prefix + plugin.getConfig().getString("messages.slot-jackpot").replace("&","§"));
                        plugin.getLucky().startLucky(player, machine);
                        gain = Math.max(gain, 1);
                    } else {
                        player.sendMessage(prefix + plugin.getConfig().getString("messages.slot-fail").replace("&","§"));
                    }
                }
            } else {
                // Lucky 중에 가운데로 들어오면 보너스 처리
                if (isCenter){
                    plugin.getLucky().onCenterDuringLucky(player, machine);
                }
            }

            if (gain>0){
                try {
                    Sound s = Sound.valueOf(plugin.getConfig().getString("effects.jackpot-sound","ENTITY_PLAYER_LEVELUP"));
                    pos.getWorld().playSound(pos, s, 1.0f, 1.1f);
                } catch (Exception ignored){}

                int per = plugin.getConfig().getInt("payout-stream.balls-per-win",5);
                int total = Math.min(plugin.getConfig().getInt("payout-stream.max-total-balls",400), gain * per);
                Material mat = plugin.getBallMaterial();
                int every = Math.max(1, plugin.getConfig().getInt("payout-stream.drop-interval-ticks",2));
                final int[] count = {0};
                Location hole = machine.getPayoutHoleLocation(plugin.getRegistry());
                Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                    if (count[0] >= total){ task.cancel(); return; }
                    pos.getWorld().dropItemNaturally(hole, new ItemStack(mat,1));
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
