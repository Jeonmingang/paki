package com.minkang.ultimate.pachinko.listener;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.model.Machine;
import com.minkang.ultimate.pachinko.model.MachineManager;
import com.minkang.ultimate.pachinko.util.ItemUtil;
import com.minkang.ultimate.pachinko.util.Text;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

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
            // payout button (operator only)
            nearest.payOut(p);
            e.setCancelled(true);
            return;
        } else if (b.getType() == Material.DIAMOND_BLOCK){
            // "출구" touch hint
            Text.msg(p, "&7[안내] 이 블록은 &b배출구&7 입니다.");
            return;
        }

        // Otherwise: treat as "insert ball" if holding configured ball
        ItemStack hand = e.getItem();
        if (hand == null){
            hand = p.getInventory().getItemInMainHand();
        }
        if (ItemUtil.isValidBall(plugin, nearest, hand)){
            boolean ok = nearest.tryInsertBall(p, hand);
            if (ok) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e){
        // If quitter is operator on any machine, end stage & release
        Player p = e.getPlayer();
        for (Machine m : plugin.machines().all()){
            if (p.getUniqueId().equals(m.getOperator())){
                m.endStage(p, "플레이어 퇴장");
            }
        }
    }
}
