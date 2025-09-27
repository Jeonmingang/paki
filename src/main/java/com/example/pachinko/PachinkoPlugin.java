package com.example.pachinko;

import com.example.pachinko.command.PachinkoCommand;
import com.example.pachinko.listener.InteractListener;
import com.example.pachinko.manager.MachineManager;
import com.example.pachinko.manager.RankingManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class PachinkoPlugin extends JavaPlugin {

    private static PachinkoPlugin instance;
    private MachineManager machineManager;
    private RankingManager rankingManager;

    public static PachinkoPlugin get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.machineManager = new MachineManager(this);
        this.machineManager.loadMachines();
        this.rankingManager = new RankingManager(this);

        // Commands
        getCommand("파칭코").setExecutor(new PachinkoCommand(this));

        // Listeners
        Bukkit.getPluginManager().registerEvents(new InteractListener(this), this);

        // Idle unlock task
        Bukkit.getScheduler().runTaskTimer(this, () -> machineManager.tickIdleUnlock(), 20L, 20L);
        getLogger().info("UltimatePachinkoPlus enabled.");
    }

    @Override
    public void onDisable() {
        if (machineManager != null) machineManager.saveMachines();
        if (rankingManager != null) rankingManager.save();
        getLogger().info("UltimatePachinkoPlus disabled.");
    }

    public MachineManager machines() { return machineManager; }
    public RankingManager ranking() { return rankingManager; }

    public String msg(String key) {
        FileConfiguration cfg = getConfig();
        String prefix = cfg.getString("messages.prefix", "");
        return prefix + cfg.getString("messages." + key, key);
    }
}
