
package com.minkang.ultimate.pachinko;

import com.minkang.ultimate.pachinko.cmd.PachinkoCommand;
import com.minkang.ultimate.pachinko.listener.InteractListener;
import com.minkang.ultimate.pachinko.model.MachineManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private static Main instance;
    private MachineManager manager;

    public static Main get(){ return instance; }
    public MachineManager getManager(){ return manager; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        manager = new MachineManager(this);
        manager.load();
        getServer().getPluginManager().registerEvents(new InteractListener(manager), this);
        if (getCommand("파칭코")!=null) getCommand("파칭코").setExecutor(new PachinkoCommand(manager));
        getLogger().info("[UltimatePachinko] v"+getDescription().getVersion()+" enabled, machines="+manager.getAll().size());
    }

    @Override
    public void onDisable() {
        if (manager!=null) manager.save();
    }
}
