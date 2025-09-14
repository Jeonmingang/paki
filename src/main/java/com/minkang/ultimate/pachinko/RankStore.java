package com.minkang.ultimate.pachinko;

public class RankStore {
    public static class Stats {
        public java.util.UUID uuid;
        public String name;
        public int centerHits;
        public int stageUps;
        public int finishes;
        public int bestStage;
        public int totalPayout;
    }

    private final java.io.File file;
    private final java.util.Map<java.util.UUID, Stats> map = new java.util.HashMap<java.util.UUID, Stats>();

    public RankStore(java.io.File dataFolder){
        this.file = new java.io.File(dataFolder, "stats.yml");
        load();
    }

    private Stats ensure(java.util.UUID id, String name){
        Stats s = map.get(id);
        if (s==null){
            s = new Stats();
            s.uuid = id;
            s.name = name;
            s.centerHits = 0;
            s.stageUps = 0;
            s.finishes = 0;
            s.bestStage = 0;
            s.totalPayout = 0;
            map.put(id, s);
        }else if (name != null && (s.name==null || !s.name.equals(name))){
            s.name = name;
        }
        return s;
    }

    public void incCenter(java.util.UUID id, String name){
        ensure(id, name).centerHits++;
        save();
    }
    public void incStageUp(java.util.UUID id, String name, int newStage){
        Stats s = ensure(id, name);
        s.stageUps++;
        if (newStage > s.bestStage) s.bestStage = newStage;
        save();
    }
    public void finish(java.util.UUID id, String name, int finalStage, int paidTotal){
        Stats s = ensure(id, name);
        s.finishes++;
        if (finalStage > s.bestStage) s.bestStage = finalStage;
        s.totalPayout += Math.max(0, paidTotal);
        save();
    }

    public java.util.List<Stats> top(int n){
        java.util.List<Stats> list = new java.util.ArrayList<Stats>(map.values());
        java.util.Collections.sort(list, new java.util.Comparator<Stats>(){
            public int compare(Stats a, Stats b){
                if (a.centerHits != b.centerHits) return b.centerHits - a.centerHits;
                if (a.stageUps != b.stageUps) return b.stageUps - a.stageUps;
                if (a.bestStage != b.bestStage) return b.bestStage - a.bestStage;
                if (a.totalPayout != b.totalPayout) return b.totalPayout - a.totalPayout;
                return b.finishes - a.finishes;
            }
        });
        if (list.size() > n) return list.subList(0, n);
        return list;
    }

    public Stats get(java.util.UUID id){ return map.get(id); }

    public void load(){
        map.clear();
        if (!file.exists()) return;
        org.bukkit.configuration.file.YamlConfiguration y = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        for (String key : y.getKeys(false)){
            try{
                java.util.UUID id = java.util.UUID.fromString(key);
                Stats s = new Stats();
                s.uuid = id;
                s.name = y.getString(key+".name", "");
                s.centerHits = y.getInt(key+".centerHits", 0);
                s.stageUps = y.getInt(key+".stageUps", 0);
                s.finishes = y.getInt(key+".finishes", 0);
                s.bestStage = y.getInt(key+".bestStage", 0);
                s.totalPayout = y.getInt(key+".totalPayout", 0);
                map.put(id, s);
            }catch (Exception ignored){}
        }
    }

    public void save(){
        try{
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            org.bukkit.configuration.file.YamlConfiguration y = new org.bukkit.configuration.file.YamlConfiguration();
            for (java.util.Map.Entry<java.util.UUID, Stats> e : map.entrySet()){
                String k = e.getKey().toString();
                Stats s = e.getValue();
                y.set(k+".name", s.name);
                y.set(k+".centerHits", s.centerHits);
                y.set(k+".stageUps", s.stageUps);
                y.set(k+".finishes", s.finishes);
                y.set(k+".bestStage", s.bestStage);
                y.set(k+".totalPayout", s.totalPayout);
            }
            y.save(file);
        }catch (java.io.IOException ignored){}
    }
}
