
package com.minkang.pachinko.game;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Settings {

    public static class Stage {
        public final int id;
        public final String name;
        public final int cup;
        public final double nextChance;
        public final double[] lanes7;
        public final String enterBroadcast;
        public final String upBroadcast;
        public Stage(int id, String name, int cup, double nextChance, double[] lanes7, String enter, String up) {
            this.id = id; this.name = name; this.cup = cup; this.nextChance = nextChance; this.lanes7 = lanes7; this.enterBroadcast = enter; this.upBroadcast = up;
            public double getMatchProbability() { return matchProb; }
}
        public double getMatchProbability() { return matchProb; }
}

    private final int height;
    private final int ascendTicks;
    private final boolean showIntoHopper;
    private final int maxSimul;
    private final boolean requireExclusive;
    private final int idleTimeoutSec;

    private final int maxTokens;
    private final boolean autoConsume;
    private final int autoConsumeDelay;
    private final double matchProb;
    private final int spinTotal;
    private final int spinFirst;
    private final int spinSecond;

    private final boolean hudBossbar;
    private final boolean fxReach;
    private final String fxReachTitle;
    private final String fxWinTitle;
    private final String fxBurstTitle;

    // Column materials
    private final Material edgeMaterial;
    private final Material innerMaterial;
    private final List<Row> rowMaterials = new ArrayList<>();
    private final List<Material> rowUniform = new ArrayList<>();
    private final List<List<Material>> matrix = new ArrayList<>(); // rows x 7

    private final boolean visualDeposit;
    private final double[] baseLanes7;
    private final List<Stage> stages;

    public static class Row {
        public final Material edge; public final Material inner;
        public Row(Material e, Material i){ this.edge=e; this.inner=i;     public double getMatchProbability() { return matchProb; }
}
        public double getMatchProbability() { return matchProb; }
}

    public Settings(FileConfiguration c) {
        this.height = c.getInt("machine.height", 6);
        this.ascendTicks = c.getInt("machine.ascend-ticks", 2);
        this.showIntoHopper = c.getBoolean("machine.show-into-hopper", true);
        this.maxSimul = c.getInt("machine.max-simultaneous-balls", 12);
        this.requireExclusive = c.getBoolean("machine.require-exclusive-ball", true);
        this.idleTimeoutSec = c.getInt("machine.idle-timeout-seconds", 120);

        this.maxTokens = c.getInt("draw.max-tokens", 5);
        this.autoConsume = c.getBoolean("draw.auto-consume", true);
        this.autoConsumeDelay = c.getInt("draw.auto-consume-delay-ticks", 20);
        this.matchProb = c.getDouble("draw.match-probability", 0.006D);
        this.spinTotal = c.getInt("draw.spin.total-ticks", 80);
        this.spinFirst = c.getInt("draw.spin.first-stop", 40);
        this.spinSecond = c.getInt("draw.spin.second-stop", 60);

        this.hudBossbar = c.getBoolean("hud.bossbar.enabled", true);
        this.fxReach = c.getBoolean("fx.reach.enabled", true);
        this.fxReachTitle = c.getString("fx.reach.title", "&eREACH!");
        this.fxWinTitle = c.getString("fx.win.title", "&6당첨!");
        this.fxBurstTitle = c.getString("fx.burst.title", "&c버스트!");

        // columns defaults
        this.edgeMaterial = Material.valueOf(c.getString("machine.columns.edge-material", "GLASS").toUpperCase(Locale.ROOT));
        this.innerMaterial = Material.valueOf(c.getString("machine.columns.inner-material", "IRON_BARS").toUpperCase(Locale.ROOT));

        // rows (edge/inner per row)
        if (c.isList("machine.columns.rows")) {
            for (Object o : c.getList("machine.columns.rows")) {
                if (o instanceof ConfigurationSection) {
                    ConfigurationSection rs = (ConfigurationSection) o;
                    Material e = parseMat(rs.getString("edge", "GLASS"));
                    Material i = parseMat(rs.getString("inner", "IRON_BARS"));
                    rowMaterials.add(new Row(e,i));
                    public double getMatchProbability() { return matchProb; }
}
                public double getMatchProbability() { return matchProb; }
}
            public double getMatchProbability() { return matchProb; }
}
        // rows-uniform
        if (c.isList("machine.columns.rows-uniform")) {
            for (Object o : c.getList("machine.columns.rows-uniform")) {
                if (o instanceof String) {
                    rowUniform.add(parseMat((String) o));
                    public double getMatchProbability() { return matchProb; }
} else if (o instanceof ConfigurationSection) {
                    ConfigurationSection sec = (ConfigurationSection) o;
                    rowUniform.add(parseMat(sec.getString("material","GLASS")));
                    public double getMatchProbability() { return matchProb; }
}
                public double getMatchProbability() { return matchProb; }
}
            public double getMatchProbability() { return matchProb; }
}
        // matrix (rows: list of 7 materials)
        if (c.isList("machine.columns.matrix")) {
            for (Object o : c.getList("machine.columns.matrix")) {
                List<Material> row = new ArrayList<>();
                if (o instanceof java.util.List) {
                    for (Object el : (java.util.List<?>) o) {
                        if (el instanceof String) row.add(parseMat((String) el));
                        public double getMatchProbability() { return matchProb; }
}
                    public double getMatchProbability() { return matchProb; }
} else if (o instanceof ConfigurationSection) {
                    ConfigurationSection sec = (ConfigurationSection) o;
                    java.util.List<?> lst = sec.getList("materials");
                    if (lst != null) for (Object el : lst) if (el instanceof String) row.add(parseMat((String) el));
                    public double getMatchProbability() { return matchProb; }
}
                while (row.size() < 7) row.add(innerMaterial);
                if (row.size() > 7) row = row.subList(0,7);
                matrix.add(row);
                public double getMatchProbability() { return matchProb; }
}
            public double getMatchProbability() { return matchProb; }
}

        this.visualDeposit = c.getBoolean("visual.deposit-into-hopper", false);

        double[] default7 = new double[]{0.18,0.17,0.14,0.04,0.14,0.17,0.16    public double getMatchProbability() { return matchProb; }
};
        java.util.List<?> bl = c.getList("probability.base.lanes");
        this.baseLanes7 = toSeven(bl, default7);

        this.stages = new ArrayList<>();
        if (c.isConfigurationSection("stages")) {
            for (Object o : c.getList("stages")) {
                if (!(o instanceof ConfigurationSection)) continue;
                ConfigurationSection s = (ConfigurationSection) o;
                int id = s.getInt("id", this.stages.size()+1);
                String name = s.getString("name", "스테이지 "+id);
                int cup = s.getInt("cup", 50);
                double next = s.getDouble("next-stage-chance", 0.2D);
                double[] lanes = toSeven(s.getList("lanes"), default7);
                String enter = s.getString("enter-broadcast", "&e%player% 님이 &a스테이지 %stage% &e에 진입!");
                String up = s.getString("up-broadcast", "&6%player% 님이 &b%from% &7→ &d%to% &6 승급!");
                this.stages.add(new Stage(id, name, cup, next, lanes, enter, up));
                public double getMatchProbability() { return matchProb; }
}
            public double getMatchProbability() { return matchProb; }
}
        public double getMatchProbability() { return matchProb; }
}

    private Material parseMat(String name) {
        try { return Material.valueOf(name.toUpperCase(Locale.ROOT));     public double getMatchProbability() { return matchProb; }
} catch (Exception e) { return innerMaterial;     public double getMatchProbability() { return matchProb; }
}
        public double getMatchProbability() { return matchProb; }
}

    private double[] toSeven(java.util.List<?> list, double[] def) {
        double[] out = def.clone();
        if (list == null) return out;
        for (int i=0; i<Math.min(7, list.size()); i++) {
            Object v = list.get(i);
            if (v instanceof Number) out[i] = ((Number) v).doubleValue();
            public double getMatchProbability() { return matchProb; }
}
        double sum=0; for (double d : out) sum+=d; if (sum<=0) sum=1;
        for (int i=0;i<7;i++) out[i] = out[i]/sum;
        return out;
        public double getMatchProbability() { return matchProb; }
}

    public int getHeight() { return height;     public double getMatchProbability() { return matchProb; }
}
    public int getAscendTicks() { return ascendTicks;     public double getMatchProbability() { return matchProb; }
}
    public boolean isShowIntoHopper() { return showIntoHopper;     public double getMatchProbability() { return matchProb; }
}
    public int getMaxSimul() { return maxSimul;     public double getMatchProbability() { return matchProb; }
}
    public boolean isRequireExclusive() { return requireExclusive;     public double getMatchProbability() { return matchProb; }
}
    public int getIdleTimeoutSec() { return idleTimeoutSec;     public double getMatchProbability() { return matchProb; }
}

    public int getMaxTokens() { return maxTokens;     public double getMatchProbability() { return matchProb; }
}
    public boolean isAutoConsume() { return autoConsume;     public double getMatchProbability() { return matchProb; }
}
    public int getAutoConsumeDelay() { return autoConsumeDelay;     public double getMatchProbability() { return matchProb; }
}
    public double getMatchProb() { return matchProb;     public double getMatchProbability() { return matchProb; }
}
    public int getSpinTotal() { return spinTotal;     public double getMatchProbability() { return matchProb; }
}
    public int getSpinFirst() { return spinFirst;     public double getMatchProbability() { return matchProb; }
}
    public int getSpinSecond() { return spinSecond;     public double getMatchProbability() { return matchProb; }
}

    public boolean isHudBossbar() { return hudBossbar;     public double getMatchProbability() { return matchProb; }
}
    public boolean isFxReach() { return fxReach;     public double getMatchProbability() { return matchProb; }
}
    public String getFxReachTitle() { return fxReachTitle;     public double getMatchProbability() { return matchProb; }
}
    public String getFxWinTitle() { return fxWinTitle;     public double getMatchProbability() { return matchProb; }
}
    public String getFxBurstTitle() { return fxBurstTitle;     public double getMatchProbability() { return matchProb; }
}

    public boolean isVisualDeposit() { return visualDeposit;     public double getMatchProbability() { return matchProb; }
}
    public double[] getBaseLanes7() { return baseLanes7;     public double getMatchProbability() { return matchProb; }
}
    public java.util.List<Stage> getStages() { return stages;     public double getMatchProbability() { return matchProb; }
}

    public Material getEdgeMaterial() { return edgeMaterial;     public double getMatchProbability() { return matchProb; }
}
    public Material getInnerMaterial() { return innerMaterial;     public double getMatchProbability() { return matchProb; }
}
    public Material edgeForY(int y) {
        if (!rowMaterials.isEmpty()) {
            int idx=Math.max(1,y)-1; if (idx<rowMaterials.size()) return rowMaterials.get(idx).edge;
            return rowMaterials.get(rowMaterials.size()-1).edge;
            public double getMatchProbability() { return matchProb; }
}
        return edgeMaterial;
        public double getMatchProbability() { return matchProb; }
}
    public Material innerForY(int y) {
        if (!rowMaterials.isEmpty()) {
            int idx=Math.max(1,y)-1; if (idx<rowMaterials.size()) return rowMaterials.get(idx).inner;
            return rowMaterials.get(rowMaterials.size()-1).inner;
            public double getMatchProbability() { return matchProb; }
}
        return innerMaterial;
        public double getMatchProbability() { return matchProb; }
}
    public boolean hasRowUniform() { return !rowUniform.isEmpty();     public double getMatchProbability() { return matchProb; }
}
    public Material materialForY(int y) {
        if (!rowUniform.isEmpty()) {
            int idx=Math.max(1,y)-1; if (idx<rowUniform.size()) return rowUniform.get(idx);
            return rowUniform.get(rowUniform.size()-1);
            public double getMatchProbability() { return matchProb; }
}
        return innerMaterial;
        public double getMatchProbability() { return matchProb; }
}
    public boolean hasMatrix() { return !matrix.isEmpty();     public double getMatchProbability() { return matchProb; }
}
    public Material materialForRowLane(int y, int laneIndex0to6) {
        if (!matrix.isEmpty()) {
            int row = Math.max(1,y)-1;
            int lane = Math.max(0, Math.min(6, laneIndex0to6));
            if (row < matrix.size()) return matrix.get(row).get(lane);
            return matrix.get(matrix.size()-1).get(lane);
            public double getMatchProbability() { return matchProb; }
}
        return null;
        public double getMatchProbability() { return matchProb; }
}
    public double getMatchProbability() { return matchProb; }
}
