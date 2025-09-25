
package com.minkang.ultimate.pachinko.model;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.util.ItemUtil;
import com.minkang.ultimate.pachinko.util.Text;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class MachineManager {

    private final Main plugin;
    private final Map<Integer, Machine> machines = new HashMap<Integer, Machine>();
    private File machinesFile;

    public MachineManager(Main plugin) {
        this.plugin = plugin;
    }

    public Map<Integer, Machine> getMachines() {
        return machines;
    }

    public File getMachinesFile() {
        if (machinesFile == null) machinesFile = new File(plugin.getDataFolder(), "machines.yml");
        return machinesFile;
    }

    public Machine getMachine(int id) {
        return machines.get(id);
    }

    public Machine getMachineBySpecialBlock(Location l) {
        for (Machine m : machines.values()) {
            if (m.isSpecialBlock(l)) {
                return m;
            }
        }
        return null;
    }
    public boolean applyBallFromHand(Machine m, Player p) {
        org.bukkit.inventory.ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == org.bukkit.Material.AIR) return false;
        m.setBallTemplate(hand.getType(), hand.hasItemMeta()? hand.getItemMeta().getDisplayName() : null,
                hand.hasItemMeta() && hand.getItemMeta().hasLore() ? hand.getItemMeta().getLore() : null);
        saveAll(plugin.getMachinesConfig());
        try { plugin.getMachinesConfig().save(getMachinesFile()); } catch (Exception ignored) {}
        return true;
    }
    public void resetBallTemplate(Machine m) {
        m.setBallTemplate(null, null, null);
        saveAll(plugin.getMachinesConfig());
        try { plugin.getMachinesConfig().save(getMachinesFile()); } catch (Exception ignored) {}
    }

