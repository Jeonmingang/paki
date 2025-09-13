package com.minkang.ultimate.pachinko;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class LuckyManager {
    private final Main plugin;
    private final Map<java.util.UUID, Session> sessions = new HashMap<>();
    private final Random rnd = new Random();
    private final List<StageCfg> stages = new ArrayList<>();

    static class StageCfg {
        int cap;
        int clickGain;
        int continueChance;
        Integer centerContinueChance; // nullable
        String music;
    }

    static class Session {
        final Player player;
        final Machine machine;
        int remaining;
        int stage; // 1-based
        int cap;
        int paidTotal;
        int musicStopTaskId = -1;
        Session(Player p, Machine m, int cap){
            this.player=p; this.machine=m; this.cap=cap;
            this.remaining=cap; this.stage=1; this.paidTotal=0;
        }
    }

    public LuckyManager(Main p){
        this.plugin = p;
        loadStagesFromConfig();
    }

    public void reload(){
        for (Session s : new ArrayList<Session>(sessions.values())){
            stopAllMusic(s.player);
        }
        sessions.clear();
        loadStagesFromConfig();
    }

    private void loadStagesFromConfig(){
        stages.clear();
        FileConfiguration cfg = plugin.getConfig();
        java.util.List<java.util.Map<?,?>> list = cfg.getMapList("lucky.stages");
        if (list!=null && !list.isEmpty()){
            for (java.util.Map<?,?> m : list){
                StageCfg sc = new StageCfg();
                Object cap = m.get("cap"); sc.cap = cap==null?150:((Number)cap).intValue();
                Object cg = m.get("click-gain"); sc.clickGain = cg==null?3:((Number)cg).intValue();
                Object cc = m.get("continue-chance"); sc.continueChance = cc==null?40:((Number)cc).intValue();
                Object ccc = m.get("center-continue-chance"); sc.centerContinueChance = ccc==null?null:((Number)ccc).intValue();
                Object mu = m.get("music"); sc.music = mu==null?"MUSIC_DISC_CAT":String.valueOf(mu);
                stages.add(sc);
            }
        } else {
            for (int i=0;i<3;i++){
                StageCfg sc = new StageCfg();
                sc.cap = 150 + i*30;
                sc.clickGain = i>=2?4:3;
                sc.continueChance = (i==0?40:(i==1?35:25));
                sc.centerContinueChance = (i==0?30:(i==1?25:20));
                sc.music = i==0?"MUSIC_DISC_CAT":(i==1?"MUSIC_DISC_CHIRP":"MUSIC_DISC_STAL");
                stages.add(sc);
            }
        }
    }

    private StageCfg current(Session s){ return stages.get(Math.max(0, Math.min(stages.size()-1, s.stage-1))); }
    private int maxStages(){ return stages.size(); }

    public boolean isInLucky(Player p){ return sessions.containsKey(p.getUniqueId()); }
    public boolean isSessionMachine(Player p, Machine m){
        Session s = sessions.get(p.getUniqueId());
        return s!=null && s.machine==m;
    }

    private String subStatus(Session s){
        String sub = plugin.getConfig().getString("lucky.title-sub-status","&e스테이지 {stage}/{max}  &f{paid}/{cap}");
        return sub.replace("{stage}", String.valueOf(s.stage))
                .replace("{max}", String.valueOf(maxStages()))
                .replace("{paid}", String.valueOf(s.paidTotal))
                .replace("{cap}", String.valueOf(s.cap))
                .replace("&","§");
    }

    private void stopAllMusic(Player p){
        try{
            p.stopSound(Sound.MUSIC_DISC_CAT);
            p.stopSound(Sound.MUSIC_DISC_CHIRP);
            p.stopSound(Sound.MUSIC_DISC_STAL);
        }catch(Exception ignored){}
    }

    private void scheduleStop(Session s){
        if (s.musicStopTaskId != -1) Bukkit.getScheduler().cancelTask(s.musicStopTaskId);
        int ticks = Math.max(1, plugin.getConfig().getInt("lucky.music-stop-ticks", 2400));
        s.musicStopTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> stopAllMusic(s.player), ticks);
    }

    private void playStageMusic(Session s){
        stopAllMusic(s.player);
        StageCfg sc = current(s);
        String ms = sc.music!=null?sc.music:"MUSIC_DISC_CAT";
        float vol = (float) plugin.getConfig().getDouble("lucky.music-volume", 1.0);
        float pit = (float) plugin.getConfig().getDouble("lucky.music-pitch", 1.0);
        try{ s.player.playSound(s.player.getLocation(), Sound.valueOf(ms), vol, pit); }catch(Exception ignored){}
        scheduleStop(s);
    }

    public void startLucky(Player p, Machine m){
        if (stages.isEmpty()) return;
        StageCfg sc = stages.get(0);
        Session s = new Session(p, m, sc.cap);
        sessions.put(p.getUniqueId(), s);
        try {
            String title = plugin.getConfig().getString("lucky.title-start","&d럭키 타임!").replace("&","§");
            p.sendTitle(title, subStatus(s), 10, 60, 10);
        } catch (Exception ignored){}
        playStageMusic(s);
        sendStatus(s);
    }

    public boolean handleClick(Player p, Machine m){
        Session s = sessions.get(p.getUniqueId());
        if (s==null || s.machine != m) return false;
        if (s.remaining <= 0){
            stepContinueOrEnd(s);
            return true;
        }
        StageCfg sc = current(s);
        int perClick = Math.max(1, sc.clickGain);
        int give = Math.min(perClick, s.remaining);
        s.remaining -= give;
        s.paidTotal += give;
        ItemStack stack = plugin.createBallItem(1);
        Location mouth = s.machine.getPayoutHoleLocation(plugin.getRegistry());
        for (int i=0;i<give;i++){
            p.getWorld().dropItemNaturally(mouth, stack.clone());
        }
        try{ p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.8f);}catch(Exception ignored){}
        sendStatus(s);
        if (s.remaining<=0) stepContinueOrEnd(s);
        return true;
    }

    private void stepContinueOrEnd(Session s){
        Player p = s.player;
        StageCfg sc = current(s);
        int chance = sc.continueChance;
        if (s.stage < maxStages() && rnd.nextInt(100) < chance){
            s.stage++;
            StageCfg next = current(s);
            s.remaining = s.cap = Math.max(1, next.cap);
            try{
                String title = plugin.getConfig().getString("lucky.title-continue","&b계속!").replace("&","§");
                p.sendTitle(title, subStatus(s), 10, 40, 10);
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            }catch(Exception ignored){}
            playStageMusic(s);
            sendStatus(s);
        } else {
            sessions.remove(p.getUniqueId());
            stopAllMusic(p);
            p.sendMessage(plugin.getConfig().getString("messages.lucky-finish").replace("{total}", String.valueOf(s.paidTotal)).replace("&","§"));
        }
    }

    public void onCenterDuringLucky(Player p, Machine m){
        Session s = sessions.get(p.getUniqueId());
        if (s==null || s.machine != m) return;
        int add = plugin.getConfig().getInt("lucky.cap-increase-on-center",0);
        if (add>0){
            s.cap += add;
            s.remaining += add;
            try{
                String title = plugin.getConfig().getString("lucky.title-capup","&a천장 +{amount}!").replace("{amount}", String.valueOf(add)).replace("&","§");
                p.sendTitle(title, subStatus(s), 5, 30, 5);
                p.playSound(p.getLocation(), Sound.BLOCK_PISTON_EXTEND, 1.0f, 0.9f);
            }catch(Exception ignored){}
        }
        StageCfg sc = current(s);
        Integer cont = sc.centerContinueChance;
        if (cont!=null && s.stage < maxStages() && rnd.nextInt(100)<cont){
            s.stage++;
            try{
                String title = plugin.getConfig().getString("lucky.title-stageup","&6스테이지 업!").replace("&","§");
                p.sendTitle(title, subStatus(s), 10, 40, 10);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.1f);
            }catch(Exception ignored){}
            playStageMusic(s);
        }
        sendStatus(s);
    }

    public void broadcastJackpot(Player p, Machine m){
        String msg = plugin.getConfig().getString("messages.jackpot-broadcast")
                .replace("{player}", p.getName())
                .replace("{id}", String.valueOf(m.id))
                .replace("&","§");
        Bukkit.broadcastMessage(msg);
        try{ p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);}catch(Exception ignored){}
    }

    private void sendStatus(Session s){
        String msg = plugin.getConfig().getString("messages.lucky-status")
                .replace("{stage}", String.valueOf(s.stage))
                .replace("{max}", String.valueOf(maxStages()))
                .replace("{paid}", String.valueOf(s.paidTotal))
                .replace("{cap}", String.valueOf(s.cap))
                .replace("{remain}", String.valueOf(s.remaining))
                .replace("&","§");
        s.player.sendMessage(msg);
        s.player.sendTitle("", msg, 0, 10, 0);
    }
}
