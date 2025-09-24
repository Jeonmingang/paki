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
                Machine mach = new Machine(plugin, id, world, base, slots);

                // per-machine ball spec
                Object ballObj = m.get("ball");
                if (ballObj instanceof Map){
                    Map<?,?> b = (Map<?,?>) ballObj;
                    String type = b.get("type")==null ? null : String.valueOf(b.get("type"));
                    String name = b.get("name")==null ? null : String.valueOf(b.get("name"));
                    List<String> lore = new ArrayList<>();
                    Object lo = b.get("lore");
                    if (lo instanceof List){
                        for (Object o : (List<?>)lo) lore.add(String.valueOf(o));
                    }
                    mach.setBallSpec(type, name, lore);
                }

                machines.put(id, mach);
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
            Map<String,Object> bs = m.serializeBallSpec();
            if (bs != null) mm.put("ball", bs);
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
            Player actor = m.getOperatorPlayer();
            if (actor == null) continue; // no operator = no auto-advance
            m.tryAdvanceStage(actor);
        }
    }
}
