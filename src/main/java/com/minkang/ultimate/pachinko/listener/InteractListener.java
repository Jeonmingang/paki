package com.minkang.ultimate.pachinko.listener;

import com.minkang.ultimate.pachinko.model.Machine;
import com.minkang.ultimate.pachinko.model.MachineManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class InteractListener implements Listener {

    private final MachineManager manager;

    public InteractListener(MachineManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b == null) return;
        Player p = e.getPlayer();

        // Gold Block acts as "payout" button during stage
        if (b.getType() == Material.GOLD_BLOCK) {
            Machine m = manager.getByBase(b.getLocation());
            if (m != null && m.isStageActive()) {
                // payout one item up to cup
                if (m.getStagePayout() < m.getStageCup()) {
                    m.setStagePayout(m.getStagePayout() + 1);
                    p.getWorld().dropItemNaturally(b.getLocation().add(0.5, 1, 0.5), new ItemStack(Material.SLIME_BALL, 1));
                    p.sendMessage("§e[스테이지] §f현재 §b" + m.getStagePayout() + "§7/§e" + m.getStageCup());
                } else {
                    p.sendMessage("§c상한에 도달했습니다!");
                }
                e.setCancelled(true);
            }
        }
    }
}