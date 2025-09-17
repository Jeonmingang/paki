
package com.minkang.ultimate.pachinko.listener;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.model.Machine;
import com.minkang.ultimate.pachinko.model.MachineManager;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import com.minkang.ultimate.pachinko.util.ItemUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class InteractListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b == null) return;
        if (b.getType() != Material.GOLD_BLOCK) return;
        Player p = e.getPlayer();
        MachineManager mm = Main.get().getMachineManager();
        for (Machine m : mm.getMachines().values()) {
            if (m.getGoldButton() != null && m.getGoldButton().getBlock().equals(b)) {
                e.setCancelled(true);
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand != null && m.getBallItem()!=null && ItemUtil.isSameMarble(hand, m.getBallItem())) {
                    // consume 1 and launch
                    if (hand.getAmount() > 1) hand.setAmount(hand.getAmount()-1); else p.getInventory().setItemInMainHand(null);
                    mm.launchBall(p, m);
                } else {
                    mm.payOut(p, m);
                }
                return;
            }
        }
    }
}
