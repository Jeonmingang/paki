
package com.minkang.pachinko.slot;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.*;

public class SlotSettings {

    public static class Symbol {
        public final String name, display;
        public final int weight;
        public Symbol(String n, String d, int w) { name=n; display=d; weight=w; }
    }

    private final int totalTicks, firstStop, secondStop;
    private final Material costMat;
    private final String costName;
    private final int costPerSpin;
    private final Map<String,Integer> payout3 = new HashMap<>();
    private final Map<String,Integer> payout2 = new HashMap<>();
    private final List<Symbol> symbols = new ArrayList<>();

    public SlotSettings(FileConfiguration c) {
        ConfigurationSection sec = c.getConfigurationSection("slot");
        ConfigurationSection spin = sec.getConfigurationSection("spin");
        this.totalTicks = spin.getInt("total-ticks", 80);
        this.firstStop  = spin.getInt("first-stop", 40);
        this.secondStop = spin.getInt("second-stop", 60);
        ConfigurationSection cost = sec.getConfigurationSection("cost-item");
        this.costMat  = Material.matchMaterial(cost.getString("material", "GOLD_NUGGET"));
        this.costName = cost.getString("display-name", "§6코인");
        this.costPerSpin = sec.getInt("cost-per-spin", 1);
        ConfigurationSection pay = sec.getConfigurationSection("payout");
        if (pay.isConfigurationSection("three-of-a-kind")) {
            for (String k : pay.getConfigurationSection("three-of-a-kind").getKeys(false)) payout3.put(k.toUpperCase(Locale.ROOT), pay.getInt("three-of-a-kind."+k));
        }
        if (pay.isConfigurationSection("two-of-a-kind")) {
            for (String k : pay.getConfigurationSection("two-of-a-kind").getKeys(false)) payout2.put(k.toUpperCase(Locale.ROOT), pay.getInt("two-of-a-kind."+k));
        }
        for (Map<?,?> m : sec.getMapList("symbols")) {
            String n = String.valueOf(m.get("name"));
            String d = String.valueOf(m.get("display"));
            int w    = Integer.parseInt(String.valueOf(m.get("weight")));
            symbols.add(new Symbol(n.toUpperCase(Locale.ROOT), d, w));
        }
    }

    public int getTotalTicks(){ return totalTicks; }
    public int getFirstStop(){ return firstStop; }
    public int getSecondStop(){ return secondStop; }
    public Material getCostMaterial(){ return costMat; }
    public String getCostDisplayName(){ return costName; }
    public int getCostPerSpin(){ return costPerSpin; }
    public Map<String,Integer> getPayout3(){ return payout3; }
    public Map<String,Integer> getPayout2(){ return payout2; }
    public List<Symbol> getSymbols(){ return symbols; }
}
