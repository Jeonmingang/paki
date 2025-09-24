package com.minkang.ultimate.pachinko.model;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MachineManager {
    private final Main plugin;
    private final Map<Integer, Machine> machines = new HashMap<>();
    private File file;
    private FileConfiguration conf;

    public MachineManager(Main plugin){
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "machines.yml");
    }

    public void load(){
        if (!file.exists()){
            try { plugin.saveResource("machines.yml", false); }catch(IllegalArgumentException ignored){
                try { plugin.getDataFolder().mkdirs(); file.createNewFile(); }catch(IOException ignored2){}
            }
        }
        conf = YamlConfiguration.loadConfiguration(file);
        machines.clear();
        List<Map<?,?>> list = conf.getMapList("machines");
        int idx = 0;
        for (Map<?,?> m : list){
            try{
                int id = ((Number)m.getOrDefault("id", ++idx)).intValue();
                String world = String.valueOf(m.getOrDefault("world", plugin.getConfig().getString("defaultWorld","bskyblock_world")));
                double x = Double.parseDouble(String.valueOf(m.getOrDefault("x", 0)));
                double y = Double.parseDouble(String.valueOf(m.getOrDefault("y", 64)));
                double z = Double.parseDouble(String.valueOf(m.getOrDefault("z", 0)));
                int slots = Integer.parseInt(String.valueOf(m.getOrDefault("slots", 7)));
                World w = Bukkit.getWorld(world);
                if (w == null) continue;
                Location base = new Location(w, x, y, z);
                machines.put(id, new Machine(plugin, id, world, base, slots));
            }catch(Throwable ignored){}
        }
    }

    public void save(){
        List<Map<String,Object>> list = new ArrayList<>();
        for (Machine m : machines.values()){
            Map<String,Object> mm = new HashMap<>();
            mm.put("id", m.getId());
            mm.put("world", m.getBase().getWorld().getName());
            mm.put("x", m.getBase().getX());
            mm.put("y", m.getBase().getY());
            mm.put("z", m.getBase().getZ());
            mm.put("slots", 7);
            list.add(mm);
        }
        conf.set("machines", list);
        try { conf.save(file); } catch (IOException ignored) {}
    }

    public Collection<Machine> all(){ return machines.values(); }
    public Machine byId(int id){ return machines.get(id); }

    public void tickAutoAdvance(){
        boolean enabled = plugin.getConfig().getBoolean("autoAdvance.enabled", true);
        if (!enabled) return;
        for (Machine m : machines.values()){
            // For demo, auto advance only if a player is nearby within 6 blocks
            Player near = nearestPlayer(m.getBase(), 6);
            if (near == null) continue;
            m.tryAdvanceStage(near);
        }
    }

    private Player nearestPlayer(Location loc, double radius){
        Player best = null;
        double bestD = radius*radius;
        for (Player p : loc.getWorld().getPlayers()){
            double d = p.getLocation().distanceSquared(loc);
            if (d <= bestD){
                bestD = d; best = p;
            }
        }
        return best;
    }
}
