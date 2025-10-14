package com.minkang.pachinko.slot;

import com.minkang.pachinko.PachinkoPlugin;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SlotManager {

    private final PachinkoPlugin plugin;
    private final Map<Integer, SlotMachine> machines = new HashMap<>();
    private final File file;
    private YamlConfiguration y;
    private final SlotSettings settings;

    public SlotManager(PachinkoPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "slots.yml");
        this.settings = new SlotSettings(plugin.getConfig());
        reload();
    }

    public void reload() {
        machines.clear();
        if (!file.exists()) {
            try { file.getParentFile().mkdirs(); file.createNewFile(); } catch (IOException ignored) {}
        }
        y = YamlConfiguration.loadConfiguration(file);
        if (y.isConfigurationSection("slots")) {
            for (String k : y.getConfigurationSection("slots").getKeys(false)) {
                try {
                    int id = Integer.parseInt(k);
                    SlotMachine m = SlotMachine.from(y, id);
                    if (m != null) machines.put(id, m);
                } catch (Exception ignored) {}
            }
        }
    }

    public void saveAll() {
        for (Integer id : machines.keySet()) machines.get(id).saveTo(y);
        try { y.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public SlotSettings getSettings() { return settings; }
    public SlotMachine getMachine(int id) { return machines.get(id); }
    public SlotMachine getByLever(org.bukkit.block.Block lever) {
        for (SlotMachine m : machines.values()) if (m.isLever(lever)) return m;
        return null;
    }

    public boolean setCoinForMachine(int id, org.bukkit.inventory.ItemStack item) {
        SlotMachine m = machines.get(id);
        if (m == null) return false;
        m.setCoinItem(item);
        saveAll();
        return true;
    }

    public SlotMachine createTemplate(int id, Player p) {
        if (machines.containsKey(id)) return null;
        World w = p.getWorld();
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX());
        Location center = p.getLocation().getBlock().getLocation().add(dir.multiply(3));

        // screen 3x3
        for (int y=1;y<=3;y++) for (int x=-1;x<=1;x++) {
            center.clone().add(right.clone().multiply(x)).add(0,y,0).getBlock().setType(Material.WHITE_CONCRETE);
        }
        // lamp
        center.clone().add(0,4,0).getBlock().setType(Material.REDSTONE_LAMP);
        // lever
        Location lever = center.clone().add(right.clone().multiply(2)).add(0,1,0);
        lever.getBlock().setType(Material.LEVER);

        SlotMachine m = new SlotMachine(id, center, lever);
        machines.put(id, m);
        saveAll();
        return m;
    }
}
