package com.minkang.ultimate.pachinko;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class UiListener implements Listener {
    private final Main plugin;
    public UiListener(Main p){ this.plugin = p; }

    @EventHandler
    public void onInteract(PlayerInteractEvent e){
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b == null) return;

        for (Machine m : plugin.getRegistry().all()){
            org.bukkit.Location start = m.getStartBlockLocation(plugin.getRegistry());
            if (start.getWorld()==null || b.getWorld()==null) continue;
            if (!start.getWorld().equals(b.getWorld())) continue;
            if (start.getBlockX()==b.getX() && start.getBlockY()==b.getY() && start.getBlockZ()==b.getZ()){
                e.setCancelled(true);
                Player p = e.getPlayer();
                if (!plugin.canInsertNow(p.getUniqueId())) return;

                ItemStack hand = p.getInventory().getItemInMainHand();
                if (!plugin.isBallItemForMachine(hand, m)){
                    String msg = plugin.getConfig().getString("messages.need-ball", "&c구슬을 손에 들어주세요.").replace("&","§");
                    p.sendMessage(msg);
                    return;
                }

                // consume 1
                int amt = hand.getAmount();
                if (amt <= 1){
                    p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                } else {
                    hand.setAmount(amt - 1);
                    p.getInventory().setItemInMainHand(hand);
                }

                // upward animation (visual only)
                try{
                    ItemStack ghost = plugin.createBallItemWith(m, 1);
                    final org.bukkit.entity.Item item = p.getWorld().dropItem(start.clone().add(0.5, 1.0, 0.5), ghost);
                    item.setPickupDelay(32767);
                    item.setGravity(false);
                    new BukkitRunnable(){
                        int ticks=0;
                        @Override public void run(){
                            ticks++;
                            item.teleport(item.getLocation().add(0, 0.22, 0));
                            if (ticks>=20){
                                item.remove();
                                cancel();
                            }
                        }
                    }.runTaskTimer(plugin, 0L, 1L);
                }catch(Throwable ignored){}

                // start sound & message
                try {
                    String key = plugin.getConfig().getString("effects.start-sound", "UI_BUTTON_CLICK");
                    p.playSound(p.getLocation(), Sound.valueOf(key), 0.8f, 1.0f);
                } catch (Throwable ignored){}
                String startMsg = plugin.getConfig().getString("messages.start", "&b구슬 투입!").replace("&","§");
                p.sendMessage(startMsg);

                new RunBall(plugin, p, m).begin();
                return;
            }
        }
    }
}
