package com.minkang.ultimate.pachinko;

import com.minkang.ultimate.pachinko.cmd.PachinkoCommand;
import com.minkang.ultimate.pachinko.listener.InteractListener;
import com.minkang.ultimate.pachinko.model.MachineManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;
    private MachineManager manager;

    public static Main getInstance() {
        return instance;
    }

    public MachineManager getManager() {
        return manager;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        manager = new MachineManager(this);
        // listeners
        Bukkit.getPluginManager().registerEvents(new InteractListener(manager), this);
        // commands
        getCommand("파칭코").setExecutor(new PachinkoCommand(this, manager));
        getLogger().info("[UltimatePachinko] enabled.");
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.shutdown();
        getLogger().info("[UltimatePachinko] disabled.");
    }
}