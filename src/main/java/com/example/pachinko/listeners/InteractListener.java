
package com.example.pachinko.listeners;

import com.example.pachinko.UltimatePachinko;
import com.example.pachinko.machine.Machine;
import com.example.pachinko.machine.MachineManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class InteractListener implements Listener {
    private final UltimatePachinko plugin;
    public InteractListener(UltimatePachinko plugin){ this.plugin=plugin; }

    @EventHandler public void onInteract(PlayerInteractEvent e){
        if (e.getAction()!= Action.RIGHT_CLICK_BLOCK) return;
        Block b=e.getClickedBlock(); if (b==null) return;
        Material type=b.getType(); if (type!=Material.GOLD_BLOCK && type!=Material.COAL_BLOCK) return;
        MachineManager mm=plugin.machines(); Machine m=mm.getBySpecialBlock(b.getLocation()); if (m==null) return;
        e.setCancelled(true); Player p=e.getPlayer();
        if (type==Material.GOLD_BLOCK){ ItemStack hand=p.getInventory().getItemInMainHand(); m.onGoldClick(p, hand, p.isSneaking()); }
        else if (type==Material.COAL_BLOCK){ m.onCoalClick(p); }
    }
}
