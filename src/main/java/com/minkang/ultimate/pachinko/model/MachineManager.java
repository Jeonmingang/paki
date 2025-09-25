
package com.minkang.ultimate.pachinko.model;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.util.ItemUtil;
import com.minkang.ultimate.pachinko.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class MachineManager {

    private final Main plugin;
    private final Map<Integer, Machine> machines = new HashMap<Integer, Machine>();
    private File machinesFile;

    public MachineManager(Main plugin) { this.plugin = plugin; }

    public Map<Integer, Machine> getMachines() { return machines; }

    public File getMachinesFile() {
        if (machinesFile == null) machinesFile = new File(plugin.getDataFolder(), "machines.yml");
        return machinesFile;
    }

    public Machine getMachine(int id) { return machines.get(id); }

    public Machine getMachineBySpecialBlock(Location l) {
        for (Machine m : machines.values()) {
            if (m.isSpecialBlock(l)) return m;
        }
        return null;
    }

    public void loadAll(FileConfiguration yml) {
        machines.clear();
        List<Map<?, ?>> raw = yml.getMapList("machines");
        if (raw == null) return;
        for (Map<?, ?> map : raw) {
            Object idObj = map.get("id");
            if (idObj == null) continue;
            int id;
            try { id = Integer.parseInt(String.valueOf(idObj)); } catch (Exception e) { continue; }

            // Build a temporary YAML section with nested maps preserved
            YamlConfiguration tmp = new YamlConfiguration();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                tmp.set(String.valueOf(e.getKey()), e.getValue());
            }
            Machine m = Machine.load(plugin, id, tmp);
            machines.put(id, m);
        }
    }

    public void saveAll(FileConfiguration yml) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Map.Entry<Integer, Machine> entry : machines.entrySet()) {
            Integer id = entry.getKey();
            Machine m = entry.getValue();
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("id", id);
            map.put("world", m.getWorld().getName());

            Map<String, Object> base = new LinkedHashMap<String, Object>();
            base.put("x", m.getBase().getBlockX());
            base.put("y", m.getBase().getBlockY());
            base.put("z", m.getBase().getBlockZ());
            map.put("base", base);

            map.put("facing", m.getFacing().name());

            Map<String, Object> ball = new LinkedHashMap<String, Object>();
            if (m.getBallMaterial() != null) ball.put("material", m.getBallMaterial().name());
            if (m.getBallName() != null) ball.put("name", m.getBallName());
            if (m.getBallLore() != null) ball.put("lore", new ArrayList<String>(m.getBallLore()));
            ball.put("lock", m.isLockBallToMachine());
            map.put("ball", ball);

            List<Map<String, Object>> stageList = new ArrayList<Map<String, Object>>();
            for (Stage s : m.getStages()) {
                Map<String, Object> sm = new LinkedHashMap<String, Object>();
                sm.put("name", s.getName());
                sm.put("cup", s.getCup());
                sm.put("entryChance", s.getEntryChance());
                sm.put("advanceChance", s.getAdvanceChance());
                stageList.add(sm);
            }
            Map<String, Object> stages = new LinkedHashMap<String, Object>();
            stages.put("list", stageList);
            map.put("stages", stages);

            list.add(map);
        }
        yml.set("machines", list);
    }

    public Machine installMachine(Player p, int id) {
        if (machines.containsKey(id)) return null;

        World w = p.getWorld();
        String def = plugin.getConfig().getString("worldDefault", "bskyblock_world");
        if (Bukkit.getWorld(def) != null) w = Bukkit.getWorld(def);

        Location base = p.getLocation().getBlock().getLocation();
        BlockFace facing = yawToFace(p.getLocation().getYaw());

        Machine m = new Machine(plugin, id, w, base, facing);
        m.buildStructure();

        // Save & register
        YamlConfiguration tmp = new YamlConfiguration();
        m.save(tmp);
        machines.put(id, m);
        saveAll(plugin.getMachinesConfig());
        try { plugin.getMachinesConfig().save(getMachinesFile()); } catch (Exception ignored) {}
        return m;
    }

    private BlockFace yawToFace(float yaw) {
        float rot = (yaw - 90) % 360;
        if (rot < 0) rot += 360.0F;
        if (0 <= rot && rot < 45) return BlockFace.WEST;
        if (45 <= rot && rot < 135) return BlockFace.NORTH;
        if (135 <= rot && rot < 225) return BlockFace.EAST;
        if (225 <= rot && rot < 315) return BlockFace.SOUTH;
        return BlockFace.WEST;
    }

    // per-machine ball helpers
    public boolean applyBallFromHand(Machine m, Player p) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == org.bukkit.Material.AIR) return false;
        String name = null;
        java.util.List<String> lore = null;
        if (hand.hasItemMeta()) {
            if (hand.getItemMeta().hasDisplayName()) name = hand.getItemMeta().getDisplayName();
            if (hand.getItemMeta().hasLore()) lore = hand.getItemMeta().getLore();
        }
        m.setBallTemplate(hand.getType(), name, lore);
        saveAll(plugin.getMachinesConfig());
        try { plugin.getMachinesConfig().save(getMachinesFile()); } catch (Exception ignored) {}
        return true;
    }

    public void resetBallTemplate(Machine m) {
        m.setBallTemplate(null, null, null);
        saveAll(plugin.getMachinesConfig());
        try { plugin.getMachinesConfig().save(getMachinesFile()); } catch (Exception ignored) {}
    }

    public void giveBalls(Player target, int amount, Integer machineId) {
        ItemStack item;
        if (machineId != null && machines.containsKey(machineId)) {
            item = ItemUtil.newBallForMachine(machines.get(machineId), amount, machineId);
        } else {
            item = ItemUtil.newBall(amount, machineId);
        }
        target.getInventory().addItem(item);
    }

    public void removeMachine(int id) {
        machines.remove(id);
        saveAll(plugin.getMachinesConfig());
        try { plugin.getMachinesConfig().save(getMachinesFile()); } catch (Exception ignored) {}
    }

}
