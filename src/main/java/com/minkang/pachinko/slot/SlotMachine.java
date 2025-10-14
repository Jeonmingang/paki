
package com.minkang.pachinko.slot;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Switch;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SlotMachine {

    private final int id;
    private final Location base;   // 중심(바닥) 좌표
    private final Location lever;  // 레버 위치
    private UUID occupant;
    private boolean spinning = false;
    private ItemStack coinItem; // null이면 설정상의 cost-item 사용

    public SlotMachine(int id, Location base, Location lever) {
        this.id = id;
        this.base = base.clone();
        this.lever = lever.clone();
    }

    public int getId() { return id; }
    public Location getBase() { return base; }
    public Location getLever() { return lever; }
    public UUID getOccupant() { return occupant; }
    public boolean isLever(Block b) { return b.getLocation().equals(lever); }
    public void setCoinItem(ItemStack item) { this.coinItem = (item==null?null:item.clone()); }

    public void saveTo(YamlConfiguration y) {
        String p = "slots."+id+".";
        y.set(p+"base", serialize(base));
        y.set(p+"lever", serialize(lever));
        // 코인 저장은 생략(선택), NPE 방지를 위해 null 체크만
        if (coinItem != null) {
            try {
                ItemMeta meta = coinItem.getItemMeta();
                if (meta != null) {
                    y.set(p+"coin.material", coinItem.getType().name());
                    y.set(p+"coin.name", meta.getDisplayName());
                } else {
                    y.set(p+"coin.material", coinItem.getType().name());
                }
            } catch (Exception ignored) {}
        }
    }

    private static String serialize(Location l) {
        if (l == null || l.getWorld() == null) return "";
        return l.getWorld().getName()+","+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ();
    }

    private boolean assignIfFree(Player p) {
        if (occupant == null) { occupant = p.getUniqueId(); return true; }
        if (occupant.equals(p.getUniqueId())) return true;
        return false;
    }
    private void release() { occupant = null; }

    private ItemStack templateCoin(SlotSettings s) {
        if (coinItem != null) return coinItem.clone();
        ItemStack it = new ItemStack(s.getCostMaterial(), 1);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(s.getCostDisplayName()); it.setItemMeta(meta); }
        return it;
    }

    private boolean hasEnough(Player p, ItemStack tmpl, int amount) {
        int need = amount;
        for (ItemStack st : p.getInventory().getContents()) {
            if (st == null) continue;
            if (st.isSimilar(tmpl)) { need -= st.getAmount(); if (need <= 0) return true; }
        }
        return need <= 0;
    }
    private void take(Player p, ItemStack tmpl, int amount) {
        int left = amount;
        ItemStack[] inv = p.getInventory().getContents();
        for (int i=0;i<inv.length && left>0;i++) {
            ItemStack st = inv[i];
            if (st == null) continue;
            if (st.isSimilar(tmpl)) {
                int take = Math.min(left, st.getAmount());
                st.setAmount(st.getAmount() - take);
                if (st.getAmount() == 0) inv[i] = null;
                left -= take;
            }
        }
        p.getInventory().setContents(inv);
    }

    private void leverPowered(boolean on) {
        Block b = lever.getBlock();
        if (!(b.getBlockData() instanceof Switch)) return;
        Switch sw = (Switch)b.getBlockData();
        sw.setPowered(on);
        b.setBlockData(sw);
        b.getState().update(true, true);
    }
    private void lamp(boolean on) {
        Location lamp = base.clone().add(0,4,0);
        lamp.getBlock().setType(on ? Material.REDSTONE_LAMP : Material.REDSTONE_LAMP);
        // 점멸 효과는 생략
    }

    public boolean onLever(Player p, SlotSettings s) {
        if (!assignIfFree(p)) { p.sendMessage(ChatColor.RED + "다른 플레이어가 사용 중입니다."); return true; }
        if (spinning) return true;

        ItemStack tmpl = templateCoin(s);
        if (!hasEnough(p, tmpl, s.getCostPerSpin())) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§c코인이 부족합니다."));
            release();
            return true;
        }
        take(p, tmpl, s.getCostPerSpin());

        leverPowered(true);
        lamp(true);
        spinning = true;

        Random rnd = new Random();
        String A = pick(s, rnd), B = pick(s, rnd), C = pick(s, rnd);
        p.sendTitle("§f[ §d"+A+" §f| §d"+B+" §f| §d"+C+" §f]", "", 0, 40, 10);

        int payout = 0;
        if (A.equals(B) && B.equals(C)) {
            payout = s.getPayout3().getOrDefault(A, 0);
        } else if (A.equals(B) || B.equals(C) || A.equals(C)) {
            String sym = A.equals(B) ? A : (B.equals(C) ? B : A);
            payout = s.getPayout2().getOrDefault(sym, 0);
        }

        if (payout > 0) {
            ItemStack win = new ItemStack(s.getCostMaterial(), payout);
            ItemMeta meta = win.getItemMeta(); if (meta != null) { meta.setDisplayName(s.getCostDisplayName()); win.setItemMeta(meta); }
            p.getWorld().dropItemNaturally(p.getLocation(), win);
            p.sendTitle("§6WIN §ex"+payout, "", 5, 40, 10);
        } else {
            p.sendTitle("§cMISS", "", 5, 20, 10);
        }

        leverPowered(false);
        lamp(false);
        spinning = false;
        release();
        return true;
    }

    private String pick(SlotSettings s, Random rnd) {
        int total = 0;
        for (SlotSettings.Symbol sym : s.getSymbols()) total += sym.weight;
        int r = rnd.nextInt(total) + 1, acc=0;
        for (SlotSettings.Symbol sym : s.getSymbols()) {
            acc += sym.weight;
            if (r <= acc) return sym.name;
        }
        return s.getSymbols().get(s.getSymbols().size()-1).name;
    }
}
