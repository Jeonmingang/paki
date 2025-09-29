package com.minkang.ultimate.pachinko.service;

import com.minkang.ultimate.pachinko.PachinkoPlugin;
import com.minkang.ultimate.pachinko.model.Machine;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MachineService {

    private final PachinkoPlugin plugin;
    private final Map<String, Machine> machines = new LinkedHashMap<>();
    private File machinesFile;
    private YamlConfiguration machinesYaml;
    private File rankingFile;
    private YamlConfiguration rankingYaml;

    public MachineService(PachinkoPlugin plugin) {
        this.plugin = plugin;
        machinesFile = new File(plugin.getDataFolder(), "machines.yml");
        rankingFile = new File(plugin.getDataFolder(), "ranking.yml");
    }

    public Map<String, Machine> all() { return machines; }

    public void loadAll() {
        if (!machinesFile.exists()) {
            try { plugin.getDataFolder().mkdirs(); machinesFile.createNewFile(); }
            catch (IOException ignored) {}
        }
        machinesYaml = YamlConfiguration.loadConfiguration(machinesFile);
        machines.clear();
        for (String id : machinesYaml.getKeys(false)) {
            ConfigurationSection s = machinesYaml.getConfigurationSection(id);
            if (s == null) continue;
            Machine m = new Machine(id);
            m.setCoalBase(deserializeLocation(s.getConfigurationSection("coal")));
            m.setGoldBase(deserializeLocation(s.getConfigurationSection("gold")));
            m.setDiamondBase(deserializeLocation(s.getConfigurationSection("diamond")));
            m.setHopperLeft(deserializeLocation(s.getConfigurationSection("hopper-left")));
            m.setHopperCenter(deserializeLocation(s.getConfigurationSection("hopper-center")));
            m.setHopperRight(deserializeLocation(s.getConfigurationSection("hopper-right")));
            m.setStage(0);
            if (s.isItemStack("exclusive-ball")) {
                m.setExclusiveBall(s.getItemStack("exclusive-ball"));
            }
            machines.put(id, m);
        }

        if (!rankingFile.exists()) {
            try { rankingFile.createNewFile(); } catch (IOException ignored) {}
        }
        rankingYaml = YamlConfiguration.loadConfiguration(rankingFile);
    }

    public void saveAll() {
        machinesYaml = new YamlConfiguration();
        for (Map.Entry<String, Machine> e : machines.entrySet()) {
            Machine m = e.getValue();
            ConfigurationSection s = machinesYaml.createSection(e.getKey());
            serializeLocation(s.createSection("coal"), m.getCoalBase());
            serializeLocation(s.createSection("gold"), m.getGoldBase());
            serializeLocation(s.createSection("diamond"), m.getDiamondBase());
            serializeLocation(s.createSection("hopper-left"), m.getHopperLeft());
            serializeLocation(s.createSection("hopper-center"), m.getHopperCenter());
            serializeLocation(s.createSection("hopper-right"), m.getHopperRight());
            if (m.getExclusiveBall() != null) s.set("exclusive-ball", m.getExclusiveBall());
        }
        try { machinesYaml.save(machinesFile); } catch (IOException ignored) {}
        try { rankingYaml.save(rankingFile); } catch (IOException ignored) {}
    }

    public Machine getByBase(Block clicked) {
        for (Machine m : machines.values()) {
            if (m.getGoldBase() != null && m.getGoldBase().equals(clicked.getLocation())) return m;
            if (m.getCoalBase() != null && m.getCoalBase().equals(clicked.getLocation())) return m;
        }
        return null;
    }

    public void recordBurst(UUID uuid, String name, int drops) {
        List<Map<String, Object>> list = (List<Map<String, Object>>) rankingYaml.getList("records", new ArrayList<>());
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("uuid", uuid.toString());
        row.put("name", name);
        row.put("drops", drops);
        row.put("time", System.currentTimeMillis());
        list.add(row);
        rankingYaml.set("records", list);
        saveAll();
    }

    public List<Map<String, Object>> topN(int n) {
        List<Map<String, Object>> list = (List<Map<String, Object>>) rankingYaml.getList("records", new ArrayList<>());
        list.sort((a,b) -> Integer.compare((int)b.get("drops"), (int)a.get("drops")));
        return list.stream().limit(n).collect(Collectors.toList());
    }

    public boolean installTemplate(String id, Player p) {
        if (machines.containsKey(id)) return false;
        Location base = p.getLocation().getBlock().getLocation();
        World w = base.getWorld();
        Vector forward = p.getLocation().getDirection().setY(0).normalize();
        Vector right = new Vector(-forward.getZ(), 0, forward.getX()); // perpendicular
        // Ground: left=coal, center=gold, right=diamond (1 block gaps)
        Location gold = base.clone().add(forward.clone().multiply(2)).getBlock().getLocation();
        Location coal = gold.clone().add(right.clone().multiply(-2)); // left with gaps
        Location diamond = gold.clone().add(right.clone().multiply(2)); // right

        coal.getBlock().setType(Material.COAL_BLOCK);
        gold.getBlock().setType(Material.GOLD_BLOCK);
        diamond.getBlock().setType(Material.DIAMOND_BLOCK);

        int height = plugin.getConfig().getInt("machine.height", 6);
        // Pillars: glass(left/right), iron_bars(center), hoppers on top
        for (int i=1;i<=height;i++) {
            coal.clone().add(0,i,0).getBlock().setType(Material.GLASS);
            gold.clone().add(0,i,0).getBlock().setType(Material.IRON_BARS);
            diamond.clone().add(0,i,0).getBlock().setType(Material.GLASS);
        }
        Location hopperL = coal.clone().add(0,height+1,0);
        Location hopperC = gold.clone().add(0,height+1,0);
        Location hopperR = diamond.clone().add(0,height+1,0);
        hopperL.getBlock().setType(Material.HOPPER);
        hopperC.getBlock().setType(Material.HOPPER);
        hopperR.getBlock().setType(Material.HOPPER);

        Machine m = new Machine(id);
        m.setCoalBase(coal);
        m.setGoldBase(gold);
        m.setDiamondBase(diamond);
        m.setHopperLeft(hopperL);
        m.setHopperCenter(hopperC);
        m.setHopperRight(hopperR);
        m.setStage(0);
        machines.put(id, m);
        saveAll();
        return true;
    }

    public void setExclusiveBall(String id, ItemStack item) {
        Machine m = machines.get(id);
        if (m != null) {
            m.setExclusiveBall(item);
            saveAll();
        }
    }

    private Location deserializeLocation(ConfigurationSection s) {
        if (s == null) return null;
        World w = Bukkit.getWorld(s.getString("world", "world"));
        if (w == null) return null;
        return new Location(w, s.getInt("x"), s.getInt("y"), s.getInt("z"));
    }
    private void serializeLocation(ConfigurationSection s, Location l) {
        if (l == null) return;
        s.set("world", l.getWorld().getName());
        s.set("x", l.getBlockX());
        s.set("y", l.getBlockY());
        s.set("z", l.getBlockZ());
    }
}
