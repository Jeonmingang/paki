package com.minkang.ultimate.pachinko;

import org.bukkit.Material;
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
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b==null) return;

        for (Machine m : plugin.getRegistry().all()){
            boolean isStart = m.getStartBlockLocation(plugin.getRegistry()).getBlock().equals(b);
            if (isStart && b.getType()==m.getStartBlockMaterial(plugin.getRegistry())){
                e.setCancelled(true);
                Player p = e.getPlayer();

                if (!plugin.canInsertNow(p.getUniqueId())){
                    return; // 딜레이
                }

                ItemStack hand = p.getInventory().getItemInMainHand();
                if (!plugin.isBallItemForMachine(hand, m)){
                    p.sendMessage(plugin.getConfig().getString("messages.need-ball").replace("&","§"));
                    return;
                }
                int amt = hand.getAmount();
                if (amt<=0){ p.sendMessage(plugin.getConfig().getString("messages.need-ball").replace("&","§")); return; }
                hand.setAmount(amt-1);
                p.getInventory().setItemInMainHand( (hand.getAmount()<=0) ? new org.bukkit.inventory.ItemStack(Material.AIR) : hand );

                try {
                    org.bukkit.Sound s = org.bukkit.Sound.valueOf(plugin.getConfig().getString("effects.start-sound","UI_BUTTON_CLICK"));
                    p.playSound(p.getLocation(), s, 0.8f, 1.0f);
                } catch (Exception ignored){}
                p.sendMessage(plugin.getConfig().getString("messages.start").replace("&","§"));

                if (plugin.getLucky().isInLucky(p) && plugin.getLucky().isSessionMachine(p, m)){
                    plugin.getLucky().handleClick(p, m);
                }
                new RunBall(plugin, p, m).begin();
                return;
            }
        }
    }
}
