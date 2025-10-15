package com.minkang.pachinko;

import com.minkang.pachinko.cmd.RootCommand;
import com.minkang.pachinko.game.MachineManager;
import com.minkang.pachinko.game.RankingManager;
import com.minkang.pachinko.game.Settings;
import com.minkang.pachinko.listener.InteractListener;
import com.minkang.pachinko.slot.SlotCommand;
import com.minkang.pachinko.slot.SlotManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class PachinkoPlugin extends JavaPlugin {

    private static PachinkoPlugin instance;
    private Settings settings;
    private MachineManager machines;
    private RankingManager ranking;
    private SlotManager slots;

    public static PachinkoPlugin getInstance() { return instance; }
    /** 일부 코드에서 get()을 사용하길래 호환용 alias 제공 */
    public static PachinkoPlugin get() { return instance; }

    public Settings getSettings() { return settings; }
    public MachineManager getMachines() { return machines; }
    public MachineManager getMachineManager() { return machines; }
    public RankingManager getRanking() { return ranking; }
    public RankingManager getRankingManager() { return ranking; }
    public SlotManager getSlots() { return slots; }
    public SlotManager getSlotManager() { return slots; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // load settings from config
        this.settings = new Settings(getConfig());

        // managers
        this.machines = new MachineManager(this);
        this.ranking  = new RankingManager(getDataFolder());
        this.slots    = new SlotManager(this);

        // listeners
        getServer().getPluginManager().registerEvents(new InteractListener(machines, settings, ranking, slots), this);

        // commands
        PluginCommand pachinkoCmd = getCommand("파칭코");
        if (pachinkoCmd != null) {
            RootCommand root = new RootCommand(this);
            pachinkoCmd.setExecutor(root);
            pachinkoCmd.setTabCompleter(root);
        }

        PluginCommand slotCmd = getCommand("슬롯");
        if (slotCmd != null) {
            slotCmd.setExecutor(new SlotCommand(this));
        }
    }

    @Override
    public void onDisable() {
        try {
            if (machines != null) machines.saveAll();
        } catch (Throwable ignored) {}
        try {
            if (slots != null) slots.saveAll();
        } catch (Throwable ignored) {}
        try {
            if (ranking != null) ranking.save();
        } catch (Throwable ignored) {}
    }

    /** config/manager 통합 리로드 */
    public void reloadAll() {
        reloadConfig();
        this.settings = new Settings(getConfig());
        if (this.machines != null) this.machines.reload();
        if (this.slots != null) this.slots.reload();
    }
}
