
package com.minkang.pachinko.listener;

import com.minkang.pachinko.game.Machine;
import com.minkang.pachinko.game.MachineManager;
import com.minkang.pachinko.game.RankingManager;
import com.minkang.pachinko.game.Settings;
import com.minkang.pachinko.slot.SlotMachine;
import com.minkang.pachinko.slot.SlotManager;
import com.minkang.pachinko.util.Text;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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
    private final SlotManager slotManager;

    public InteractListener(MachineManager machines, Settings settings, RankingManager ranking, SlotManager slotManager){
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

        // 슬롯머신 레버
        if (type == Material.LEVER) {
            SlotMachine sm = slotManager.getByLever(b);
            if (sm != null) {
                e.setCancelled(true);
                sm.onLever(e.getPlayer(), slotManager.getSettings());
            }
            return;
        }

        // 파칭코 3블럭 (석탄/금/다이아)
        if (type != Material.GOLD_BLOCK && type != Material.COAL_BLOCK && type != Material.DIAMOND_BLOCK) return;

        Machine m = machines.getByBlock(b.getLocation());
        if (m == null) return;

        Player p = e.getPlayer();
        e.setCancelled(true);

        if (type == Material.GOLD_BLOCK) {
            m.onClickGold(p, settings);
            // 구슬 투입 사운드
            try {
                if (settings.isInsertSoundEnabled()) {
                    org.bukkit.Sound sd = org.bukkit.Sound.valueOf(settings.getInsertSoundName());
                    p.playSound(p.getLocation(), sd, settings.getInsertSoundVolume(), settings.getInsertSoundPitch());
                }
            } catch (IllegalArgumentException ex) {
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.4f);
            }
        } else if (type == Material.COAL_BLOCK) {
            m.onClickCoal(p, settings);
            // 남은 추첨 횟수 액션바
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(Text.color("&b추첨 &f" + m.getTokens() + "/" + settings.getMaxTokens())));
        } else { // DIAMOND
            m.onClickDiamond(p, settings, ranking);
        }
    }
}
