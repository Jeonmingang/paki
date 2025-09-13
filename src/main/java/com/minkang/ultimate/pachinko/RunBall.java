package com.minkang.ultimate.pachinko;

import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.Random;

public class RunBall {
    private final Main plugin;
    private final Player player;
    private final Machine machine;
    private int rows, cols, interval, leftBias;
    private int task = -1;
    private int r = 0, c;
    private ArmorStand ball;
    private final Random rnd = new Random();

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
        ball = w.spawn(spawn, ArmorStand.class, as -> {
            as.setGravity(false); as.setMarker(true); as.setVisible(false); as.setSmall(true);
            as.setHelmet(new org.bukkit.inventory.ItemStack(plugin.getBallMaterial(machine)));
        });
        task = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, interval, interval);
    }

    private void playListSounds(java.util.List<String> list, Location at, float vol, float pit){
        if (list==null) return;
        for (String s : list){
            try{ at.getWorld().playSound(at, org.bukkit.Sound.valueOf(s), vol, pit); }catch(Exception ignored){}
        }
    }
    private void spawnParticles(java.util.List<String> list, Location at, int count){
        if (list==null) return;
        for (String s : list){
            try{
                org.bukkit.Particle pa = org.bukkit.Particle.valueOf(s);
                at.getWorld().spawnParticle(pa, at, count, 0.2,0.2,0.2, 0.01);
            }catch(Exception ignored){}
        }
    }

    private void doSlotSequential(boolean isCenter){
        if (!isCenter){
            int idx = c + 1;
            player.sendMessage(plugin.getConfig().getString("messages.slot-index-lose")
                    .replace("{slot}", String.valueOf(idx)).replace("&","§"));
            return;
        }
        int rollChance = Math.max(0, Math.min(100, plugin.getConfig().getInt("slot.center-roll-chance", 100)));
        if (rnd.nextInt(100) >= rollChance){
            player.sendMessage(plugin.getConfig().getString("messages.center-no-roll").replace("&","§"));
            return;
        }
        if (!plugin.getConfig().getBoolean("slot.enabled", true)) return;

        final int delay = Math.max(1, plugin.getConfig().getInt("slot.suspense-delay-ticks",12));
        final int lastMin = Math.max(0, plugin.getConfig().getInt("slot.suspense-last-extra-min", 20));
        final int lastMax = Math.max(lastMin, plugin.getConfig().getInt("slot.suspense-last-extra-max", 40));
        final String loopSound = plugin.getConfig().getString("slot.suspense-loop-sound", "BLOCK_NOTE_BLOCK_HAT");
        final String prefix = plugin.getConfig().getString("slot.chat-prefix","&a[슬롯] ").replace("&","§");

        boolean jackpot = rnd.nextInt(100) < plugin.getGlobalJackpotPercent();
        int symbols = Math.max(2, plugin.getConfig().getInt("slot.symbols",10));
        int a,b,c;
        if (jackpot){ a=rnd.nextInt(symbols); b=a; c=a; }
        else {
            a=rnd.nextInt(symbols);
            do{ b=rnd.nextInt(symbols);}while(symbols>1 && b==a);
            do{ c=rnd.nextInt(symbols);}while(symbols>2 && c==a && b==a);
        }
        final int A=a,B=b,C=c;

        // 두구두구 루프
        Bukkit.getScheduler().runTaskLater(plugin, ()->{
            try{ player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(loopSound), .7f, 1.0f);}catch(Exception ignored){}
            player.sendMessage(prefix + "첫번째: " + A);
            Bukkit.getScheduler().runTaskLater(plugin, ()->{
                try{ player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(loopSound), .7f, 1.1f);}catch(Exception ignored){}
                player.sendMessage(prefix + "두번째: " + B);
                int lastDelay = delay + (lastMax>lastMin ? rnd.nextInt(lastMax-lastMin+1)+lastMin : lastMin);
                Bukkit.getScheduler().runTaskLater(plugin, ()->{
                    player.sendMessage(prefix + plugin.getConfig().getString("messages.slot-rolling")
                            .replace("{a}", String.valueOf(A))
                            .replace("{b}", String.valueOf(B))
                            .replace("{c}", String.valueOf(C))
                            .replace("&","§"));
                    if (jackpot){
                        player.sendMessage(prefix + plugin.getConfig().getString("messages.slot-jackpot").replace("&","§"));
                        plugin.getLucky().broadcastJackpot(player, machine);
                        plugin.getLucky().startLucky(player, machine);

                        // 고퀄 잭팟 연출
                        java.util.List<String> sfx = plugin.getConfig().getStringList("effects.jackpot.sounds");
                        java.util.List<String> pfx = plugin.getConfig().getStringList("effects.jackpot.particles");
                        Location mouth = machine.getPayoutHoleLocation(plugin.getRegistry());
                        playListSounds(sfx, mouth, 1.0f, 1.0f);
                        spawnParticles(pfx, mouth, 50);
                    } else {
                        player.sendMessage(prefix + plugin.getConfig().getString("messages.slot-fail").replace("&","§"));
                    }
                }, lastDelay);
            }, delay);
        }, delay);
    }

    private void finish(){
        if (ball!=null && !ball.isDead()) {
            ball.remove();
            int jackpotIdx = plugin.getJackpotSlotIndex(cols);
            boolean isCenter = (c == jackpotIdx);
            // 확률 보정은 럭키 타임이 아닐 때만 적용
            if (!plugin.getLucky().isInLucky(player) && !isCenter){
                if (rnd.nextInt(100) < plugin.getGlobalCenterHitPercent()){
                    isCenter = true;
                }
            }
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
