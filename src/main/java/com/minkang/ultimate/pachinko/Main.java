package com.minkang.ultimate.pachinko;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private static Main inst;
    private MachineRegistry registry;
    private LuckyManager lucky;

    @Override public void onEnable() {
        inst = this;
        saveDefaultConfig();
        if (getConfig().getConfigurationSection("machines") == null){
            getConfig().createSection("machines");
            saveConfig();
        }
        registry = new MachineRegistry(this);
        registry.loadFromConfig();
        lucky = new LuckyManager(this);
        getCommand("파칭코").setExecutor(new PCommand(this));
        Bukkit.getPluginManager().registerEvents(new UiListener(this), this);
        getLogger().info("PachinkoReal v2.2.2 enabled");
    }

    @Override public void onDisable() {
        if (registry != null) registry.saveToConfig();
        saveConfig();
    }

    public static Main get(){ return inst; }
    public MachineRegistry getRegistry(){ return registry; }
    public LuckyManager getLucky(){ return lucky; }

    public Material getBallMaterial(){
        String s = getConfig().getString("ball-item", "IRON_NUGGET");
        Material m = Material.matchMaterial(s);
        return m != null ? m : Material.IRON_NUGGET;
    }
    public void setBallMaterial(Material m){
        getConfig().set("ball-item", m.name());
        saveConfig();
    }
}
