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
            boolean isStartBlock = m.getStartBlockLocation(plugin.getRegistry()).getBlock().equals(b);
            if (isStartBlock && b.getType()==m.getStartBlockMaterial(plugin.getRegistry())){
                e.setCancelled(true);
                Player p = e.getPlayer();

                // Lucky session click?
                if (plugin.getLucky().isInLucky(p)){
                    plugin.getLucky().handleClick(p, m);
                    return;
                }

                // Normal start: consume 1 ball-item from inventory
                Material want = plugin.getBallMaterial();
                int slot = -1;
                for (int i=0;i<p.getInventory().getSize();i++){
                    ItemStack it = p.getInventory().getItem(i);
                    if (it!=null && it.getType()==want){ slot=i; break; }
                }
                if (slot==-1){
                    p.sendMessage(plugin.getConfig().getString("messages.need-ball").replace("&","§"));
                    return;
                }
                ItemStack it = p.getInventory().getItem(slot);
                it.setAmount(it.getAmount()-1);
                if (it.getAmount()<=0) p.getInventory().setItem(slot, null);
                else p.getInventory().setItem(slot, it);

                try {
                    org.bukkit.Sound s = org.bukkit.Sound.valueOf(plugin.getConfig().getString("effects.start-sound","UI_BUTTON_CLICK"));
                    p.playSound(p.getLocation(), s, 0.8f, 1.0f);
                } catch (Exception ignored){}
                p.sendMessage(plugin.getConfig().getString("messages.start").replace("&","§"));
                new RunBall(plugin, p, m).begin();
                return;
            }
        }
    }
}
