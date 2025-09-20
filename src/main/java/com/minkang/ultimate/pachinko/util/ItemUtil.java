
package com.minkang.ultimate.pachinko.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class ItemUtil {
    private ItemUtil(){}

    public static boolean isSameMarble(ItemStack a, ItemStack b){
        if (a == null || b == null) return false;
        if (a.getType() != b.getType()) return false;
        ItemMeta am = a.getItemMeta();
        ItemMeta bm = b.getItemMeta();
        if (am == null || bm == null) return false;
        String an = am.hasDisplayName() ? am.getDisplayName() : "";
        String bn = bm.hasDisplayName() ? bm.getDisplayName() : "";
        if (!an.equals(bn)) return false;
        List<String> al = am.getLore();
        List<String> bl = bm.getLore();
        if (al == null || bl == null) return false;
        return al.equals(bl);
    }
}
