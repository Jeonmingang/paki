
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
                org.bukkit.inventory.ItemStack hand = p.getInventory().getItemInMainHand();
                org.bukkit.inventory.ItemStack authBall = mm.getBallItemFor(m);
                boolean hasBall = (hand!=null && authBall!=null) && com.minkang.ultimate.pachinko.util.ItemUtil.isSameMarble(hand, authBall);

                if (hasBall){
                    // consume 1 and launch
                    if (hand.getAmount()>1) hand.setAmount(hand.getAmount()-1);
                    else p.getInventory().setItemInMainHand(null);
                    mm.launchBall(p, m);
                }else{
                    if (m.getStageIndex()>0){
                        // stage mode supply eject
                        int count = Main.get().getConfig().getInt("goldEject.supplyCount", 5);
                        org.bukkit.inventory.ItemStack ball = mm.getBallItemFor(m);
                        if (m.getDiamondBlock()!=null){
                            for (int i=0;i<count;i++){
                                m.getDiamondBlock().getWorld().dropItemNaturally(m.getDiamondBlock(), ball.clone());
                            }
                        }
                        com.minkang.ultimate.pachinko.util.Text.msg(p, "&a구슬 x"+count+" 배출됨.");
                    }else{
                        com.minkang.ultimate.pachinko.util.Text.msg(p, "&7구슬을 손에 들고 사용하세요.");
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
}
}
}
}
}
private int calcPayoutForStage(Machine m, int spins){
        double triple = getTripleChance(m);
        int per = (int) ((Number) Main.get().getConfig().getMapList("stages").get(m.getStageIndex()).getOrDefault("payoutPerHit",1)).intValue();
        int won=0;
        java.util.Random r = new java.util.Random();
        for (int i=0;i<spins;i++){
            if (r.nextDouble() < triple) won += per;
        }
        return won;
    }
    private double getTripleChance(Machine m){
        java.util.List<java.util.Map<?,?>> stages = Main.get().getConfig().getMapList("stages");
        if (m.getStageIndex()>=0 && m.getStageIndex()<stages.size()){
            Object tc = stages.get(m.getStageIndex()).get("tripleMatchChance");
            if (tc instanceof Number) return ((Number)tc).doubleValue();
        }
        return Main.get().getConfig().getDouble("centerSlot.tripleMatchChance", 0.09);
    }
}