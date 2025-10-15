package com.minkang.pachinko;

import com.minkang.pachinko.cmd.RootCommand;
import com.minkang.pachinko.game.MachineManager;
import com.minkang.pachinko.game.RankingManager;
import com.minkang.pachinko.game.Settings;
import com.minkang.pachinko.listener.InteractListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class PachinkoPlugin extends JavaPlugin {
    private static PachinkoPlugin instance;
    public static PachinkoPlugin getInstance(){ return instance; }

    private static PachinkoPlugin instance;
    private Settings settings;
    private MachineManager machineManager;
    private RankingManager rankingManager;
    private com.minkang.pachinko.slot.SlotManager slotManager;

    public static PachinkoPlugin get() { return instance; }
    public Settings getSettings() { return settings; }
    public MachineManager getMachineManager() { return machineManager; }
    public RankingManager getRankingManager() { return rankingManager; }
    public com.minkang.pachinko.slot.SlotManager getSlotManager() { return slotManager; }

    @Override
    public void onEnable(){
        instance = this; {
        instance = this;
        saveDefaultConfig();
        settings = new Settings(getConfig());
        machineManager = new MachineManager(this);
        rankingManager = new RankingManager(getDataFolder());
        slotManager = new com.minkang.pachinko.slot.SlotManager(this);

        Bukkit.getPluginManager().registerEvents(new InteractListener(machineManager, settings, rankingManager, slotManager), this);

        getCommand("파칭코").setExecutor(new RootCommand(this));
        getCommand("파칭코").setTabCompleter(new RootCommand(this));
        getCommand("슬롯").setExecutor(new com.minkang.pachinko.slot.SlotCommand(this));
    }

    public void reloadAll() {
        reloadConfig();
        settings = new Settings(getConfig());
        machineManager.reload();
        slotManager.reload();
        rankingManager.reload();
    }
}
