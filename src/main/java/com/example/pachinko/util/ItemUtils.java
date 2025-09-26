
package com.example.pachinko.util;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ItemUtils {
    public static boolean equalsLoose(ItemStack a, ItemStack b){
        if (a==null || b==null) return false;
        if (a.getType()!=b.getType()) return false;
        ItemMeta am=a.getItemMeta(), bm=b.getItemMeta();
        String an = (am!=null && am.hasDisplayName())? am.getDisplayName() : null;
        String bn = (bm!=null && bm.hasDisplayName())? bm.getDisplayName() : null;
        if ((an==null)!=(bn==null)) return false;
        if (an!=null && !an.equals(bn)) return false;
        List<String> al = (am!=null && am.hasLore())? am.getLore() : null;
        List<String> bl = (bm!=null && bm.hasLore())? bm.getLore() : null;
        if ((al==null)!=(bl==null)) return false;
        if (al!=null && !al.equals(bl)) return false;
        return true;
    }
    public static ItemStack fromConfig(ConfigurationSection sec){
        if (sec==null) return null;
        ItemStack it = sec.getItemStack("data");
        if (it!=null && it.getType()!=Material.AIR) return it;
        String type = sec.getString("type","STONE");
        int amount = sec.getInt("amount",1);
        Material m = Material.matchMaterial(type);
        if (m==null) m=Material.STONE;
        return new ItemStack(m, amount);
    }
    public static void toConfig(ConfigurationSection sec, ItemStack item){
        if (sec==null) return;
        sec.set("data", item);
        if (item!=null){ sec.set("type", item.getType().name()); sec.set("amount", item.getAmount()); }
    }
}
