package com.minkang.ultimate.pachinko;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
        if (getCommand("파칭코") != null) getCommand("파칭코").setExecutor(new PCommand(this));
        Bukkit.getPluginManager().registerEvents(new UiListener(this), this);
        getLogger().info("PachinkoReal v2.6.1 enabled");
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
    public String getBallName(){ return getConfig().getString("ball-name", ""); }
    public java.util.List<String> getBallLore(){ return getConfig().getStringList("ball-lore"); }

    public void setBallFromItem(ItemStack it){
        if (it==null){ return; }
        getConfig().set("ball-item", it.getType().name());
        ItemMeta meta = it.getItemMeta();
        if (meta!=null && meta.hasDisplayName()) getConfig().set("ball-name", meta.getDisplayName().replace("§","&"));
        else getConfig().set("ball-name", "");
        if (meta!=null && meta.hasLore()){
            java.util.List<String> copy = new java.util.ArrayList<String>();
            for (String s : meta.getLore()) copy.add(s.replace("§","&"));
            getConfig().set("ball-lore", copy);
        } else getConfig().set("ball-lore", new java.util.ArrayList<String>());
        saveConfig();
    }
    public boolean isBallItem(ItemStack it){
        if (it==null) return false;
        if (it.getType()!=getBallMaterial()) return false;
        String nameCfg = getBallName();
        java.util.List<String> loreCfg = getBallLore();
        ItemMeta meta = it.getItemMeta();
        if (nameCfg!=null && !nameCfg.isEmpty()){
            String nm = (meta!=null && meta.hasDisplayName()) ? meta.getDisplayName().replace("§","&") : "";
            if (!nameCfg.equals(nm)) return false;
        }
        if (loreCfg!=null && !loreCfg.isEmpty()){
            java.util.List<String> lo = (meta!=null && meta.hasLore()) ? meta.getLore() : null;
            if (lo==null || lo.size()!=loreCfg.size()) return false;
            for (int i=0;i<lo.size();i++){
                String a = lo.get(i).replace("§","&");
                if (!a.equals(loreCfg.get(i))) return false;
            }
        }
        return true;
    }
}
