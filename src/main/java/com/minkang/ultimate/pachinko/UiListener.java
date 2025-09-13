package com.minkang.ultimate.pachinko;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class UiListener implements Listener {
    private final Main plugin;
    public UiListener(Main p){ this.plugin = p; }

    @EventHandler
    public void onInteract(PlayerInteractEvent e){
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b==null) return;

        for (Machine m : plugin.getRegistry().all()){
            boolean isButton = m.getStartButtonLocation(plugin.getRegistry()).getBlock().equals(b);
            boolean isStartBlock = m.getStartBlockLocation(plugin.getRegistry()).getBlock().equals(b);
            if (isButton || (isStartBlock && b.getType()==m.getStartBlockMaterial(plugin.getRegistry()))){
                e.setCancelled(true);
                startIfValid(e.getPlayer(), m);
                return;
            }
        }
    }

    private void startIfValid(Player p, Machine m){
        Material mat = plugin.getBallMaterial();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand==null || hand.getType()!=mat){
            p.sendMessage(plugin.getConfig().getString("messages.need-ball").replace("&","§"));
            return;
        }
        // consume 1
        hand.setAmount(hand.getAmount()-1);
        if (hand.getAmount()<=0) p.getInventory().setItemInMainHand(null);
        // start sound
        try {
            org.bukkit.Sound s = org.bukkit.Sound.valueOf(plugin.getConfig().getString("effects.start-sound","UI_BUTTON_CLICK"));
            p.playSound(p.getLocation(), s, 0.8f, 1.0f);
        } catch (Exception ignored){}
        p.sendMessage(plugin.getConfig().getString("messages.start").replace("&","§"));
        new RunBall(plugin, p, m).begin();
    }
}
