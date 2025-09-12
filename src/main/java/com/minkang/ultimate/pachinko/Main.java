package com.minkang.ultimate.pachinko;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;

public class Main extends JavaPlugin {
    private static Main inst;
    private Economy econ;
    private Store store;
    private MachineRegistry registry;
    private PayoutDispenser dispenser;

    @Override public void onEnable() {
        inst = this;
        saveDefaultConfig();
        setupVault();
        store = new Store();
        registry = new MachineRegistry(this);
        dispenser = new PayoutDispenser(this);
        getCommand("파칭코").setExecutor(new PCommand(this));
        Bukkit.getPluginManager().registerEvents(new UiListener(), this);
        getLogger().info("PachinkoReal v1.1.0 enabled");
    }
    private void setupVault() {
        boolean want = getConfig().getBoolean("economy.use-vault", true);
        if (!want) { econ = null; return; }
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        boolean ok = (rsp != null);
        if (!ok) { econ = null; return; }
        econ = rsp.getProvider();
    }
    public static Main get(){ return inst; }
    public boolean hasVault(){ return econ != null; }
    public Economy getEcon(){ return econ; }
    public Store getStore(){ return store; }
    public MachineRegistry getRegistry(){ return registry; }
    public PayoutDispenser getDispenser(){ return dispenser; }
}