
package com.minkang.ultimate.pachinko;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;

public class MachineRegistry {
    private final Main plugin;
    private final Map<Integer, Machine> map = new HashMap<>();
    private int nextId = 1;

    public MachineRegistry(Main p){ this.plugin = p; }

    public int count(){ return map.size(); }

    public Collection<Machine> all(){ return map.values(); }

    public Machine get(int id){ return map.get(id); }

    public Machine create(Location base, int slots){
        Machine m = new Machine();
        m.id = allocId();
        m.base = base.clone();
        m.cols = Math.max(3, slots);
        m.rows = 8;
        map.put(m.id, m);
        m.build();
        return m;
    }

    public boolean remove(int id){
        Machine m = map.remove(id);
        if (m!=null){ m.clear(); return true; }
        return false;
    }

    private int allocId(){
        while(map.containsKey(nextId)) nextId++;
        return nextId++;
    }

    public void saveAll(){
        List<Map<String,Object>> list = new ArrayList<>();
        for (Machine m : map.values()){
            Map<String,Object> e = new HashMap<>();
            e.put("id", m.id);
            e.put("world", m.base.getWorld().getName());
            e.put("x", m.base.getX());
            e.put("y", m.base.getY());
            e.put("z", m.base.getZ());
            e.put("cols", m.cols);
            e.put("rows", m.rows);
            list.add(e);
        }
        plugin.getConfig().set("machines", list);
        plugin.saveConfig();
    }

    public void loadAll(){
        map.clear();
        java.util.List<java.util.Map<?,?>> list = plugin.getConfig().getMapList("machines");
        if (list==null) return;
        for (java.util.Map<?,?> e : list){
            try{
                int id = ((Number)e.get("id")).intValue();
                String world = String.valueOf(e.get("world"));
                World w = Bukkit.getWorld(world);
                if (w==null) continue;
                double x = ((Number)e.get("x")).doubleValue();
                double y = ((Number)e.get("y")).doubleValue();
                double z = ((Number)e.get("z")).doubleValue();
                Machine m = new Machine();
                m.id = id;
                m.base = new Location(w, x, y, z);
                m.cols = ((Number)e.get("cols")).intValue();
                m.rows = ((Number)e.get("rows")).intValue();
                map.put(id, m);
                if (m.base.getChunk().isLoaded()){
                    m.build();
                }
                nextId = Math.max(nextId, id+1);
            }catch(Exception ignored){}
        }
    }
}
