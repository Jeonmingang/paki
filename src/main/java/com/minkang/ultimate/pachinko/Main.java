package com.minkang.ultimate.pachinko;

import com.minkang.ultimate.pachinko.cmd.PachinkoCommand;
import com.minkang.ultimate.pachinko.model.MachineManager;
import com.minkang.ultimate.pachinko.model.RankingService;
import com.minkang.ultimate.pachinko.listener.InteractListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

public class Main extends JavaPlugin {
    private static Main inst;
    private MachineManager machineManager;
    private RankingService rankingService;
    private Logger log;

    public static Main get() { return inst; }
    public MachineManager machines() { return machineManager; }
    public RankingService ranks() { return rankingService; }

    @Override
    public void onEnable() {
        inst = this;
        this.log = getLogger();
        saveDefaultConfig();
        // Default world fallback
        if (!getConfig().isSet("defaultWorld")) {
            getConfig().set("defaultWorld", "bskyblock_world");
            saveConfig();
        }
        // Load machines.yml
        saveResourceIfNotExists("machines.yml");
        machineManager = new MachineManager(this);
        machineManager.load();
        // Load ranking file
        rankingService = new RankingService(this);
        rankingService.load();
        // Register command
        if (getCommand("파칭코") != null) {
            getCommand("파칭코").setExecutor(new PachinkoCommand(this));
        }
        // Register listeners
        Bukkit.getPluginManager().registerEvents(new InteractListener(this), this);
        // Auto-advance task
        startAutoAdvanceTask();
        log.info("[UltimatePachinko] v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (rankingService != null) rankingService.save();
        if (machineManager != null) machineManager.save();
    }

    public void reloadAll() {
        reloadConfig();
        machineManager.load();
        rankingService.save(); // persist before reload of tasks
        startAutoAdvanceTask();
    }

    private void startAutoAdvanceTask() {
        // cancel existing tasks
        Bukkit.getScheduler().cancelTasks(this);
        // schedule 1-tick animator tasks happen within classes
        // schedule auto-advance
        boolean enabled = getConfig().getBoolean("autoAdvance.enabled", true);
        int interval = getConfig().getInt("autoAdvance.intervalTicks", 20);
        if (interval < 1) interval = 20;
        if (enabled) {
            Bukkit.getScheduler().runTaskTimer(this, () -> {
                machineManager.tickAutoAdvance();
            }, interval, interval);
        }
    }

    private void saveResourceIfNotExists(String name) {
        File f = new File(getDataFolder(), name);
        if (!f.exists()) {
            saveResource(name, false);
        }
    }
}
