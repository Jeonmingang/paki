
package com.minkang.ultimate.pachinko.util;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemUtil {
    public static ItemStack readItem(ConfigurationSection sec){
        if (sec==null) return null;
        Material mat = Material.matchMaterial(sec.getString("type","SLIME_BALL"));
        if (mat==null) mat = Material.SLIME_BALL;
        ItemStack it = new ItemStack(mat,1);
        ItemMeta meta = it.getItemMeta();
        if (meta!=null){
            if (sec.contains("name")) meta.setDisplayName(Text.color(sec.getString("name")));
            if (sec.isList("lore")){
                List<String> lore = new ArrayList<>();
                for (String s : sec.getStringList("lore")) lore.add(Text.color(s));
                meta.setLore(lore);
            
    public static boolean isSameMarble(org.bukkit.inventory.ItemStack a, org.bukkit.inventory.ItemStack b){
        if (a==null || b==null) return false;
        if (a.getType()!=b.getType()) return false;
        org.bukkit.inventory.meta.ItemMeta ma = a.getItemMeta();
        org.bukkit.inventory.meta.ItemMeta mb = b.getItemMeta();
        if (ma!=null && ma.hasDisplayName() && mb!=null && mb.hasDisplayName()){
            String sa = Text.color(ma.getDisplayName());
            String sb = Text.color(mb.getDisplayName());
            if (!org.bukkit.ChatColor.stripColor(sa).equals(org.bukkit.ChatColor.stripColor(sb))) return false;
        }
        if (ma!=null && ma.hasLore() && mb!=null && mb.hasLore()){
            java.util.List<String> la = new java.util.ArrayList<>();
            java.util.List<String> lb = new java.util.ArrayList<>();
            for (String s: ma.getLore()) la.add(org.bukkit.ChatColor.stripColor(Text.color(s)));
            for (String s: mb.getLore()) lb.add(org.bukkit.ChatColor.stripColor(Text.color(s)));
            if (!la.equals(lb)) return false;
        }
        return true;
    }
}
            it.setItemMeta(meta);
        
    public static boolean isSameMarble(org.bukkit.inventory.ItemStack a, org.bukkit.inventory.ItemStack b){
        if (a==null || b==null) return false;
        if (a.getType()!=b.getType()) return false;
        org.bukkit.inventory.meta.ItemMeta ma = a.getItemMeta();
        org.bukkit.inventory.meta.ItemMeta mb = b.getItemMeta();
        if (ma!=null && ma.hasDisplayName() && mb!=null && mb.hasDisplayName()){
            String sa = Text.color(ma.getDisplayName());
            String sb = Text.color(mb.getDisplayName());
            if (!org.bukkit.ChatColor.stripColor(sa).equals(org.bukkit.ChatColor.stripColor(sb))) return false;
        }
        if (ma!=null && ma.hasLore() && mb!=null && mb.hasLore()){
            java.util.List<String> la = new java.util.ArrayList<>();
            java.util.List<String> lb = new java.util.ArrayList<>();
            for (String s: ma.getLore()) la.add(org.bukkit.ChatColor.stripColor(Text.color(s)));
            for (String s: mb.getLore()) lb.add(org.bukkit.ChatColor.stripColor(Text.color(s)));
            if (!la.equals(lb)) return false;
        }
        return true;
    }
}
        return it;
    
    public static boolean isSameMarble(org.bukkit.inventory.ItemStack a, org.bukkit.inventory.ItemStack b){
        if (a==null || b==null) return false;
        if (a.getType()!=b.getType()) return false;
        org.bukkit.inventory.meta.ItemMeta ma = a.getItemMeta();
        org.bukkit.inventory.meta.ItemMeta mb = b.getItemMeta();
        if (ma!=null && ma.hasDisplayName() && mb!=null && mb.hasDisplayName()){
            String sa = Text.color(ma.getDisplayName());
            String sb = Text.color(mb.getDisplayName());
            if (!org.bukkit.ChatColor.stripColor(sa).equals(org.bukkit.ChatColor.stripColor(sb))) return false;
        }
        if (ma!=null && ma.hasLore() && mb!=null && mb.hasLore()){
            java.util.List<String> la = new java.util.ArrayList<>();
            java.util.List<String> lb = new java.util.ArrayList<>();
            for (String s: ma.getLore()) la.add(org.bukkit.ChatColor.stripColor(Text.color(s)));
            for (String s: mb.getLore()) lb.add(org.bukkit.ChatColor.stripColor(Text.color(s)));
            if (!la.equals(lb)) return false;
        }
        return true;
    }
}

    public static boolean isSameMarble(org.bukkit.inventory.ItemStack a, org.bukkit.inventory.ItemStack b){
        if (a==null || b==null) return false;
        if (a.getType()!=b.getType()) return false;
        org.bukkit.inventory.meta.ItemMeta ma = a.getItemMeta();
        org.bukkit.inventory.meta.ItemMeta mb = b.getItemMeta();
        if (ma!=null && ma.hasDisplayName() && mb!=null && mb.hasDisplayName()){
            String sa = Text.color(ma.getDisplayName());
            String sb = Text.color(mb.getDisplayName());
            if (!org.bukkit.ChatColor.stripColor(sa).equals(org.bukkit.ChatColor.stripColor(sb))) return false;
        }
        if (ma!=null && ma.hasLore() && mb!=null && mb.hasLore()){
            java.util.List<String> la = new java.util.ArrayList<>();
            java.util.List<String> lb = new java.util.ArrayList<>();
            for (String s: ma.getLore()) la.add(org.bukkit.ChatColor.stripColor(Text.color(s)));
            for (String s: mb.getLore()) lb.add(org.bukkit.ChatColor.stripColor(Text.color(s)));
            if (!la.equals(lb)) return false;
        }
        return true;
    }
}
