package com.minkang.pachinko.listener;

import com.minkang.pachinko.game.Machine;
import com.minkang.pachinko.game.MachineManager;
import com.minkang.pachinko.game.RankingManager;
import com.minkang.pachinko.game.Settings;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class InteractListener implements Listener {

    private final MachineManager machines;
    private final Settings settings;
    private final RankingManager ranking;
    private final com.minkang.pachinko.slot.SlotManager slotManager;

    public InteractListener(MachineManager machines, Settings settings, RankingManager ranking, com.minkang.pachinko.slot.SlotManager slotManager) {
        this.machines = machines;
        this.settings = settings;
        this.ranking = ranking;
        this.slotManager = slotManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b == null) return;
        Material type = b.getType();

        // Slot lever
        if (type == Material.LEVER) {
            com.minkang.pachinko.slot.SlotMachine sm = slotManager.getByLever(b);
            if (sm != null) { e.setCancelled(true); sm.onLever(e.getPlayer(), slotManager.getSettings()); }
            return;
        }

        if (type != Material.GOLD_BLOCK && type != Material.COAL_BLOCK && type != Material.DIAMOND_BLOCK) return;

        Machine m = machines.getByBlock(b.getLocation());
        if (m == null) return;

        Player p = e.getPlayer();
        e.setCancelled(true);

        if (type == Material.GOLD_BLOCK) m.onClickGold(p, settings);
        else if (type == Material.COAL_BLOCK) m.onClickCoal(p, settings);
        else m.onClickDiamond(p, settings, ranking);
    }
}
