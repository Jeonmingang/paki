package com.minkang.pachinko.slot;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class SlotSettings {

    public static class Symbol {
        public final String name;
        public final String display;
        public final int weight;
        public Symbol(String name, String display, int weight) {
            this.name = name; this.display = display; this.weight = weight;
        }
    }

    private final int totalTicks;
    private final int firstStop;
    private final int secondStop;

    private final Material costMaterial;
    private final String costName;
    private final int costPerSpin;

    private final Map<String, Integer> payout3Kind;
    private final int payout2KindCherry;
    private final List<Symbol> symbols;

    public SlotSettings(FileConfiguration c) {
        this.totalTicks = c.getInt("slot.spin.total-ticks", 80);
        this.firstStop = c.getInt("slot.spin.first-stop", 40);
        this.secondStop = c.getInt("slot.spin.second-stop", 60);

        this.costMaterial = Material.valueOf(c.getString("slot.cost-item.material", "GOLD_NUGGET"));
        this.costName = c.getString("slot.cost-item.display-name", "§6코인");
        this.costPerSpin = c.getInt("slot.cost-per-spin", 1);

        this.payout3Kind = new HashMap<>();
        if (c.isConfigurationSection("slot.payout.three-of-a-kind")) {
            for (String k : c.getConfigurationSection("slot.payout.three-of-a-kind").getKeys(false)) {
                payout3Kind.put(k.toUpperCase(Locale.ROOT), c.getInt("slot.payout.three-of-a-kind."+k, 0));
            }
        }
        this.payout2KindCherry = c.getInt("slot.payout.two-of-a-kind.CHERRY", 2);

        this.symbols = new ArrayList<>();
        if (c.isList("slot.symbols")) {
            for (Object o : c.getList("slot.symbols")) {
                if (!(o instanceof ConfigurationSection)) continue;
                ConfigurationSection s = (ConfigurationSection) o;
                symbols.add(new Symbol(
                        s.getString("name","SYM").toUpperCase(Locale.ROOT),
                        s.getString("display","SYM"),
                        s.getInt("weight", 1)
                ));
            }
        }
        if (symbols.isEmpty()) {
            symbols.add(new Symbol("SEVEN","7",1));
            symbols.add(new Symbol("BAR","BAR",2));
            symbols.add(new Symbol("BELL","Bell",4));
            symbols.add(new Symbol("GRAPE","Grape",6));
            symbols.add(new Symbol("CHERRY","Cherry",8));
        }
    }

    public int getTotalTicks() { return totalTicks; }
    public int getFirstStop() { return firstStop; }
    public int getSecondStop() { return secondStop; }
    public Material getCostMaterial() { return costMaterial; }
    public String getCostName() { return costName; }
    public int getCostPerSpin() { return costPerSpin; }
    public Map<String,Integer> getPayout3Kind() { return payout3Kind; }
    public int getPayout2KindCherry() { return payout2KindCherry; }
    public List<Symbol> getSymbols() { return symbols; }
}
