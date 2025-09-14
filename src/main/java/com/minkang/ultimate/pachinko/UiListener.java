package com.minkang.ultimate.pachinko;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class UiListener implements Listener {
    private final Main plugin;
    public UiListener(Main p){ this.plugin = p; }

    @EventHandler
    public void onInteract(PlayerInteractEvent e){
        // main hand only (avoid off-hand double fire)
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b == null) return;

        // find which machine's start block was clicked
        for (Machine m : plugin.getRegistry().all()){
            org.bukkit.Location start = m.getStartBlockLocation(plugin.getRegistry());
            if (start.getWorld() == null || b.getWorld() == null) continue;
            if (!start.getWorld().equals(b.getWorld())) continue;
            if (start.getBlockX() == b.getX() && start.getBlockY() == b.getY() && start.getBlockZ() == b.getZ()){
                // optional: material check
                Material want = m.getStartBlockMaterial(plugin.getRegistry());
                if (b.getType() != want) {
                    // allow even if material changed? we keep strict for now
                    // continue;
                }
                e.setCancelled(true);
                Player p = e.getPlayer();

                // anti-spam
                if (!plugin.canInsertNow(p.getUniqueId())) return;

                // need ball
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (!plugin.isBallItemForMachine(hand, m)){
                    String msg = plugin.getConfig().getString("messages.need-ball", "&c구슬을 손에 들어주세요.").replace("&","§");
                    p.sendMessage(msg);
                    return;
                }

                // consume 1
                int amt = hand.getAmount();
                if (amt <= 1){
                    p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                } else {
                    hand.setAmount(amt - 1);
                    p.getInventory().setItemInMainHand(hand);
                }

                // start sound
                try {
                    String key = plugin.getConfig().getString("effects.start-sound", "UI_BUTTON_CLICK");
                    Sound s = Sound.valueOf(key);
                    p.playSound(p.getLocation(), s, 0.8f, 1.0f);
                } catch (Throwable ignored){}

                // start message
                String startMsg = plugin.getConfig().getString("messages.start", "&a시작!").replace("&","§");
                p.sendMessage(startMsg);

                // if already in a running lucky session for this machine, delegate click to manager
                try{
                    if (plugin.getLucky().isInLucky(p) && plugin.getLucky().isSessionMachine(p, m)){
                        plugin.getLucky().handleClick(p, m);
                    }
                }catch(Throwable ignored){}

                // slot suspense reveal
                new RunBall(plugin, p, m).begin();
                return;
            }
        }
    }
}
