package com.minkang.ultimate.pachinko;

import com.minkang.ultimate.pachinko.command.PachinkoCommand;
import com.minkang.ultimate.pachinko.service.MachineService;
import com.minkang.ultimate.pachinko.service.PlayService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class PachinkoPlugin extends JavaPlugin {

    private static PachinkoPlugin instance;
    private MachineService machineService;
    private PlayService playService;

    public static PachinkoPlugin inst() { return instance; }
    public MachineService machines() { return machineService; }
    public PlayService play() { return playService; }

    @Override public void onLoad() { instance = this; }

    @Override public void onEnable() {
        saveDefaultConfig();
        machineService = new MachineService(this);
        playService = new PlayService(this);
        machineService.loadAll();
        getCommand("파칭코").setExecutor(new PachinkoCommand(this));
        getCommand("파칭코").setTabCompleter(new PachinkoCommand(this));
        Bukkit.getPluginManager().registerEvents(playService, this);
        getLogger().info("[UltimatePachinko] Enabled");
    }

    @Override public void onDisable() {
        machines().saveAll();
        getLogger().info("[UltimatePachinko] Disabled");
    }
}
