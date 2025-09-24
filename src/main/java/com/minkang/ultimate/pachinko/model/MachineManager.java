package com.minkang.ultimate.pachinko.model;

import com.minkang.ultimate.pachinko.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MachineManager {

    private final JavaPlugin plugin;
    private final Map<String, Machine> machines = new HashMap<>();
    private File machinesFile;
    private FileConfiguration machinesCfg;

    public MachineManager(JavaPlugin plugin) {
        this.plugin = plugin;
        initMachinesFile();
        loadMachines();
        startTicker();
    }

    private void initMachinesFile() {
        machinesFile = new File(plugin.getDataFolder(), "machines.yml");
        if (!machinesFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                machinesFile.createNewFile();
                machinesCfg = YamlConfiguration.loadConfiguration(machinesFile);
                machinesCfg.set("machines", new ArrayList<Map<String, Object>>());
                machinesCfg.save(machinesFile);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create machines.yml: " + e.getMessage());
            }
        }
        machinesCfg = YamlConfiguration.loadConfiguration(machinesFile);
    }

    public Collection<Machine> getMachines() {
        return machines.values();
    }

    public Machine getById(String id) {
        return machines.get(id);
    }

    public Machine getByBase(Location base) {
        if (base == null) return null;
        for (Machine m : machines.values()) {
            if (m.getBase().getWorld().equals(base.getWorld())
                    && m.getBase().getBlockX() == base.getBlockX()
                    && m.getBase().getBlockY() == base.getBlockY()
                    && m.getBase().getBlockZ() == base.getBlockZ()) {
                return m;
            }
        }
        return null;
    }

    public Machine register(Location base) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        Machine m = new Machine(id, base);
        machines.put(id, m);
        saveMachines();
        return m;
    }

    public boolean unregister(String id) {
        Machine m = machines.remove(id);
        if (m != null) {
            saveMachines();
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public void loadMachines() {
        machines.clear();
        List<Map<?, ?>> raw = machinesCfg.getMapList("machines");
        for (Map<?, ?> entry : raw) {
            try {
                String id = String.valueOf(entry.get("id"));
                String world = String.valueOf(entry.get("world"));
                int x = (int) entry.get("x");
                int y = (int) entry.get("y");
                int z = (int) entry.get("z");
                Location loc = new Location(Bukkit.getWorld(world), x, y, z);
                machines.put(id, new Machine(id, loc));
            } catch (Exception e) {
                plugin.getLogger().warning("Bad machine entry: " + entry);
            }
        }
    }

    public void saveMachines() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Machine m : machines.values()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("world", m.getBase().getWorld().getName());
            map.put("x", m.getBase().getBlockX());
            map.put("y", m.getBase().getBlockY());
            map.put("z", m.getBase().getBlockZ());
            list.add(map);
        }
        machinesCfg.set("machines", list);
        try { machinesCfg.save(machinesFile); } catch (IOException ignored) {}
    }

    // ===== simple stage handling =====

    public void enterStage(Machine machine, Player owner) {
        if (machine == null) return;
        machine.setStageActive(true);
        machine.setStageOwner(owner != null ? owner.getName() : "");
        machine.setStageIndex(1);
        machine.setStageCup(getStageCupForIndex(1));
        machine.setStagePayout(0);
        stopBgm(owner);
        playBgm(owner, "stage_start");
        owner.sendMessage("§e[파칭코] §f스테이지 §6#1 §f시작! 상한 §e" + machine.getStageCup() + "§f개.");
    }

    public void exitStage(Machine machine, boolean announce) {
        if (machine == null) return;
        Player p = machine.getStageOwner().isEmpty() ? null : Bukkit.getPlayerExact(machine.getStageOwner());
        stopBgm(p);
        if (announce && p != null) {
            Bukkit.broadcastMessage("§6[파칭코] §e" + p.getName() + "§f님의 스테이지 종료! 총 배출: §b" + machine.getStagePayout());
        }
        machine.setStageActive(false);
        machine.setStageOwner("");
        machine.setStageIndex(0);
        machine.setStageCup(0);
        machine.setStagePayout(0);
        if (machine.getAutoTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(machine.getAutoTaskId());
            machine.setAutoTaskId(-1);
        }
    }

    private int getStageCupForIndex(int idx) {
        // read from config if exists, else 200 + (idx-1)*50
        List<Map<?, ?>> stages = plugin.getConfig().getMapList("stages");
        if (idx >= 1 && idx <= stages.size()) {
            Map<?, ?> st = stages.get(idx - 1);
            Object cup = st.get("cup");
            if (cup instanceof Number) return ((Number) cup).intValue();
        }
        return 200 + (idx - 1) * 50;
    }

    private void playBgm(Player p, String key) {
        if (p == null) return;
        // use a neutral sound constant available in 1.16.5
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
    }

    private void stopBgm(Player p) {
        if (p == null) return;
        // Bukkit API 1.16 doesn't have a "stopSound(String)" for all, so do nothing here
        // Advanced implementation can use Player.stopSound if available per sound.
    }

    private void startTicker() {
        new BukkitRunnable() {
            @Override public void run() {
                // simple heartbeat; keep for future animation
                for (Machine m : machines.values()) {
                    if (m.isStageActive()) {
                        // no-op
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void shutdown() {
        // nothing for now
    }
}