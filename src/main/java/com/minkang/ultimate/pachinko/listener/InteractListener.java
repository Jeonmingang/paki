
package com.minkang.ultimate.pachinko.listener;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.model.Machine;
import com.minkang.ultimate.pachinko.model.MachineManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class InteractListener implements Listener {

    private final Main plugin;
    private final MachineManager manager;

    public InteractListener(Main plugin) {
        this.plugin = plugin;
        this.manager = plugin.getMachineManager();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b == null) return;

        Machine m = manager.getMachineBySpecialBlock(b.getLocation());
        if (m == null) return;

        e.setCancelled(true);

        Material type = b.getType();
        Player p = e.getPlayer();

        if (type == Material.GOLD_BLOCK) {
            ItemStack hand = p.getInventory().getItemInMainHand();
            m.onGoldClicked(p);
            return;
        }
        if (type == Material.COAL_BLOCK) {
            m.onCoalClicked(p);
            return;
        }
        // DIAMOND_BLOCK: no action
    }
}
