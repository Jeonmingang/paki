
package com.example.pachinko;

import com.example.pachinko.commands.PachinkoCommand;
import com.example.pachinko.listeners.InteractListener;
import com.example.pachinko.machine.MachineManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class UltimatePachinko extends JavaPlugin {
    private static UltimatePachinko instance;
    private MachineManager machineManager;
    public static UltimatePachinko inst(){ return instance; }
    public MachineManager machines(){ return machineManager; }

    @Override public void onEnable(){
        instance = this;
        saveDefaultConfig();
        reloadConfig();
        machineManager = new MachineManager(this);
        machineManager.loadFromConfig();
        PachinkoCommand cmd = new PachinkoCommand(this);
        getCommand("pachinko").setExecutor(cmd);
        getCommand("pachinko").setTabCompleter(cmd);
        Bukkit.getPluginManager().registerEvents(new InteractListener(this), this);
    }
    @Override public void onDisable(){
        if (machineManager != null) machineManager.saveToConfig();
        saveConfig();
        instance = null;
    }
    public void reloadAll(){
        reloadConfig();
        machineManager.loadFromConfig();
    }
}
