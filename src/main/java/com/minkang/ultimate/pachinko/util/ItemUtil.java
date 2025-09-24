package com.minkang.ultimate.pachinko.util;

import com.minkang.ultimate.pachinko.model.Machine;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemUtil {
    public static ItemStack defaultBall(org.bukkit.plugin.Plugin plugin){
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("defaultBall");
        Material type = Material.SLIME_BALL;
        String name = "&a파칭코 구슬";
        List<String> lore = new ArrayList<>();
        lore.add("&7이 구슬은 특정 기계에서만 사용됩니다.");
        if (sec != null) {
            try { type = Material.valueOf(sec.getString("type", "SLIME_BALL")); } catch(Throwable ignored){}
            name = sec.getString("name", name);
            lore = sec.getStringList("lore");
        }
        ItemStack it = new ItemStack(type);
        ItemMeta im = it.getItemMeta();
        if (im != null){
            im.setDisplayName(Text.color(name));
            List<String> nl = new ArrayList<>();
            for (String l : lore) nl.add(Text.color(l));
            im.setLore(nl);
            try{ im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); }catch(Throwable ignored){}
            it.setItemMeta(im);
        }
        return it;
    }

    public static ItemStack getConfiguredBall(org.bukkit.plugin.Plugin plugin, Machine m){
        if (m != null){
            // if machine has ball spec, build from it
            try{
                java.util.Map<String,Object> bs = m.serializeBallSpec();
                if (bs != null){
                    Material type = Material.SLIME_BALL;
                    try { type = Material.valueOf(String.valueOf(bs.get("type"))); }catch(Throwable ignored){}
                    String name = bs.containsKey("name") ? String.valueOf(bs.get("name")) : "&a파칭코 구슬";
                    @SuppressWarnings("unchecked")
                    List<String> lore = (List<String>) bs.get("lore");
                    if (lore == null) lore = new ArrayList<>();
                    ItemStack it = new ItemStack(type);
                    ItemMeta im = it.getItemMeta();
                    if (im != null){
                        im.setDisplayName(Text.color(name));
                        List<String> nl = new ArrayList<>();
                        for (String l : lore) nl.add(Text.color(l));
                        im.setLore(nl);
                        try{ im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); }catch(Throwable ignored){}
                        it.setItemMeta(im);
                    }
                    return it;
                }
            }catch(Throwable ignored){}
        }
        return defaultBall(plugin);
    }

    
public static boolean isValidBall(org.bukkit.plugin.Plugin plugin, Machine m, ItemStack it){
    if (it == null) return false;
    if (it.getType() == Material.AIR) return false;
    ItemStack conf = getConfiguredBall(plugin, m);
    if (conf.getType() != it.getType()) return false;
    String cn = displayName(conf);
    String in = displayName(it);
    // 이름: 색코드 제거 후 "완전 동일" 요구
    if (cn != null || in != null){
        if (cn == null || in == null) return false;
        if (!strip(cn).equals(strip(in))) return false;
    }
    // 로어: 색코드 제거 후 "개수/순서/내용 완전 동일" 요구
    java.util.List<String> cl = lore(conf);
    java.util.List<String> il = lore(it);
    if (cl != null && !cl.isEmpty()){
        if (il == null || il.size() != cl.size()) return false;
        for (int i=0;i<cl.size();i++){
            if (!strip(cl.get(i)).equals(strip(il.get(i)))) return false;
        }
    }
    return true;
}
private static String displayName(ItemStack it){
        ItemMeta im = it.getItemMeta();
        if (im == null) return null;
        return im.hasDisplayName() ? im.getDisplayName() : null;
    }

    private static List<String> lore(ItemStack it){
        ItemMeta im = it.getItemMeta();
        if (im == null) return java.util.Collections.emptyList();
        List<String> l = im.getLore();
        return l == null ? java.util.Collections.emptyList() : l;
    }

    private static String strip(String s){
        return ChatColor.stripColor(Text.color(s));
    }
}
