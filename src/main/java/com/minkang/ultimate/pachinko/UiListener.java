
package com.minkang.ultimate.pachinko;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class UiListener implements Listener {
    private final Main plugin;
    public UiListener(Main p){ this.plugin = p; }

    @EventHandler public void onUse(PlayerInteractEvent e){
        if (e.getAction()!= Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b==null) return;
        if (b.getType()!= Material.OAK_WALL_SIGN) return;
        if (!(b.getState() instanceof Sign)) return;
        Sign s = (Sign)b.getState();
        if (!s.getLine(0).contains("파칭코")) return;

        e.setCancelled(true);
        final Player p = e.getPlayer();

        RunBall.runSlots(plugin, p,
                // center jackpot: decide next stage
                () -> plugin.getLucky().onJackpotAndBall(p, true),
                // other jackpot: normal payout
                () -> plugin.getLucky().onJackpotAndBall(p, false));
    }
}
