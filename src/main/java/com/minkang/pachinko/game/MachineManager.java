package com.minkang.pachinko.game;

import com.minkang.pachinko.PachinkoPlugin;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MachineManager {

    private final PachinkoPlugin plugin;
    private final Map<Integer, Machine> machines = new HashMap<>();
    private final File file;
    private YamlConfiguration y;

    public MachineManager(PachinkoPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "machines.yml");
        reload();
    }

    public void reload() {
        machines.clear();
        if (!file.exists()) {
            try { file.getParentFile().mkdirs(); file.createNewFile(); } catch (IOException ignored) {}
        }
        y = YamlConfiguration.loadConfiguration(file);
        if (y.isConfigurationSection("machines")) {
            for (String k : y.getConfigurationSection("machines").getKeys(false)) {
                try {
                    int id = Integer.parseInt(k);
                    Machine m = Machine.from(y, id);
                    if (m != null) machines.put(id, m);
                } catch (Exception ignored) {}
            }
        }
    }

    public void saveAll() {
        for (Integer id : machines.keySet()) machines.get(id).saveTo(y);
        try { y.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public Machine getMachine(int id) { return machines.get(id); }

    public Machine getByOccupant(java.util.UUID u) {
        for (Machine m : machines.values()) if (u.equals(m.getOccupant())) return m;
        return null;
    }

    public Machine getByBlock(org.bukkit.Location l) {
        for (Machine m : machines.values()) if (m.isInteractionBlock(l)) return m;
        return null;
    }

    public Machine createTemplate(int id, Player p) {
        if (machines.containsKey(id)) return null;
        World w = p.getWorld();
        Settings s = PachinkoPlugin.get().getSettings();

        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        if (dir.lengthSquared() < 0.01) dir = new Vector(0,0,1);
        Vector right = new Vector(-dir.getZ(), 0, dir.getX());

        Location center = p.getLocation().getBlock().getLocation().add(dir.multiply(3)); // front of player

        // Floor triggers: left=COAL, center=GOLD, right=DIAMOND
        Location gold = center.clone();
        Location coal = center.clone().add(right.clone().multiply(-1));
        Location diamond = center.clone().add(right.clone().multiply(1));
        gold.getBlock().setType(Material.GOLD_BLOCK);
        coal.getBlock().setType(Material.COAL_BLOCK);
        diamond.getBlock().setType(Material.DIAMOND_BLOCK);

        // 7 lanes columns (-3..+3), glass on edges, iron bars internal
        int h = Math.max(4, s.getHeight());
        Location[] hs = new Location[7];
        for (int i=-3;i<=3;i++) {
            for (int y=1;y<=h;y++) {
                Location col = center.clone().add(right.clone().multiply(i)).add(0, y, 0);
                Material m = s.hasRowUniform() ? s.materialForY(y) : ((i==-3 || i==3) ? s.edgeForY(y) : s.innerForY(y));
                col.getBlock().setType(m);
            }
        }

        // Hoppers on top of each lane
        for (int i=-3;i<=3;i++) {
            Location hp = center.clone().add(right.clone().multiply(i)).add(0, h+1, 0);
            hp.getBlock().setType(Material.HOPPER);
            hs[i+3] = hp;
        }
        // Indicator block above center hopper
        Location indicator = hs[3].clone().add(0, 1, 0);
        indicator.getBlock().setType(Material.STONE_BRICKS);

        Machine m = new Machine(id, coal, gold, diamond, hs, indicator);
        machines.put(id, m);
        saveAll();
        return m;
    }
}
