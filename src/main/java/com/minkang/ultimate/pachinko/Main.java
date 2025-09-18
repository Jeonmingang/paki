
package com.minkang.ultimate.pachinko;

import com.minkang.ultimate.pachinko.cmd.PachinkoCommand;
import com.minkang.ultimate.pachinko.data.DataStore;
import com.minkang.ultimate.pachinko.listener.InteractListener;
import com.minkang.ultimate.pachinko.model.MachineManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private static Main instance;
    private MachineManager machineManager;
    private DataStore dataStore;

    public static Main get() { return instance; }
    public MachineManager getMachineManager(){ return machineManager; }
    public DataStore getDataStore(){ return dataStore; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        dataStore = new DataStore(this);
        machineManager = new MachineManager(this, dataStore);
        machineManager.loadMachines();
        getServer().getPluginManager().registerEvents(new InteractListener(), this);
        getCommand("파칭코").setExecutor(new PachinkoCommand(machineManager));
        getLogger().info("[UltimatePachinko] Enabled with " + machineManager.getMachines().size() + " machines.");
    }

    @Override
    public void onDisable() {
        try {
            if (machineManager != null) {
                machineManager.saveMachines();
                machineManager.shutdown();
            }
            if (dataStore != null) {
                dataStore.save();
            }
        } catch (Exception e) {
            getLogger().warning("Failed to save on disable: " + e.getMessage());
        }
    }
}
