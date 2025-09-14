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
    private RankStore ranks;
    private final Map<java.util.UUID, Long> lastInsert = new HashMap<java.util.UUID, Long>();

    // cached ball (global)
    private Material ballMaterial = Material.IRON_NUGGET;
    private String ballName = null;
    private java.util.List<String> ballLore = null;

    public static Main inst(){ return inst; }
    public MachineRegistry getRegistry(){ return registry; }
    public LuckyManager getLucky(){ return lucky; }

    @Override public void onEnable(){
        inst = this;
        saveDefaultConfig();
        registry = new MachineRegistry(this);
        registry.loadFromConfig();
        lucky = new LuckyManager(this);
        getCommand("파칭코").setExecutor(new PCommand(this));
        Bukkit.getPluginManager().registerEvents(new UiListener(this), this);
        ranks = new RankStore(getDataFolder());
        // load global ball
        loadBallFromConfig();
    }

    private void loadBallFromConfig(){
        String mat = getConfig().getString("ball-item", "IRON_NUGGET");
        Material m = Material.matchMaterial(mat);
        if (m!=null) ballMaterial = m;
        ballName = emptyToNull(getConfig().getString("ball-name", ""));
        ballLore = getConfig().getStringList("ball-lore");
        if (ballLore!=null && ballLore.isEmpty()) ballLore = null;
    }

    private static String emptyToNull(String s){ return (s==null || s.trim().isEmpty()) ? null : s; }

    // ===== Ball helpers =====
    public void setBallFromItem(ItemStack item){
        if (item==null || item.getType()==Material.AIR) return;
        ballMaterial = item.getType();
        ItemMeta meta = item.getItemMeta();
        if (meta!=null){
            ballName = emptyToNull(meta.hasDisplayName() ? meta.getDisplayName() : null);
            ballLore = (meta.hasLore() ? meta.getLore() : null);
        } else { ballName = null; ballLore = null; }

        getConfig().set("ball-item", ballMaterial.name());
        getConfig().set("ball-name", ballName==null?"":ballName);
        getConfig().set("ball-lore", ballLore==null?new ArrayList<String>():new ArrayList<String>(ballLore));
        saveConfig();
    }
    public Material getBallMaterial(){ return ballMaterial; }
    public String getBallName(){ return ballName; }
    public java.util.List<String> getBallLore(){ return ballLore; }

    // ===== Global percents (for sign & default) =====
    public int getGlobalCenterHitPercent(){ return getConfig().getInt("global-probability.center-hit", 30); }
    public int getGlobalJackpotPercent(){ return getConfig().getInt("global-probability.jackpot", 2); }

    // ===== Jackpot slot index helper =====
    public int getJackpotSlotIndex(int cols){
        String v = getConfig().getString("slot.jackpot-slot","center");
        if (v==null) v="center";
        v = v.trim().toLowerCase();
        if (v.equals("left")) return 0;
        if (v.equals("right")) return Math.max(0, cols-1);
        if (v.equals("center")) return cols/2;
        try{
            int one = Integer.parseInt(v);
            return Math.max(0, Math.min(cols-1, one-1));
        }catch(Exception e){
            return cols/2;
        }
    }
    public void updateAllSigns(){
        try{ registry.refreshSigns(); }catch(Exception ignored){}
    }

    // ===== Per-machine / global ball helpers =====
    public org.bukkit.inventory.ItemStack createBallItemWith(Machine m, int amount){
        org.bukkit.Material mat = getBallMaterial(m);
        org.bukkit.inventory.ItemStack it = new org.bukkit.inventory.ItemStack(mat, Math.max(1, amount));
        org.bukkit.inventory.meta.ItemMeta meta = it.getItemMeta();
        if (meta != null){
            String nm = getBallName(m);
            java.util.List<String> lr = getBallLore(m);
            if (nm != null) meta.setDisplayName(nm);
            if (lr != null && !lr.isEmpty()) meta.setLore(new java.util.ArrayList<String>(lr));
            it.setItemMeta(meta);
        }
        return it;
    }
    public boolean isBallItemForMachine(org.bukkit.inventory.ItemStack item, Machine m){
        if (item == null || item.getType() == org.bukkit.Material.AIR) return false;
        org.bukkit.Material want = getBallMaterial(m);
        if (item.getType() != want) return false;
        String wantName = getBallName(m);
        java.util.List<String> wantLore = getBallLore(m);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (wantName != null){
            if (meta == null || !meta.hasDisplayName()) return false;
            if (!wantName.equals(meta.getDisplayName())) return false;
        }
        if (wantLore != null && !wantLore.isEmpty()){
            if (meta == null || !meta.hasLore()) return false;
            java.util.List<String> got = meta.getLore();
            if (got == null || !got.equals(wantLore)) return false;
        }
        return true;
    }
    public org.bukkit.Material getBallMaterial(Machine m){
        if (m != null && m.ballItem != null){
            org.bukkit.Material mm = org.bukkit.Material.matchMaterial(m.ballItem);
            if (mm != null) return mm;
        }
        return getBallMaterial();
    }
    public String getBallName(Machine m){
        if (m != null && m.ballName != null) return m.ballName;
        return getBallName();
    }
    public java.util.List<String> getBallLore(Machine m){
        if (m != null && m.ballLore != null) return m.ballLore;
        return getBallLore();
    }
    public boolean canInsertNow(java.util.UUID uuid){
        long now = System.currentTimeMillis();
        long cd = getConfig().getLong("anti-spam.insert-cooldown-ms", 300L);
        Long last = lastInsert.get(uuid);
        if (last == null || now - last >= cd){
            lastInsert.put(uuid, now);
            return true;
        }
        return false;
    }
    
    public RankStore getRanks(){ return ranks; }

    @Override public void onDisable(){ try{ if (ranks!=null) ranks.save(); }catch(Exception ignored){} }
