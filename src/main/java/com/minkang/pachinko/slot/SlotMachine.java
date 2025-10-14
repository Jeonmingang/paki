
package com.minkang.pachinko.slot;

import com.minkang.pachinko.PachinkoPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Switch;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class SlotMachine {

    private final int id;
    private final World world;
    private final Location base;
    private final Location lever;
    private Location lamp;
    private java.util.UUID occupant;
    private boolean spinning;
    private BossBar hud;

    private org.bukkit.inventory.ItemStack coinItem;
    private final Random rng = new Random();

    public SlotMachine(int id, Location base, Location lever) {
        this.id = id; this.base = base.clone(); this.lever = lever.clone();
        this.world = base.getWorld();
        this.lamp = base.clone().add(0,4,0);
    }

    public int getId() { return id; }
    public java.util.UUID getOccupant() { return occupant; }
    public boolean isLever(Block b) { return b.getLocation().equals(lever); }
    public void setCoinItem(org.bukkit.inventory.ItemStack item) { this.coinItem = item==null?null:item.clone(); }

    public void saveTo(YamlConfiguration y) {
        String p = "slots."+id+".";
        y.set(p+"base", toString(base));
        y.set(p+"lever", toString(lever));
        if (coinItem != null) {
            try { y.set(p+"coin", com.minkang.pachinko.util.ItemSerializer.itemToBase64(coinItem)); } catch (Exception ignored) {}
        }
    }
    public static SlotMachine from(YamlConfiguration y, int id) {
        String p = "slots."+id+".";
        if (!y.contains(p+"base")) return null;
        Location base = fromString(y.getString(p+"base"));
        Location lever = fromString(y.getString(p+"lever"));
        SlotMachine m = new SlotMachine(id, base, lever);
        try { if (y.contains(p+"coin")) m.coinItem = com.minkang.pachinko.util.ItemSerializer.itemFromBase64(y.getString(p+"coin")); } catch (Exception ignored) {}
        return m;
    }

    private static String toString(Location l) {
        return l.getWorld().getName()+","+l.getX()+","+l.getY()+","+l.getZ()+","+l.getYaw()+","+l.getPitch();
    }
    private static Location fromString(String s) {
        String[] p = s.split(",");
        World w = Bukkit.getWorld(p[0]);
        return new Location(w, Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]), Float.parseFloat(p[4]), Float.parseFloat(p[5]));
    }

    private boolean assignIfFree(Player p) {
        if (occupant == null) { occupant = p.getUniqueId(); return true; }
        return occupant.equals(p.getUniqueId());
    }

    public boolean onLever(Player p, SlotSettings s) {
        if (!assignIfFree(p)) { p.sendMessage(ChatColor.RED + "다른 플레이어가 사용 중입니다."); return false; }
        if (spinning) return false;

        ItemStack template = getCoinOrDefault(s);
        if (!hasEnough(p, template, s.getCostPerSpin())) {
            p.sendMessage(ChatColor.RED + "코인이 부족합니다.");
            return false;
        }
        takeFromInventory(p, template, s.getCostPerSpin());
        setLeverPowered(true);
        lampOn();

        startSpin(p, s);
        return true;
    }

    private ItemStack getCoinOrDefault(SlotSettings s) {
        if (coinItem != null) return coinItem.clone();
        ItemStack it = new ItemStack(s.getCostMaterial(), 1);
        return it;
    }
    private boolean hasEnough(Player p, ItemStack template, int amount) {
        int need = amount;
        for (ItemStack st : p.getInventory().getContents()) {
            if (st == null) continue;
            if (st.isSimilar(template)) { need -= st.getAmount(); if (need <= 0) return true; }
        }
        return need <= 0;
    }
    private void takeFromInventory(Player p, ItemStack template, int amount) {
        int need = amount;
        for (int i=0;i<p.getInventory().getSize();i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st == null || !st.isSimilar(template)) continue;
            int take = Math.min(need, st.getAmount());
            st.setAmount(st.getAmount()-take);
            if (st.getAmount()<=0) p.getInventory().setItem(i, null);
            need -= take; if (need<=0) break;
        }
    }

    private void startSpin(Player p, SlotSettings s) {
        spinning = true;
        hudShow(p, ChatColor.LIGHT_PURPLE + "SPIN...", 0.0);
        int total = s.getTotalTicks(), stop1 = s.getFirstStop(), stop2 = s.getSecondStop();
        WeightedPicker picker = new WeightedPicker(s.getSymbols());

        new BukkitRunnable() {
            int t=0; SlotSettings.Symbol a1=null,a2=null,a3=null, b1=null,b2=null,b3=null, c1=null,c2=null,c3=null;
            boolean s1=false,s2=false, reach=false;
            @Override public void run() {
                t++;
                if (!s1) { a1=picker.pick(rng); a2=picker.pick(rng); a3=picker.pick(rng); }
                if (!s2) { b1=picker.pick(rng); b2=picker.pick(rng); b3=picker.pick(rng); }
                { c1=picker.pick(rng); c2=picker.pick(rng); c3=picker.pick(rng); }
                if (t==stop1) s1=true;
                if (t==stop2) s2=true;

                if (s1 && !s2 && a2.name.equals(b2.name) && !reach) {
                    reach=true; world.playSound(base, Sound.UI_TOAST_IN, 1f, 1.4f);
                }

                show(p, a2.display, b2.display, c2.display);
                hudShow(p, ChatColor.LIGHT_PURPLE + "SPIN...", Math.min(1.0, t/(double)total));

                if (t>=total) {
                    finish(p, s, a1,a2,a3,b1,b2,b3,c1,c2,c3);
                    cancel();
                }
            }
        }.runTaskTimer(PachinkoPlugin.get(), 0L, 2L);
    }

    private void finish(Player p, SlotSettings s, SlotSettings.Symbol a1,SlotSettings.Symbol a2,SlotSettings.Symbol a3,
                        SlotSettings.Symbol b1,SlotSettings.Symbol b2,SlotSettings.Symbol b3,
                        SlotSettings.Symbol c1,SlotSettings.Symbol c2,SlotSettings.Symbol c3) {
        spinning=false;
        setLeverPowered(false);
        lampOff();
        show(p, a2.display, b2.display, c2.display);

        String[][] grid = new String[][]{
                {a1.name,b1.name,c1.name},
                {a2.name,b2.name,c2.name},
                {a3.name,b3.name,c3.name}
        };
        int totalPay = 0;
        java.util.function.Function<String,Integer> pay = sym -> s.getPayout3Kind().getOrDefault(sym, 0);
        String[][] lines = new String[][] {
                {grid[0][0],grid[0][1],grid[0][2]},
                {grid[1][0],grid[1][1],grid[1][2]},
                {grid[2][0],grid[2][1],grid[2][2]},
                {grid[0][0],grid[1][1],grid[2][2]},
                {grid[2][0],grid[1][1],grid[0][2]}
        };
        for (String[] line : lines) if (line[0].equals(line[1]) && line[1].equals(line[2])) totalPay += pay.apply(line[0]);
        if (grid[1][0].equals("CHERRY") && grid[1][1].equals("CHERRY")) totalPay += s.getPayout2KindCherry();

        if (totalPay>0) {
            hudShow(p, ChatColor.GOLD + "WIN x"+totalPay, 1.0);
            ItemStack coin = getCoinOrDefault(s);
            int left = totalPay;
            while (left>0) {
                int amt = Math.min(coin.getMaxStackSize(), left);
                ItemStack st = coin.clone(); st.setAmount(amt);
                world.dropItem(base.clone().add(0.5,1,0.5), st);
                left -= amt;
            }
            world.playSound(base, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.0f);
        } else {
            hudShow(p, ChatColor.GRAY + "LOSE", 0.0);
            world.playSound(base, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.8f);
        }
        Bukkit.getScheduler().runTaskLater(PachinkoPlugin.get(), () -> hudClear(), 40L);
    }

    private void show(Player p, String s1, String s2, String s3) {
        String s = ChatColor.WHITE + "[ " + ChatColor.LIGHT_PURPLE + s1 + ChatColor.WHITE + " | "
                + ChatColor.LIGHT_PURPLE + s2 + ChatColor.WHITE + " | "
                + ChatColor.LIGHT_PURPLE + s3 + ChatColor.WHITE + " ]";
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(s));
    }

    private void setLeverPowered(boolean powered) {
        BlockData data = lever.getBlock().getBlockData();
        if (data instanceof Switch) {
            Switch sw = (Switch) data;
            sw.setPowered(powered);
            lever.getBlock().setBlockData(sw, false);
        }
    }
    private void lampOn() { if (lamp!=null) lamp.getBlock().setType(Material.SEA_LANTERN); }
    private void lampOff() { if (lamp!=null) lamp.getBlock().setType(Material.REDSTONE_LAMP); }

    private void hudShow(Player p, String title, double progress) {
        if (hud == null) { hud = Bukkit.createBossBar("SLOT", BarColor.PURPLE, BarStyle.SEGMENTED_10); hud.addPlayer(p); }
        hud.setTitle(title);
        hud.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        if (!hud.getPlayers().contains(p)) hud.addPlayer(p);
    }
    private void hudClear() { if (hud!=null) { hud.removeAll(); hud=null; } }

    private static class WeightedPicker {
        private final java.util.List<SlotSettings.Symbol> list; private final int total;
        WeightedPicker(java.util.List<SlotSettings.Symbol> l) { this.list=l; int s=0; for (SlotSettings.Symbol x:l) s+=Math.max(1,x.weight); this.total=s; }
        SlotSettings.Symbol pick(Random r) { int x=r.nextInt(total), a=0; for (SlotSettings.Symbol s:list){ a+=Math.max(1,s.weight); if (x<a) return s; } return list.get(list.size()-1); }
    }
}
