
package com.example.pachinko.machine;

import com.example.pachinko.UltimatePachinko;
import com.example.pachinko.util.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class MachineManager {
    private final UltimatePachinko plugin;
    private final Map<Integer, Machine> machines = new HashMap<>();
    public MachineManager(UltimatePachinko plugin){ this.plugin=plugin; }
    public Map<Integer, Machine> getMachines(){ return machines; }
    public Machine getById(int id){ return machines.get(id); }

    public Machine getBySpecialBlock(Location loc){
        for (Machine m : machines.values()) if (m.isSpecialBlock(loc)) return m;
        return null;
    }

    private static int getInt(Object o, int def){ return (o instanceof Number)? ((Number)o).intValue() : def; }
    private static double getDouble(Object o, double def){ return (o instanceof Number)? ((Number)o).doubleValue() : def; }

    public Machine createMachine(Location origin){
        int id=1; while(machines.containsKey(id)) id++;
        Machine m=new Machine(plugin, id, origin);
        // Load default stages (safe against wildcard capture)
        List<Map<?, ?>> defs = plugin.getConfig().getMapList("defaultStages");
        if (defs != null) {
            for (Map<?, ?> raw : defs) {
                Object oc = raw.get("cup");
                Object oup = raw.get("upgradeChance");
                Object ob = raw.get("payoutBurst");
                int cup = getInt(oc, 30);
                double up = getDouble(oup, 30.0);
                int burst = getInt(ob, 3);
                m.getStages().add(new StageConfig(cup, up, burst));
            }
        }
        machines.put(id, m);
        m.buildStructure();
        return m;
    }

    public boolean removeMachine(int id){
        Machine m = machines.remove(id);
        if (m != null){ m.clearStructure(); return true; }
        return false;
    }

    public void loadFromConfig(){
        machines.clear();
        ConfigurationSection msec = plugin.getConfig().getConfigurationSection("machines");
        if (msec == null) return;
        for (String key : msec.getKeys(false)){
            try{
                int id = Integer.parseInt(key);
                ConfigurationSection sec = msec.getConfigurationSection(key);
                World w = Bukkit.getWorld(sec.getString("world"));
                if (w == null) continue;
                double x = sec.getDouble("origin.x"), y = sec.getDouble("origin.y"), z = sec.getDouble("origin.z");
                Machine m = new Machine(plugin, id, new Location(w, x, y, z));
                m.setBallItem(ItemUtils.fromConfig(sec.getConfigurationSection("ball")));
                List<Map<?, ?>> list = sec.getMapList("stages.list");
                if (list != null) {
                    for (Map<?, ?> raw : list) {
                        Object oc = raw.get("cup");
                        Object oup = raw.get("upgradeChance");
                        Object ob = raw.get("payoutBurst");
                        int cup = getInt(oc, 30);
                        double up = getDouble(oup, 30.0);
                        int burst = getInt(ob, 3);
                        m.getStages().add(new StageConfig(cup, up, burst));
                    }
                }
                machines.put(id, m);
                m.buildStructure();
            }catch(Exception ex){
                plugin.getLogger().warning("Failed to load machine "+key+": "+ex.getMessage());
            }
        }
    }

    public void saveToConfig(){
        ConfigurationSection msec = plugin.getConfig().createSection("machines");
        for (Map.Entry<Integer, Machine> e : machines.entrySet()){
            int id = e.getKey(); Machine m = e.getValue();
            ConfigurationSection sec = msec.createSection(String.valueOf(id));
            sec.set("world", m.getOrigin().getWorld().getName());
            sec.set("origin.x", m.getOrigin().getX());
            sec.set("origin.y", m.getOrigin().getY());
            sec.set("origin.z", m.getOrigin().getZ());
            ItemStack ball = m.getBallItem();
            ItemUtils.toConfig(sec.createSection("ball"), ball);
            List<Map<String, Object>> out = new ArrayList<>();
            for (StageConfig s : m.getStages()){
                Map<String, Object> map = new HashMap<>();
                map.put("cup", s.cup);
                map.put("upgradeChance", s.upgradeChance);
                map.put("payoutBurst", s.payoutBurst);
                out.add(map);
            }
            sec.createSection("stages").set("list", out);
        }
        plugin.saveConfig();
    }
}
