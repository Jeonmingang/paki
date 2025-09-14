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
}
