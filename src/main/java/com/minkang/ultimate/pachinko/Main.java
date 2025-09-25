
package com.minkang.ultimate.pachinko;

import com.minkang.ultimate.pachinko.cmd.PachinkoCommand;
import com.minkang.ultimate.pachinko.listener.InteractListener;
import com.minkang.ultimate.pachinko.model.MachineManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class Main extends JavaPlugin {

    private static Main instance;
    private MachineManager machineManager;
    private File machinesFile;
    private FileConfiguration machinesConfig;
    private NamespacedKey ballKey;

    public static Main inst() {
        return instance;
    }

    public NamespacedKey getBallKey() {
        return ballKey;
    }

    public MachineManager getMachineManager() {
        return machineManager;
    }

    public FileConfiguration getMachinesConfig() {
        return machinesConfig;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        setupMachinesYml();
        ballKey = new NamespacedKey(this, getConfig().getString("ball.nbtKey", "pachinko_ball"));

        machineManager = new MachineManager(this);
        machineManager.loadAll(machinesConfig);

        // Commands / Listeners
        getCommand("파칭코").setExecutor(new PachinkoCommand(this));
        Bukkit.getPluginManager().registerEvents(new InteractListener(this), this);

        getLogger().info("UltimatePachinko enabled.");
    }

    @Override
    public void onDisable() {
        machineManager.saveAll(machinesConfig);
        try {
            machinesConfig.save(machinesFile);
        } catch (IOException e) {
            getLogger().warning("Failed to save machines.yml: " + e.getMessage());
        }
        getLogger().info("UltimatePachinko disabled.");
    }

    private void setupMachinesYml() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        machinesFile = new File(getDataFolder(), "machines.yml");
        if (!machinesFile.exists()) {
            saveResource("machines.yml", false);
        }
        machinesConfig = YamlConfiguration.loadConfiguration(machinesFile);
    }
}
