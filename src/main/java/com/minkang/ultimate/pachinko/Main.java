
package com.minkang.ultimate.pachinko;

import com.minkang.ultimate.pachinko.cmd.PachinkoCommand;
import com.minkang.ultimate.pachinko.data.DataStore;
import com.minkang.ultimate.pachinko.model.MachineManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;
    private MachineManager machineManager;
    private DataStore dataStore;

    public MachineManager getMachineManager(){ return machineManager; }

    public static Main get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        dataStore = new DataStore(this);
        machineManager = new MachineManager(this, dataStore);
        machineManager.loadMachines();
        getServer().getPluginManager().registerEvents(new com.minkang.ultimate.pachinko.listener.InteractListener(), this);
        getCommand("파칭코").setExecutor(new PachinkoCommand(machineManager));
        getLogger().info("[UltimatePachinko] Enabled with " + machineManager.getMachines().size() + " machines.");
        // Safety: task cleanup on reloads handled in onDisable via manager#shutdown
    }

    @Override
    public void onDisable() {
        try {
            if (machineManager != null) {
                machineManager.shutdown();
            }
            if (dataStore != null) {
                machineManager.saveMachines();
                dataStore.save();
            }
        } catch (Exception e) {
            getLogger().warning("Failed to save on disable: " + e.getMessage());
        }
    }
}
