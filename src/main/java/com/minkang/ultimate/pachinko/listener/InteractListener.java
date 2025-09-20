
package com.minkang.ultimate.pachinko.listener;

import com.minkang.ultimate.pachinko.model.Machine;
import com.minkang.ultimate.pachinko.model.MachineManager;
import com.minkang.ultimate.pachinko.model.ReelSpin;
import com.minkang.ultimate.pachinko.util.ItemUtil;
import com.minkang.ultimate.pachinko.util.Text;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class InteractListener implements Listener {
    private final MachineManager mm;
    public InteractListener(MachineManager mm){ this.mm = mm; }

    @EventHandler
    public void onInteract(PlayerInteractEvent e){
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock(); if (b==null) return;
        Player p = e.getPlayer();

        for (Machine m : mm.getMachines().values()){
            if (m.getGoldButton()!=null && m.getGoldButton().getBlock().equals(b)){
                e.setCancelled(true);
                ItemStack hand = p.getInventory().getItemInMainHand();
                // 금블럭: 구슬 들고 있으면 발사, 스테이지 상태면 구슬 배출
                if (hand != null && m.getBallItem()!=null && ItemUtil.isSameMarble(hand, m.getBallItem())){
                    if (hand.getAmount() > 1) hand.setAmount(hand.getAmount()-1);
                    else p.getInventory().setItemInMainHand(null);
                    mm.launchBall(p, m);
                }else if (m.isInStage()){
                    mm.dispenseStageMarbles(p, m);
                }else{
                    Text.msg(p, "&7구슬을 들고 금블럭을 우클릭하면 발사됩니다.");
                }
                return;
            }
            if (m.getDiamondBlock()!=null && m.getDiamondBlock().getBlock().equals(b)){
                e.setCancelled(true);
                mm.payOutAtDiamond(p, m); // 다이아: 모든 보상 수령
                return;
            }
            if (m.getCoalButton()!=null && m.getCoalButton().getBlock().equals(b)){
                e.setCancelled(true);
                if (!m.isDrawingNow() && m.getPendingSpins() > 0){
                    new ReelSpin(m, p).runBatch(); // 석탄: 모인 추첨 1회씩 순차 실행
                }else{
                    Text.msg(p, "&7모인 추첨 기회가 없거나 이미 추첨 중입니다.");
                }
                return;
            }
        }
    }
}
