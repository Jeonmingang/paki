
package com.minkang.ultimate.pachinko.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;

public final class ItemUtil {
    private ItemUtil(){}
    public static boolean isSameMarble(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getType() != b.getType()) return false;
        ItemMeta am = a.getItemMeta(); ItemMeta bm = b.getItemMeta();
        if (am == null || bm == null) return false;
        String an = am.getDisplayName()==null?"":am.getDisplayName();
        String bn = bm.getDisplayName()==null?"":bm.getDisplayName();
        if (!an.equals(bn)) return false;
        List<String> al = am.getLore(); List<String> bl = bm.getLore();
        if (al == null && bl == null) return true;
        if (al == null || bl == null) return false;
        if (al.size() != bl.size()) return false;
        for (int i=0;i<al.size();i++) if (!al.get(i).equals(bl.get(i))) return false;
        return true;
    }
}
