
package com.minkang.ultimate.pachinko.util;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.model.Machine;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.ArrayList;
import java.util.List;

public class ItemUtil {

    public static ItemStack newBall(int amount, Integer machineId) {
        Main plugin = Main.inst();
        Material mat = Material.valueOf(plugin.getConfig().getString("ball.material", "SLIME_BALL"));
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getConfig().getString("ball.name", "§b파칭코 구슬"));
        List<String> lore = plugin.getConfig().getStringList("ball.lore");
        if (lore == null) lore = new ArrayList<String>();
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(plugin.getBallKey(), PersistentDataType.BYTE, (byte)1);
        if (machineId != null) {
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "machine"),
                    PersistentDataType.INTEGER, machineId);
        }
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack newBallForMachine(Machine m, int amount, Integer machineId) {
        Main plugin = Main.inst();
        Material mat = m.getBallMaterial() != null ? m.getBallMaterial()
                : Material.valueOf(plugin.getConfig().getString("ball.material", "SLIME_BALL"));
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        String name = m.getBallName() != null ? m.getBallName()
                : plugin.getConfig().getString("ball.name", "§b파칭코 구슬");
        meta.setDisplayName(name);
        List<String> lore = m.getBallLore() != null ? m.getBallLore()
                : plugin.getConfig().getStringList("ball.lore");
        if (lore == null) lore = new ArrayList<String>();
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(plugin.getBallKey(), PersistentDataType.BYTE, (byte)1);
        if (machineId != null) {
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "machine"),
                    PersistentDataType.INTEGER, machineId);
        }
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isBall(ItemStack item) {
        if (item == null) return false;
        if (!item.hasItemMeta()) return false;
        Byte b = item.getItemMeta().getPersistentDataContainer().get(Main.inst().getBallKey(), PersistentDataType.BYTE);
        if (b == null) return false;
        return b == (byte)1;
    }

    public static Integer getMachineId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        org.bukkit.persistence.PersistentDataContainer c = item.getItemMeta().getPersistentDataContainer();
        return c.get(new org.bukkit.NamespacedKey(Main.inst(), "machine"), PersistentDataType.INTEGER);
    }

    public static boolean consumeOneBall(ItemStack hand) {
        if (!isBall(hand)) return false;
        int amount = hand.getAmount();
        if (amount <= 0) return false;
        amount = amount - 1;
        if (amount <= 0) hand.setAmount(0);
        else hand.setAmount(amount);
        return true;
    }
}
