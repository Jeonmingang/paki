
package com.minkang.ultimate.pachinko;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class Main extends JavaPlugin {

    private static Main inst;
    private MachineRegistry registry;
    private LuckyManager luckyManager;

    public static Main get() { return inst; }
    public MachineRegistry getRegistry(){ return registry; }
    public LuckyManager getLucky(){ return luckyManager; }

    @Override public void onEnable() {
        inst = this;
        saveDefaultConfig();
        fixLegacyConfig();
        this.registry = new MachineRegistry(this);
        this.registry.loadAll();
        this.luckyManager = new LuckyManager(this);

        getCommand("파칭코").setExecutor(new PCommand(this));
        Bukkit.getPluginManager().registerEvents(new UiListener(this), this);

        getLogger().info("PachinkoReal enabled. Machines loaded: " + registry.count());
    }

    @Override public void onDisable() {
        registry.saveAll();
    }

    /** Ensure new options exist without clobbering user's custom stage list. */
    private void fixLegacyConfig(){
        FileConfiguration cfg = getConfig();
        if (!cfg.isSet("spin-effect")) cfg.set("spin-effect", true);
        if (!cfg.isSet("broadcast-on-stage")) cfg.set("broadcast-on-stage", true);
        if (!cfg.isSet("center-next-stage-chance")) cfg.set("center-next-stage-chance", 0.5);
        if (!cfg.isSet("cap.base")) cfg.set("cap.base", 64);
        if (!cfg.isSet("cap.per-stage")) cfg.set("cap.per-stage", 32);
        if (!cfg.isSet("reward.min")) cfg.set("reward.min", 2);
        if (!cfg.isSet("reward.max")) cfg.set("reward.max", 5);
        if (!cfg.isSet("structure.sign-offset-x")) {
            cfg.set("structure.sign-offset-x",  -1);
            cfg.set("structure.sign-offset-y",   1);
            cfg.set("structure.sign-offset-z",   0);
        }
        if (!cfg.isSet("messages.stage-announce")){
            cfg.set("messages.stage-announce", "&6&l[파칭코] &e{player}&f님이 &d스테이지 {stage}&f(/{max})에 진입! 더 큰 보상을 향해!");
        }
        if (!cfg.isSet("messages.slot-line")){
            cfg.set("messages.slot-line", "&7슬롯: &f[{a}] &7- &f[{b}] &7- &f[{c}]");
        }
        if (!cfg.isSet("messages.lucky-status")){
            cfg.set("messages.lucky-status", "&e{stage}/{max} &7단계 | &b지급합계: &f{paid} &7/ &b맥시멈: &f{cap} &7(남음 {remain})");
        }
        // Add a default staged BGM only if user has not defined stages
        if (!cfg.isSet("stages")){
            cfg.set("stages[0].name", "STAGE 1");
            cfg.set("stages[0].bgm", java.util.Arrays.asList(Sound.UI_TOAST_CHALLENGE_COMPLETE.name()));
            cfg.set("stages[1].name", "STAGE 2");
            cfg.set("stages[1].bgm", java.util.Arrays.asList(Sound.ENTITY_PLAYER_LEVELUP.name()));
            cfg.set("stages[2].name", "STAGE 3");
            cfg.set("stages[2].bgm", java.util.Arrays.asList(Sound.MUSIC_DISC_MALL.name()));
        }
        saveConfig();
    }

    /** Called when a player clears a stage to announce and play BGM. */
    public void announceStage(Player p, int stageIndex, int maxStages){
        String msg = getConfig().getString("messages.stage-announce", "&6&l[파칭코] &e{player}&f님이 &d스테이지 {stage}&f(/{max})에 진입!");
        msg = msg.replace("{player}", p.getName()).replace("{stage}", String.valueOf(stageIndex)).replace("{max}", String.valueOf(maxStages)).replace("&","§");
        if (getConfig().getBoolean("broadcast-on-stage", true)){
            Bukkit.broadcastMessage(msg);
        }else{
            p.sendMessage(msg);
        }
        // BGM per stage (if defined)
        List<?> list = getConfig().getList("stages");
        if (list != null && stageIndex-1 < list.size()){
            Object stageObj = list.get(stageIndex-1);
            if (stageObj instanceof java.util.Map){
                Object bgmObj = ((java.util.Map<?,?>)stageObj).get("bgm");
                if (bgmObj instanceof java.util.List){
                    for (Object s : (java.util.List<?>)bgmObj){
                        try{
                            Sound sound = Sound.valueOf(String.valueOf(s));
                            p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
                        }catch (IllegalArgumentException ignored){}
                    }
                }
            }
        }
    }
}
