
package com.minkang.pachinko;

import com.minkang.pachinko.cmd.RootCommand;
import com.minkang.pachinko.game.MachineManager;
import com.minkang.pachinko.game.RankingManager;
import com.minkang.pachinko.game.Settings;
import com.minkang.pachinko.listener.InteractListener;
import com.minkang.pachinko.slot.SlotManager;
import org.bukkit.plugin.java.JavaPlugin;

public class PachinkoPlugin extends JavaPlugin {

    private static PachinkoPlugin instance;
    public static PachinkoPlugin getInstance(){ return instance; }

    private Settings settings;
    private MachineManager machines;
    private RankingManager ranking;
    private SlotManager slots;

    @Override
    public void onEnable(){
        instance = this;
        saveDefaultConfig();
        this.settings = new Settings(getConfig());
        this.machines = new MachineManager(this, settings);
        this.ranking = new RankingManager(this);
        this.slots    = new SlotManager(this, settings);

        getServer().getPluginManager().registerEvents(
                new InteractListener(machines, settings, ranking, slots), this);

        getCommand("파칭코").setExecutor(new RootCommand(this, machines, settings, ranking));
        getCommand("슬롯").setExecutor(slots.getCommand());
    }

    @Override
    public void onDisable(){
        if (machines != null) machines.saveAll();
        if (slots != null) slots.saveAll();
        instance = null;
    }

    public Settings getSettings(){ return settings; }
    public MachineManager getMachines(){ return machines; }
    public RankingManager getRanking(){ return ranking; }
    public SlotManager getSlots(){ return slots; }
}
