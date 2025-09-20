
package com.minkang.ultimate.pachinko.data;

import com.minkang.ultimate.pachinko.util.Locs;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DataStore {
    private final File file;
    private FileConfiguration yaml;

    public DataStore(org.bukkit.plugin.Plugin plugin){
        this.file = new File(plugin.getDataFolder(), "machines.yml");
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        reload();
    }

    public void reload(){ yaml = YamlConfiguration.loadConfiguration(file); }
    public void save(){
        try { yaml.save(file); } catch (IOException ignored) {}
    }

    public Map<String, Object> getMachine(String id){ return yaml.getConfigurationSection("machines."+id)==null?null:yaml.getConfigurationSection("machines."+id).getValues(false); }
    public void setMachine(String id, Map<String,Object> map){ yaml.set("machines."+id, map); }
    public void remove(String id){ yaml.set("machines."+id, null); }
    public Map<String, Object> getAll(){ return yaml.getConfigurationSection("machines")==null?new HashMap<>():yaml.getConfigurationSection("machines").getValues(false); }
}
