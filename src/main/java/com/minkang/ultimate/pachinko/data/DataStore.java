
package com.minkang.ultimate.pachinko.data;

import com.minkang.ultimate.pachinko.util.ItemSerializer;
import com.minkang.ultimate.pachinko.util.Locs;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataStore {
    private final Plugin plugin;
    private final File machineFile;
    private final File dataFile;
    private FileConfiguration machines;
    private FileConfiguration data;

    public DataStore(Plugin plugin) {
        this.plugin = plugin;
        this.machineFile = new File(plugin.getDataFolder(), "machines.yml");
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        reload();
    }

    public void reload() {
        machines = YamlConfiguration.loadConfiguration(machineFile);
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void save() {
        try { machines.save(machineFile); } catch (IOException ignored) {}
        try { data.save(dataFile); } catch (IOException ignored) {}
    }

    public FileConfiguration machines() { return machines; }
    public FileConfiguration data() { return data; }

    public void saveMachine(String id, Map<String, Object> map) {
        machines.set("machines." + id, map);
    }

    public void removeMachine(String id) {
        machines.set("machines." + id, null);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> loadMachine(String id) {
        Object o = machines.get("machines." + id);
        if (o instanceof Map) {
            return (Map<String, Object>) o;
        }
        return null;
    }

    public Set<String> getMachineIds() {
        return machines.getConfigurationSection("machines") != null
                ? machines.getConfigurationSection("machines").getKeys(false) : new HashSet<>();
    }

    // Ranking
    public void addWin(String player, int amount) {
        int cur = data.getInt("wins." + player, 0);
        data.set("wins." + player, cur + amount);
    }

    public List<Map.Entry<String,Integer>> topWins(int limit) {
        Map<String,Object> sec = data.getConfigurationSection("wins") != null ? data.getConfigurationSection("wins").getValues(false) : Collections.emptyMap();
        List<Map.Entry<String,Integer>> list = new ArrayList<>();
        for (Map.Entry<String,Object> e : sec.entrySet()) {
            if (e.getValue() instanceof Number) {
                list.add(new AbstractMap.SimpleEntry<>(e.getKey(), ((Number)e.getValue()).intValue()));
            }
        }
        list.sort((a,b) -> Integer.compare(b.getValue(), a.getValue()));
        if (list.size() > limit) return list.subList(0, limit);
        return list;
    }
}
