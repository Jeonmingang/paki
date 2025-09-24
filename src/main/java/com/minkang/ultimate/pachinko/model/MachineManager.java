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
                Object oId = m.get("id");
                int id;
                if (oId instanceof Number) id = ((Number)oId).intValue();
                else if (oId != null) { try { id = Integer.parseInt(String.valueOf(oId)); } catch (Exception ex){ id = ++idx; } }
                else id = ++idx;

                String defWorld = plugin.getConfig().getString("defaultWorld","bskyblock_world");
                Object oWorld = m.get("world");
                String world = (oWorld == null) ? defWorld : String.valueOf(oWorld);

                double x = toDouble(m.get("x"), 0);
                double y = toDouble(m.get("y"), 64);
                double z = toDouble(m.get("z"), 0);

                int slots = toInt(m.get("slots"), 7);

                World w = Bukkit.getWorld(world);
                if (w == null) continue;
                Location base = new Location(w, x, y, z);
                Machine mach = new Machine(plugin, id, world, base, slots);

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

    private static double toDouble(Object o, double def){
        if (o instanceof Number) return ((Number)o).doubleValue();
        if (o != null){
            try { return Double.parseDouble(String.valueOf(o)); } catch(Exception ignored){}
        }
        return def;
    }
    private static int toInt(Object o, int def){
        if (o instanceof Number) return ((Number)o).intValue();
        if (o != null){
            try { return Integer.parseInt(String.valueOf(o)); } catch(Exception ignored){}
        }
        return def;
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

    public boolean addMachine(Machine m){
        if (m == null) return false;
        machines.put(m.getId(), m);
        save();
        return true;
    }
    public boolean removeMachine(int id){
        if (!machines.containsKey(id)) return false;
        machines.remove(id);
        save();
        return true;
    }

    public void tickAutoAdvance(){
        boolean enabled = plugin.getConfig().getBoolean("autoAdvance.enabled", true);
        if (!enabled) return;
        for (Machine m : machines.values()){
            Player actor = m.getOperatorPlayer();
            if (actor == null) continue;
            m.tryAdvanceStage(actor);
        }
    }
}
