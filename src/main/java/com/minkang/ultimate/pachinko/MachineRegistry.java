package com.minkang.ultimate.pachinko;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class MachineRegistry {
    private final Main plugin;
    private final Map<Integer, Machine> map = new LinkedHashMap<Integer, Machine>();

    public MachineRegistry(Main p){ this.plugin = p; }
    public Main plugin(){ return plugin; }

    public Collection<Machine> all(){ return map.values(); }
    public int size(){ return map.size(); }
    public Machine get(int id){ return map.get(id); }

    public int nextId(){
        int id = 1;
        while(map.containsKey(id)) id++;
        return id;
    }

    public int addWithCols(Location base, int cols){
        int id = nextId();
        Machine m = new Machine(id, base, plugin.getConfig().getInt("structure.rows", 8), cols);
        // load default per-machine overrides from global ball settings
        m.ballItem = plugin.getBallMaterial().name();
        String nm = plugin.getBallName();
        m.ballName = (nm==null ? null : nm);
        java.util.List<String> lore = plugin.getBallLore();
        m.ballLore = (lore==null? null : new ArrayList<String>(lore));
        map.put(id, m);
        m.build(this);
        return id;
    }

    public boolean remove(int id){
        Machine m = map.remove(id);
        if (m==null) return false;
        try{ m.clear(); }catch(Throwable ignored){}
        return true;
    }

    public void loadFromConfig(){
        map.clear();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("machines");
        if (sec==null) return;
        for (String key : sec.getKeys(false)){
            try{
                int id = Integer.parseInt(key);
                ConfigurationSection ms = sec.getConfigurationSection(key);
                if (ms==null) continue;
                String data = ms.getString("location");
                if (data==null) continue;
                String[] sp = data.split(",");
                if (sp.length < 6) continue;
                World w = Bukkit.getWorld(sp[0]);
                if (w==null) continue;
                double x=Double.parseDouble(sp[1]);
                double y=Double.parseDouble(sp[2]);
                double z=Double.parseDouble(sp[3]);
                int rows = Integer.parseInt(sp[4]);
                int cols = Integer.parseInt(sp[5]);
                Machine m = new Machine(id, base, plugin.getConfig().getInt("structure.rows", 8), cols);
                m.ballItem = ms.getString("ball-item", null);
                m.ballName = ms.getString("ball-name", null);
                java.util.List<String> lore = ms.getStringList("ball-lore");
                m.ballLore = (lore==null || lore.isEmpty()) ? null : new ArrayList<String>(lore);
                map.put(id, m);
                // do not build on load; assume world already has structure
            }catch(Exception ignored){}
        }
    }

    public void saveToConfig(){
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("machines");
        if (sec==null) sec = plugin.getConfig().createSection("machines");
        // clear
        for (String k : new ArrayList<String>(sec.getKeys(false))) sec.set(k, null);
        for (Machine m : map.values()){
            ConfigurationSection ms = sec.createSection(String.valueOf(m.id));
            String loc = m.base.getWorld().getName()+","+m.base.getBlockX()+","+m.base.getBlockY()+","+m.base.getBlockZ()+","+m.rows+","+m.cols;
            ms.set("location", loc);
            if (m.ballItem!=null) ms.set("ball-item", m.ballItem);
            if (m.ballName!=null) ms.set("ball-name", m.ballName);
            if (m.ballLore!=null) ms.set("ball-lore", m.ballLore);
        }
        plugin.saveConfig();
    }

    public void refreshSigns(){
        for (Machine m : all()){
            try{ m.updateSignLines(this); }catch(Throwable ignored){}
        }
    
    // convenience config readers
    public String cfgStr(String path){
        return plugin.getConfig().getString(path);
    }
    public int[] cfgIntArray(String path){
        java.util.List<Integer> li = plugin.getConfig().getIntegerList(path);
        if (li!=null && li.size()>=3){
            return new int[]{li.get(0), li.get(1), li.get(2)};
        }
        String s = plugin.getConfig().getString(path, "0,0,0");
        if (s!=null){
            String[] sp = s.split(",");
            if (sp.length>=3){
                try{
                    return new int[]{Integer.parseInt(sp[0].trim()), Integer.parseInt(sp[1].trim()), Integer.parseInt(sp[2].trim())};
                }catch(Exception ignored){}
            }
        }
        return new int[]{0,0,0};
    }
}
