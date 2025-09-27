package com.example.pachinko.manager;

import com.example.pachinko.PachinkoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class RankingManager {

    private final PachinkoPlugin plugin;
    private final File file;
    private final YamlConfiguration yaml;

    public static class Entry {
        public UUID uuid;
        public String name;
        public int score;
        public long time;
    }

    public RankingManager(PachinkoPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "ranking.yml");
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try { yaml.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void recordBust(UUID uuid, String name, int produced) {
        // keep best
        int best = yaml.getInt("players."+uuid.toString()+".best", 0);
        if (produced > best) {
            yaml.set("players."+uuid.toString()+".best", produced);
            yaml.set("players."+uuid.toString()+".name", name);
            yaml.set("players."+uuid.toString()+".time", System.currentTimeMillis());
            save();
        }
    }

    public List<Entry> top(int n) {
        Map<String, Object> map = yaml.getConfigurationSection("players") != null ? yaml.getConfigurationSection("players").getValues(false) : Collections.emptyMap();
        List<Entry> list = new ArrayList<>();
        for (String k : map.keySet()) {
            Entry e = new Entry();
            e.uuid = UUID.fromString(k);
            e.name = yaml.getString("players."+k+".name", k.substring(0, 8));
            e.score = yaml.getInt("players."+k+".best", 0);
            e.time = yaml.getLong("players."+k+".time", 0L);
            list.add(e);
        }
        list.sort((a,b) -> Integer.compare(b.score, a.score));
        if (list.size() > n) list = list.subList(0, n);
        return list;
    }
}
