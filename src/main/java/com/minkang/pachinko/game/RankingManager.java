package com.minkang.pachinko.game;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

public class RankingManager {

    private final File file;
    private YamlConfiguration y;

    public RankingManager(File dataFolder) {
        this.file = new File(dataFolder, "ranking.yml");
        reload();
    }

    public synchronized void reload() {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
        } catch (IOException ignored) {}
        y = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void save() {
        try { y.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public synchronized void recordBurst(java.util.UUID player, int drops) {
        String key = player.toString();
        int best = y.getInt("best."+key, 0);
        if (drops > best) {
            y.set("best."+key, drops);
            y.set("time."+key, System.currentTimeMillis());
            save();
        }
    }

    public static class Entry {
        public final java.util.UUID player;
        public final int best;
        public final long when;
        public Entry(java.util.UUID p, int b, long w) { this.player = p; this.best = b; this.when = w; }
    }

    public synchronized java.util.List<Entry> topN(int n) {
        java.util.Map<String, Object> map = y.getConfigurationSection("best") != null ? y.getConfigurationSection("best").getValues(false) : java.util.Collections.emptyMap();
        java.util.List<Entry> list = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Object> e : map.entrySet()) {
            try {
                java.util.UUID u = java.util.UUID.fromString(e.getKey());
                int b = (int) e.getValue();
                long w = y.getLong("time."+e.getKey(), 0L);
                list.add(new Entry(u, b, w));
            } catch (Exception ignored) {}
        }
        return list.stream().sorted(java.util.Comparator.comparingInt((Entry e)-> e.best).reversed()).limit(n).collect(java.util.stream.Collectors.toList());
    }
}
