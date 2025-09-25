
package com.minkang.ultimate.pachinko.model;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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

    public void loadAll(FileConfiguration yml) {
        machines.clear();
        List<?> raw = yml.getList("machines");
        if (raw == null) return;
        for (Object obj : raw) {
            if (!(obj instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            int id = 0;
            try { id = Integer.parseInt(String.valueOf(map.get("id"))); } catch (Exception ignored) {}
            if (id == 0) continue;
            // create a temporary section to reuse Machine.load parser
            ConfigurationSection sec = yml.createSection("load-" + id, map);
            Machine m = Machine.load(plugin, id, sec);
            machines.put(id, m);
        }
    }

    public void saveAll(FileConfiguration yml) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Map.Entry<Integer, Machine> e : machines.entrySet()) {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("id", e.getKey());
            // write into a temp section to collect values
            ConfigurationSection sec = yml.createSection("save-" + e.getKey());
            e.getValue().save(sec);
            map.putAll(sec.getValues(true));
            list.add(map);
        }
        yml.set("machines", list);
    }

    public Machine installMachine(Player p, int id) {
        if (machines.containsKey(id)) return null;

        World w = p.getWorld();
        String defWorld = plugin.getConfig().getString("worldDefault", "bskyblock_world");
        if (Bukkit.getWorld(defWorld) != null) {
            w = Bukkit.getWorld(defWorld);
        }

        Location base = p.getLocation().getBlock().getLocation();
        BlockFace facing = yawToFace(p.getLocation().getYaw());

        Machine m = new Machine(plugin, id, w, base, facing);
        m.buildStructure();

        // Save immediate
        ConfigurationSection sec = plugin.getMachinesConfig().createSection("install-" + id);
        m.save(sec);
        machines.put(id, m);
        syncToYml();
        return m;
    }

    private void syncToYml() {
        FileConfiguration y = plugin.getMachinesConfig();
        saveAll(y);
        try {
            y.save(getMachinesFile());
        } catch (Exception ignored) {}
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

    // ---- Per-machine ball template helpers ----
    public boolean applyBallFromHand(Machine m, Player p) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == org.bukkit.Material.AIR) return false;
        m.setBallTemplate(hand.getType(),
                hand.hasItemMeta() ? hand.getItemMeta().getDisplayName() : null,
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

    public void giveBalls(Player target, int amount, Integer machineId) {
        ItemStack item;
        if (machineId != null && machines.containsKey(machineId)) {
            item = ItemUtil.newBallForMachine(machines.get(machineId), amount, machineId);
        } else {
            item = ItemUtil.newBall(amount, machineId);
        }
        target.getInventory().addItem(item);
    }
}
