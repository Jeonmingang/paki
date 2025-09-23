
package com.minkang.ultimate.pachinko.util;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemUtil {
    public static ItemStack buildItem(Material type, String name, java.util.List<String> lore){
        ItemStack is = new ItemStack(type);
        ItemMeta im = is.getItemMeta();
        if (im!=null){
            if (name!=null && !name.isEmpty()) im.setDisplayName(com.minkang.ultimate.pachinko.util.Text.color(name));
            java.util.List<String> ll = new java.util.ArrayList<>();
            if (lore!=null){
                for (String s: lore){ ll.add(com.minkang.ultimate.pachinko.util.Text.color(s)); }
            }
            im.setLore(ll);
            try{
                im.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES, org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }catch(Throwable ignored){}
            is.setItemMeta(im);
        }
        return is;
    }
    
    public static ItemStack readItem(ConfigurationSection sec){
        if (sec==null) return null;
        Material mat = Material.matchMaterial(sec.getString("type","SLIME_BALL"));
        if (mat==null) mat = Material.SLIME_BALL;
        ItemStack it = new ItemStack(mat,1);
        ItemMeta meta = it.getItemMeta();
        if (meta!=null){
            if (sec.contains("name")) meta.setDisplayName(com.minkang.ultimate.pachinko.util.Text.color(sec.getString("name")));
            if (sec.isList("lore")){
                List<String> lore = new ArrayList<String>();
                for (String s : sec.getStringList("lore")) lore.add(com.minkang.ultimate.pachinko.util.Text.color(s));
                meta.setLore(lore);
            }
            it.setItemMeta(meta);
        }
        return it;
    }

    // 이름/로어까지 비교(색코드 제거)
    public static boolean isSameMarble(ItemStack a, ItemStack b){
        if (a==null || b==null) return false;
        if (a.getType()!=b.getType()) return false;
        ItemMeta ma = a.getItemMeta();
        ItemMeta mb = b.getItemMeta();
        if (ma!=null && ma.hasDisplayName() && mb!=null && mb.hasDisplayName()){
            String sa = org.bukkit.ChatColor.stripColor(com.minkang.ultimate.pachinko.util.Text.color(ma.getDisplayName()));
            String sb = org.bukkit.ChatColor.stripColor(com.minkang.ultimate.pachinko.util.Text.color(mb.getDisplayName()));
            if (!sa.equals(sb)) return false;
        }
        if (ma!=null && ma.hasLore() && mb!=null && mb.hasLore()){
            List<String> la = new ArrayList<String>();
            List<String> lb = new ArrayList<String>();
            for (String s: ma.getLore()) la.add(org.bukkit.ChatColor.stripColor(com.minkang.ultimate.pachinko.util.Text.color(s)));
            for (String s: mb.getLore()) lb.add(org.bukkit.ChatColor.stripColor(com.minkang.ultimate.pachinko.util.Text.color(s)));
            if (!la.equals(lb)) return false;
        }
        return true;
    }
}
