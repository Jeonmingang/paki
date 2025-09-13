package com.minkang.ultimate.pachinko;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class Main extends JavaPlugin {
    private static Main inst;
    private MachineRegistry registry;
    private LuckyManager lucky;
    private final Map<java.util.UUID, Long> lastInsert = new HashMap<>();

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
        getLogger().info("PachinkoReal v3.3.0 enabled");
    }

    @Override public void onDisable() {
        if (registry != null) registry.saveToConfig();
        saveConfig();
    }

    public static Main get(){ return inst; }
    public MachineRegistry getRegistry(){ return registry; }
    public LuckyManager getLucky(){ return lucky; }

    // ===== Global easy knobs =====
    public int getGlobalCenterHitPercent(){
        return Math.max(0, Math.min(100, getConfig().getInt("global-probability.center-hit", 30)));
    }
    public int getGlobalJackpotPercent(){
        return Math.max(0, Math.min(100, getConfig().getInt("global-probability.jackpot", 2)));
    }

    // ===== Cooldown =====
    public boolean canInsertNow(java.util.UUID uid){
        int ticks = Math.max(0, getConfig().getInt("machine-default.insert-delay-ticks", 6));
        long cooldownMs = (long)(ticks * 50L);
        long now = System.currentTimeMillis();
        Long last = lastInsert.get(uid);
        if (last != null && now - last < cooldownMs) return false;
        lastInsert.put(uid, now);
        return true;
    }

    // ===== Global ball definition =====
    public Material getBallMaterial(){
        String s = getConfig().getString("ball-item", "IRON_NUGGET");
        Material m = Material.matchMaterial(s);
        return m != null ? m : Material.IRON_NUGGET;
    }
    public String getBallName(){
        String n = getConfig().getString("ball-name", "");
        return n==null?"":n;
    }
    public java.util.List<String> getBallLore(){
        return getConfig().getStringList("ball-lore");
    }

    // machine-specific override helpers
    public Material getBallMaterial(Machine m){
        if (m!=null && m.ballItem!=null){
            Material mat = Material.matchMaterial(m.ballItem);
            if (mat!=null) return mat;
        }
        return getBallMaterial();
    }
    public String getBallName(Machine m){
        if (m!=null && m.ballName!=null) return ChatColor.translateAlternateColorCodes('&', m.ballName);
        String n = getBallName();
        return ChatColor.translateAlternateColorCodes('&', n==null?"":n);
    }
    public java.util.List<String> getBallLore(Machine m){
        if (m!=null && m.ballLore!=null && !m.ballLore.isEmpty()){
            java.util.List<String> out = new java.util.ArrayList<String>();
            for (String s : m.ballLore) out.add(ChatColor.translateAlternateColorCodes('&', s));
            return out;
        }
        java.util.List<String> lore = getBallLore();
        java.util.List<String> out = new java.util.ArrayList<String>();
        for (String s : lore) out.add(ChatColor.translateAlternateColorCodes('&', s));
        return out;
    }

    public ItemStack createBallItemWith(Machine m, int amount){
        ItemStack it = new ItemStack(getBallMaterial(m), Math.max(1, amount));
        ItemMeta meta = it.getItemMeta();
        if (meta != null){
            String nm = getBallName(m);
            if (nm!=null && !nm.isEmpty()) meta.setDisplayName(nm);
            java.util.List<String> lore = getBallLore(m);
            if (lore!=null && !lore.isEmpty()) meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    public void setBallFromItem(ItemStack it){
        if (it==null){ return; }
        getConfig().set("ball-item", it.getType().name());
        ItemMeta meta = it.getItemMeta();
        if (meta!=null && meta.hasDisplayName()){
            getConfig().set("ball-name", meta.getDisplayName().replace("§","&"));
        } else getConfig().set("ball-name", "");
        if (meta!=null && meta.hasLore()){
            java.util.List<String> copy = new java.util.ArrayList<String>();
            for (String s : meta.getLore()) copy.add(s.replace("§","&"));
            getConfig().set("ball-lore", copy);
        } else getConfig().set("ball-lore", new java.util.ArrayList<String>());
        saveConfig();
    }

    public boolean isBallItemForMachine(ItemStack it, Machine m){
        if (it==null) return false;
        if (it.getType()!=getBallMaterial(m)) return false;
        String nameCfg = ChatColor.stripColor(getBallName(m));
        java.util.List<String> loreCfg = getBallLore(m);
        ItemMeta meta = it.getItemMeta();
        if (nameCfg!=null && !nameCfg.isEmpty()){
            String nm = (meta!=null && meta.hasDisplayName()) ? ChatColor.stripColor(meta.getDisplayName()) : "";
            if (!nameCfg.equals(ChatColor.stripColor(nm))) return false;
        }
        if (loreCfg!=null && !loreCfg.isEmpty()){
            java.util.List<String> lo = (meta!=null && meta.hasLore()) ? meta.getLore() : null;
            if (lo==null || lo.size()!=loreCfg.size()) return false;
            for (int i=0;i<lo.size();i++){
                String a = ChatColor.stripColor(lo.get(i));
                String b = ChatColor.stripColor(loreCfg.get(i));
                if (!a.equals(b)) return false;
            }
        }
        return true;
    }
}
