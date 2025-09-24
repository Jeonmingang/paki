package com.minkang.ultimate.pachinko.util;

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
}
