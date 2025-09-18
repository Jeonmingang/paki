
package com.minkang.ultimate.pachinko.listener;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.model.Machine;
import com.minkang.ultimate.pachinko.model.MachineManager;
import com.minkang.ultimate.pachinko.util.ItemUtil;
import com.minkang.ultimate.pachinko.util.Text;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

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
                    if (hand.getAmount() > 1) hand.setAmount(hand.getAmount()-1); else p.getInventory().setItemInMainHand(null);
                    mm.launchBall(p, m);
                } else {
                    if (!m.isDrawingNow() && m.getPendingSpins() > 0) {
                        m.setDrawingNow(true);
                        new com.minkang.ultimate.pachinko.model.ReelSpin(Main.get(), m, p).runBatch();
                    } else if (m.getPendingPayout() > 0) {
                        mm.payOutAtDiamond(p, m);
                        try { p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Text.color("&b보상 수령 완료"))); } catch (Throwable t) { }
                    } else {
                        Text.msg(p, "&7진행 중인 추첨이나 보상이 없습니다.");
                    }
                }
                return;
            }
        }
    }
}
