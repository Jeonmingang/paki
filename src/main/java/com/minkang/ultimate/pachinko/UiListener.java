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
        if (b==null || b.getType()!=Material.STONE_BUTTON) return;

        for (Machine m : plugin.getRegistry().all()){
            if (m.getStartButtonLocation(plugin.getRegistry()).getBlock().equals(b)){
                e.setCancelled(true);
                Player p = e.getPlayer();
                Material mat = plugin.getBallMaterial();
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand==null || hand.getType()!=mat){
                    p.sendMessage(plugin.getConfig().getString("messages.need-ball").replace("&","§"));
                    return;
                }
                // consume 1
                hand.setAmount(hand.getAmount()-1);
                if (hand.getAmount()<=0) p.getInventory().setItemInMainHand(null);
                p.sendMessage(plugin.getConfig().getString("messages.start").replace("&","§"));
                new RunBall(plugin, p, m).begin();
                return;
            }
        }
    }
}
