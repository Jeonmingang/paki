
package com.minkang.pachinko.game;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
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
        public Stage(int id, String name, int cup, double nextChance, double[] lanes7,
                     String enterBroadcast, String upBroadcast) {
            this.id=id; this.name=name; this.cup=cup; this.nextChance=nextChance;
            this.lanes7=lanes7; this.enterBroadcast=enterBroadcast; this.upBroadcast=upBroadcast;
        }
    }
    public static class Row {
        public final Material edge; public final Material inner;
        public Row(Material e, Material i){ this.edge=e; this.inner=i; }
    }

    private final FileConfiguration cfg;
    private final int height, ascendTicks, idleTimeoutSeconds;
    private final boolean requireExclusive;
    private final Material edgeMaterial, innerMaterial;
    private final List<Row> rowMaterials = new ArrayList<>();

    private final int maxTokens, autoConsumeDelay, spinTotal, spinFirst, spinSecond;
    private final boolean autoConsume;
    private final double matchProb;
    private final double[] baseLanes7 = new double[]{0.18,0.17,0.14,0.04,0.14,0.17,0.16};
    private final String fxWinTitle, fxBurstTitle;
    private final List<Stage> stages = new ArrayList<>();

    public Settings(FileConfiguration cfg){
        this.cfg = cfg;
        ConfigurationSection m = cfg.getConfigurationSection("machine");
        if (m==null) m = cfg.createSection("machine");
        this.height = m.getInt("height",6);
        this.ascendTicks = m.getInt("ascend-ticks",6);
        this.idleTimeoutSeconds = m.getInt("idle-timeout-seconds",10);
        this.requireExclusive = m.getBoolean("require-exclusive-ball", true);
        ConfigurationSection cols = m.getConfigurationSection("columns");
        if (cols==null) cols = m.createSection("columns");
        this.edgeMaterial = mat(cols.getString("edge-material","GLASS"));
        this.innerMaterial = mat(cols.getString("inner-material","IRON_BARS"));
        java.util.List<?> rows = cols.getList("rows");
        if (rows!=null){
            for(Object o: rows){
                if (o instanceof ConfigurationSection){
                    ConfigurationSection rs=(ConfigurationSection)o;
                    rowMaterials.add(new Row(mat(rs.getString("edge", edgeMaterial.name())),
                                             mat(rs.getString("inner", innerMaterial.name()))));
                } else if (o instanceof java.util.Map){
                    java.util.Map<?,?> map=(java.util.Map<?,?>)o;
                    Object ev = map.containsKey("edge") ? map.get("edge") : edgeMaterial.name();
                    String e = String.valueOf(ev);
                    Object iv = map.containsKey("inner") ? map.get("inner") : innerMaterial.name();
                    String i = String.valueOf(iv);
                    rowMaterials.add(new Row(mat(e), mat(i)));
                }
            }
        }
        ConfigurationSection draw = cfg.getConfigurationSection("draw");
        if (draw==null) draw = cfg.createSection("draw");
        this.maxTokens = draw.getInt("max-tokens",5);
        this.autoConsume = draw.getBoolean("auto-consume",true);
        this.autoConsumeDelay = draw.getInt("auto-consume-delay-ticks",20);
        this.matchProb = draw.getDouble("match-probability",0.012D);
        ConfigurationSection spin = draw.getConfigurationSection("spin");
        if (spin==null) spin = draw.createSection("spin");
        this.spinTotal = spin.getInt("total-ticks",80);
        this.spinFirst = spin.getInt("first-stop",40);
        this.spinSecond = spin.getInt("second-stop",60);
        java.util.List<?> lanes = cfg.getList("probability.base.lanes");
        if (lanes!=null && lanes.size()==7){
            for (int i=0;i<7;i++) try{ baseLanes7[i]=Double.parseDouble(String.valueOf(lanes.get(i))); }catch(Exception ignored){}
        }
        this.fxWinTitle = cfg.getString("fx.win.title","§6당첨!");
        this.fxBurstTitle = cfg.getString("fx.burst.title","§c버스트!");
        java.util.List<?> st = cfg.getList("stages");
        if (st!=null){
            for (Object o: st){
                if (o instanceof ConfigurationSection){
                    stages.add(readStage((ConfigurationSection) o));
                } else if (o instanceof java.util.Map){
                    org.bukkit.configuration.ConfigurationSection sec = new org.bukkit.configuration.file.YamlConfiguration().createSection("tmp");
                    for (java.util.Map.Entry<?,?> e: ((java.util.Map<?,?>)o).entrySet()) sec.set(String.valueOf(e.getKey()), e.getValue());
                    stages.add(readStage(sec));
                }
            }
        }
    }

    private Stage readStage(ConfigurationSection s){
        int id = s.getInt("id", stages.size()+1);
        String name = s.getString("name", "STAGE "+id);
        int cup = s.getInt("cup", 60);
        double next = s.getDouble("next-stage-chance", 0.2D);
        double[] arr = new double[]{0.16,0.16,0.15,0.06,0.15,0.16,0.16};
        java.util.List<?> lanes = s.getList("lanes");
        if (lanes!=null && lanes.size()==7){
            for (int i=0;i<7;i++) try{ arr[i]=Double.parseDouble(String.valueOf(lanes.get(i))); }catch(Exception ignored){}
        }
        String enter = s.getString("enter-broadcast","&e%player% 님이 &a스테이지 %stage% &e에 진입!");
        String up = s.getString("up-broadcast","&6%player% 님이 &b%from% &7→ &d%to% &6 승급!");
        return new Stage(id,name,cup,next,arr,enter,up);
    }

    private static Material mat(String name){
        try { return Material.valueOf(name==null?"GLASS":name.toUpperCase(Locale.ROOT)); }
        catch (Exception e){ return Material.GLASS; }
    }

    public int getHeight(){ return height; }
    public int getAscendTicks(){ return ascendTicks; }
    public boolean isRequireExclusive(){ return requireExclusive; }
    public int getIdleTimeoutSeconds(){ return idleTimeoutSeconds; }

    public int getMaxTokens(){ return maxTokens; }
    public boolean isAutoConsume(){ return autoConsume; }
    public int getAutoConsumeDelay(){ return autoConsumeDelay; }

    public int getSpinTotal(){ return spinTotal; }
    public int getSpinFirst(){ return spinFirst; }
    public int getSpinSecond(){ return spinSecond; }

    public double getMatchProbability(){ return matchProb; }
    public double getMatchProb(){ return matchProb; }

    public String getFxWinTitle(){ return fxWinTitle; }
    public String getFxBurstTitle(){ return fxBurstTitle; }

    public double[] getBaseLanes7(){ return baseLanes7.clone(); }
    public java.util.List<Stage> getStages(){ return java.util.Collections.unmodifiableList(stages); }

    public Material edgeForY(int y){
        if (!rowMaterials.isEmpty()){
            int idx = Math.max(1,y)-1;
            if (idx < rowMaterials.size()) return rowMaterials.get(idx).edge;
            return rowMaterials.get(rowMaterials.size()-1).edge;
        }
        return edgeMaterial;
    }
    public Material innerForY(int y){
        if (!rowMaterials.isEmpty()){
            int idx = Math.max(1,y)-1;
            if (idx < rowMaterials.size()) return rowMaterials.get(idx).inner;
            return rowMaterials.get(rowMaterials.size()-1).inner;
        }
        return innerMaterial;
    }
    public Material materialForRowLane(int y, int laneIndex0to6){
        return (laneIndex0to6==3)? innerForY(y) : edgeForY(y);
    }
    public Material materialForY(int y){ return edgeForY(y); }

    public boolean hasRowUniform(){ return !rowMaterials.isEmpty(); }
    public boolean hasMatrix(){ return false; }

    // passthroughs used sparsely
    public String getString(String path, String def){ return cfg.getString(path, def); }
    public int getInt(String path, int def){ return cfg.getInt(path, def); }
    public double getDouble(String path, double def){ return cfg.getDouble(path, def); }
    public java.util.List<?> getList(String path){ return cfg.getList(path); }

    public double getEntryChanceOnCenter(){
        return cfg.getDouble("draw.entry-chance-on-center", 0.0D);
    }
    

    public int getAutoConsumeDelayTicks(){
        return cfg.getInt("draw.auto-consume-delay-ticks", 20);
    }

}
