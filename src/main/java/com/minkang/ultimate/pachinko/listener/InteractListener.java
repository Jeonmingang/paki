
package com.minkang.ultimate.pachinko.listener;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.model.Machine;
import com.minkang.ultimate.pachinko.model.MachineManager;
import com.minkang.ultimate.pachinko.util.Text;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class InteractListener implements Listener {
    private final MachineManager mm;
    public InteractListener(MachineManager mm){ this.mm=mm; }

    @EventHandler
    public void onInteract(PlayerInteractEvent e){
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock(); if (b==null) return;
        Player p = e.getPlayer();
        for (Machine m : mm.getAll().values()){
            if (m.getGoldButton()!=null && m.getGoldButton().getBlock().equals(b)){
                e.setCancelled(true);
                ItemStack hand = p.getInventory().getItemInMainHand();
                ItemStack authBall = mm.getBallItemFor(m);
                boolean hasBall = (hand!=null && authBall!=null) && com.minkang.ultimate.pachinko.util.ItemUtil.isSameMarble(hand, authBall);
                if (hasBall){
                    if (hand.getAmount()>1) hand.setAmount(hand.getAmount()-1);
                    else p.getInventory().setItemInMainHand(null);
                    mm.launchBall(p, m);
                }else{
                    if (m.getStageIndex()>0){
                        int count = Main.get().getConfig().getInt("goldEject.supplyCount", 5);
                        ItemStack ball = mm.getBallItemFor(m);
                        if (m.getDiamondBlock()!=null){
                            for (int i=0;i<count;i++){
                                m.getDiamondBlock().getWorld().dropItemNaturally(m.getDiamondBlock(), ball.clone());
                            }
                        }
                        Text.msg(p, "&a구슬 x"+count+" 배출됨.");
                    }else{
                        Text.msg(p, "&7구슬을 손에 들고 사용하세요.");
                    }
                }
                return;
            }
            if (m.getCoalButton()!=null && m.getCoalButton().getBlock().equals(b)){
                e.setCancelled(true);
                String mode = Main.get().getConfig().getString("stageMode.type","accumulate");
                if ("accumulate".equalsIgnoreCase(mode)){
                    if (m.isDrawingNow()){ Text.msg(p,"&7이미 추첨 중입니다."); return; }
                    String coalMode = Main.get().getConfig().getString("centerSlot.coalClickMode","one");
                    if ("all".equalsIgnoreCase(coalMode)){
                        int spins = m.getPendingSpins();
                        if (spins<=0){ Text.msg(p,"&7모인 추첨기회가 없습니다."); return; }
                        Text.msg(p, "&b연속 추첨 시작! &7("+spins+"회)");
                        new com.minkang.ultimate.pachinko.model.ReelSpin(m, p).runBatch(spins);
                    }else{
                        if (!m.consumeOneSpin()){ Text.msg(p,"&7모인 추첨기회가 없습니다."); return; }
                        int left = m.getPendingSpins();
                        Text.msg(p, "&b1회 추첨 시작! &7(남은 기회: &e"+left+"&7)");
                        new com.minkang.ultimate.pachinko.model.ReelSpin(m, p).runOne();
                    }
                }else{
                    Text.msg(p,"&7directDraw 모드에서는 중앙 진입 시 자동 추첨됩니다.");
                }
                return;
            }
        }
    }
}
