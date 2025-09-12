package com.minkang.ultimate.pachinko;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class PayoutDispenser {
    private final Main plugin;
    public PayoutDispenser(Main p){ this.plugin = p; }

    public void streamBalls(Location mouth, Player target, int medals){
        FileConfiguration cfg = plugin.getConfig();
        boolean enabled = cfg.getBoolean("payout-stream.enabled", true);
        if (!enabled) return;

        int per = Math.max(1, cfg.getInt("payout-stream.balls-per-medal", 10));
        int total = Math.min(cfg.getInt("payout-stream.max-total-balls", 500), medals * per);
        int interval = Math.max(1, cfg.getInt("payout-stream.drop-interval-ticks", 1));
        Material mat = Material.matchMaterial(cfg.getString("payout-stream.item-material","IRON_NUGGET"));
        Sound s = Sound.valueOf(cfg.getString("payout-stream.sound","ENTITY_EXPERIENCE_ORB_PICKUP"));

        if (total <= 0) return;
        if (target != null){
            String jm = cfg.getString("messages.jackpot").replace("{balls}", String.valueOf(total)).replace("&","§");
            target.sendMessage(jm);
        }

        Location drop = mouth.clone().add(0.5, 0.5, 0);
        final int[] count = {0};
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (count[0] >= total){
                task.cancel();
                return;
            }
            ItemStack it = new ItemStack(mat, 1);
            Item ent = drop.getWorld().dropItemNaturally(drop, it);
            ent.setPickupDelay(0);
            ent.setVelocity(new Vector((Math.random()-0.5)*0.05, 0.1, 0));
            drop.getWorld().playSound(drop, s, 0.6f, 1.2f);
            count[0]++;
        }, 0L, interval);
    }
}