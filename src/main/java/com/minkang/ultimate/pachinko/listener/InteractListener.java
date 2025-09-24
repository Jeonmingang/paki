package com.minkang.ultimate.pachinko.listener;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.model.Machine;
import com.minkang.ultimate.pachinko.model.MachineManager;
import com.minkang.ultimate.pachinko.util.Text;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class InteractListener implements Listener {
    private final Main plugin;
    public InteractListener(Main plugin){ this.plugin = plugin; }

    @EventHandler
    public void onInteract(PlayerInteractEvent e){
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b == null) return;
        Player p = e.getPlayer();
        MachineManager mm = plugin.machines();
        Machine nearest = null;
        double bd = 4*4;
        for (Machine m : mm.all()){
            double d = m.getBase().distanceSquared(b.getLocation());
            if (d < bd){ bd = d; nearest = m; }
        }
        if (nearest == null) return;

        if (b.getType() == Material.GOLD_BLOCK){
            // payout button
            nearest.payOut(p);
            e.setCancelled(true);
        } else if (b.getType() == Material.DIAMOND_BLOCK){
            // "출구" touch hint
            Text.msg(p, "&7[안내] 이 블록은 &b배출구&7 입니다.");
        }
    }
}
