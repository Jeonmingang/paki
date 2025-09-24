package com.minkang.ultimate.pachinko.model;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class RankingService {
    private final Plugin plugin;
    private File file;
    private FileConfiguration conf;

    public RankingService(Plugin plugin){
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "ranks.yml");
    }

    public void load(){
        if (!file.exists()){
            try { plugin.getDataFolder().mkdirs(); file.createNewFile(); }catch(IOException ignored){}
        }
        conf = YamlConfiguration.loadConfiguration(file);
    }

    public void save(){
        try { conf.save(file); } catch (IOException ignored) {}
    }

    public void addStageUp(Player p, int newStage){
        String key = "players."+p.getUniqueId()+".bestStage";
        int best = conf.getInt(key, 0);
        if (newStage > best){
            conf.set(key, newStage);
        }
        int ups = conf.getInt("players."+p.getUniqueId()+".stageUps", 0);
        conf.set("players."+p.getUniqueId()+".stageUps", ups+1);
        save();
    }

    public void addClear(Player p){
        int clears = conf.getInt("players."+p.getUniqueId()+".clears", 0);
        conf.set("players."+p.getUniqueId()+".clears", clears+1);
        save();
    }

    public static class RankEntry {
        public final UUID uuid; public final String name; public final int clears; public final int bestStage;
        public RankEntry(UUID uuid, String name, int clears, int bestStage){
            this.uuid = uuid; this.name = name; this.clears = clears; this.bestStage = bestStage;
        }
    }

    public List<RankEntry> topByClears(int limit){
        Map<String,Object> players = conf.getConfigurationSection("players")==null ? Collections.emptyMap() : conf.getConfigurationSection("players").getValues(false);
        List<RankEntry> list = new ArrayList<>();
        for (String k : players.keySet()){
            UUID u = null; try { u = UUID.fromString(k); } catch (Throwable ignored){}
            if (u==null) continue;
            String name = conf.getString("players."+k+".name","?");
            int clears = conf.getInt("players."+k+".clears",0);
            int bestStage = conf.getInt("players."+k+".bestStage",0);
            list.add(new RankEntry(u, name, clears, bestStage));
        }
        list.sort(Comparator.comparingInt((RankEntry r) -> r.clears).reversed().thenComparingInt(r->r.bestStage).reversed());
        if (list.size() > limit) return list.subList(0, limit);
        return list;
    }

    public void touchName(Player p){
        conf.set("players."+p.getUniqueId()+".name", p.getName());
    }
}
