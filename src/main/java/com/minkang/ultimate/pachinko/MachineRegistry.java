package com.minkang.ultimate.pachinko;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class MachineRegistry {
    private final Main plugin;
    private final Map<Integer, Machine> map = new LinkedHashMap<>();

    public MachineRegistry(Main p){ this.plugin = p; }
    public Main plugin(){ return plugin; }

    public Collection<Machine> all(){ return map.values(); }
    public Machine get(int id){ return map.get(id); }

    private int nextId(){
        int id = 1;
        while (map.containsKey(id)) id++;
        return id;
    }

    public int addWithCols(Location base, int cols){
        int rows = plugin.getConfig().getInt("machine-default.rows",8);
        int id = nextId();
        Machine m = new Machine(id, base, rows, cols);
        m.build(this);
        map.put(id, m);
        return id;
    }
    public boolean remove(int id){
        Machine m = map.remove(id);
        if (m==null) return false;
        m.clear();
        return true;
    }

    public void loadFromConfig(){
        map.clear();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("machines");
        if (sec==null) return;
        for (String key : sec.getKeys(false)){
            try{
                int id = Integer.parseInt(key);
                String data = sec.getString(key);
                if (data==null) continue;
                String[] sp = data.split(",");
                World w = Bukkit.getWorld(sp[0]);
                if (w==null) continue;
                double x=Double.parseDouble(sp[1]);
                double y=Double.parseDouble(sp[2]);
                double z=Double.parseDouble(sp[3]);
                int rows = Integer.parseInt(sp[4]);
                int cols = Integer.parseInt(sp[5]);
                map.put(id, new Machine(id, new Location(w,x,y,z), rows, cols));
            }catch(Exception ignored){}
        }
    }
    public void saveToConfig(){
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("machines");
        if (sec==null) sec = plugin.getConfig().createSection("machines");
        for (String k : new ArrayList<String>(sec.getKeys(false))) sec.set(k, null);
        for (Machine m : map.values()){
            Location b = m.base;
            String v = b.getWorld().getName()+","+b.getBlockX()+","+b.getBlockY()+","+b.getBlockZ()+","+m.rows+","+m.cols;
            sec.set(String.valueOf(m.id), v);
        }
        plugin.saveConfig();
    }

    public int cfg(String path){ return plugin.getConfig().getInt(path); }
    public String cfgStr(String path){ return plugin.getConfig().getString(path); }
    public int[] cfgIntArray(String path){
        java.util.List<Integer> l = plugin.getConfig().getIntegerList(path);
        if (l==null || l.isEmpty()) return new int[]{0,0,0};
        int[] a = new int[l.size()];
        for (int i=0;i<l.size();i++) a[i]=l.get(i);
        return a;
    }
}
