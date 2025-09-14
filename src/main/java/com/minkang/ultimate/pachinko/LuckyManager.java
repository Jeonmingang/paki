package com.minkang.ultimate.pachinko;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class LuckyManager {
    private boolean isCenterSlot(Machine m, int chosenSlot){
        int cols = m.cols;
        int idx = plugin.getJackpotSlotIndex(cols);
        return chosenSlot == idx; // chosenSlot: 0..cols-1
    }

    private final Main plugin;
    private final Map<java.util.UUID, Session> sessions = new HashMap<>();
    private final Random rnd = new Random();
    private final List<StageCfg> stages = new ArrayList<>();

    // ---- Simple probability override (modes: fallback|override|scale) ----
    private void applySimpleProbabilityOverride(FileConfiguration cfg){
        try{
            // Backward compat: if boolean is used, treat true as "fallback"
            String mode;
            if (cfg.isBoolean("probability.simple-mode")){
                mode = cfg.getBoolean("probability.simple-mode") ? "fallback" : "off";
            }else{
                mode = cfg.getString("probability.simple-mode", "fallback");
            }
            if (mode==null) mode = "off";
            mode = mode.toLowerCase(java.util.Locale.ROOT);
            if ("off".equals(mode)) return;

            int globalJackpot = plugin.getGlobalJackpotPercent();
            double mult = cfg.getDouble("probability.jackpot-scale", 1.0);

            for (StageCfg sc : stages){
                if ("override".equals(mode)){
                    sc.continueChance = globalJackpot;
                    sc.centerContinueChance = globalJackpot;
                } else if ("fallback".equals(mode)){
                    if (sc.continueChance <= 0) sc.continueChance = globalJackpot;
                    if (sc.centerContinueChance == null || sc.centerContinueChance < 0) sc.centerContinueChance = globalJackpot;
                } else if ("scale".equals(mode)){
                    int c1 = (int)Math.round(Math.max(0, Math.min(100, sc.continueChance * mult)));
                    sc.continueChance = c1;
                    if (sc.centerContinueChance != null){
                        int c2 = (int)Math.round(Math.max(0, Math.min(100, sc.centerContinueChance * mult)));
                        sc.centerContinueChance = c2;
                    }
                }
            }
        }catch (Throwable ignored){}
    }
    

    static class StageCfg {
        int capIncrement = -1; // per-stage cap increment on entering this stage (-1: unset)
        Integer minPayout; Integer maxPayout;
        Integer cap;              // 고정 cap
        Integer capMin;           // 랜덤 cap 최소
        Integer capMax;           // 랜덤 cap 최대
        int clickGain;
        int continueChance;
        Integer centerContinueChance; // nullable
        String music;                 // 네임스페이스 키 권장 (minecraft:music_disc.cat)
    }

    static class Session {
        final Player player;
        final Machine machine;
        int remaining;
        int stage; // 1-based
        int cap;
        int paidTotal;
        int musicStopTaskId = -1;
        String lastMusicKey = null;
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
            stopAllMusic(s.player, s.lastMusicKey);
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
                Object cap = m.get("cap"); sc.cap = cap==null?null:((Number)cap).intValue();
                Object capMin = m.get("cap-min"); sc.capMin = capMin==null?null:((Number)capMin).intValue();
                Object capMax = m.get("cap-max"); sc.capMax = capMax==null?null:((Number)capMax).intValue();
                Object cg = m.get("click-gain"); sc.clickGain = cg==null?3:((Number)cg).intValue();
                Object mp = m.get("min-payout"); sc.minPayout = (mp==null?null:((Number)mp).intValue());
                Object mx = m.get("max-payout"); sc.maxPayout = (mx==null?null:((Number)mx).intValue());
                Object cc = m.get("continue-chance"); sc.continueChance = cc==null?40:((Number)cc).intValue();
                Object ccc = m.get("center-continue-chance"); sc.centerContinueChance = ccc==null?null:((Number)ccc).intValue();
                Object mu = m.get("music"); sc.music = mu==null?"minecraft:music_disc.cat":String.valueOf(mu);
                stages.add(sc);
            }
        } else {
            for (int i=0;i<3;i++){
                StageCfg sc = new StageCfg();
                sc.cap = 150 + i*30;
                sc.clickGain = i>=2?4:3;
                sc.continueChance = (i==0?40:(i==1?35:25));
                sc.centerContinueChance = (i==0?30:(i==1?25:20));
                sc.music = i==0?"minecraft:music_disc.cat":(i==1?"minecraft:music_disc.chirp":"minecraft:music_disc.stal");
                stages.add(sc);
            }
        }
        
    }

    
    // ---- Simple probability override (2 knobs: center-hit in RunBall, jackpot here) ----
    private void applySimpleProbabilityOverride(){
        try{
            boolean simple = plugin.getConfig().getBoolean("probability.simple-mode", true);
            if (!simple) return;
            int g = plugin.getGlobalJackpotPercent();
            for (StageCfg sc : stages){
                sc.continueChance = g;
                sc.centerContinueChance = g;
            }
        }catch (Throwable ignored){}
    }
private StageCfg current(Session s){ return stages.get(Math.max(0, Math.min(stages.size()-1, s.stage-1))); }
    private int maxStages(){ return stages.size(); }

    public boolean isInLucky(Player p){ return sessions.containsKey(p.getUniqueId()); }
    public boolean isSessionMachine(Player p, Machine m){
        Session s = sessions.get(p.getUniqueId());
        return s!=null && s.machine==m;
    }

    private void stopAllMusic(Player p, String lastKey){
        try{
            if (lastKey != null) p.stopSound(lastKey, SoundCategory.MUSIC);
            p.stopSound("minecraft:music_disc.cat",   SoundCategory.MUSIC);
            p.stopSound("minecraft:music_disc.chirp", SoundCategory.MUSIC);
            p.stopSound("minecraft:music_disc.stal",  SoundCategory.MUSIC);
        }catch(Throwable ignored){}
    }
    private void scheduleStop(Session s){
        if (s.musicStopTaskId != -1) Bukkit.getScheduler().cancelTask(s.musicStopTaskId);
        int ticks = Math.max(1, plugin.getConfig().getInt("lucky.music-stop-ticks", 2400));
        s.musicStopTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> stopAllMusic(s.player, s.lastMusicKey), ticks);
    }
    private void playStageMusic(Session s){
        stopAllMusic(s.player, s.lastMusicKey);
        StageCfg sc = current(s);
        String key = sc.music;
        float vol = (float) plugin.getConfig().getDouble("lucky.music-volume", 1.0);
        float pit = (float) plugin.getConfig().getDouble("lucky.music-pitch", 1.0);
        try{ s.player.playSound(s.player.getLocation(), key, SoundCategory.MUSIC, vol, pit); }catch(Exception ignored){}
        s.lastMusicKey = key;
        scheduleStop(s);
    }

    private int decideCapFor(StageCfg sc){
        if (sc.cap != null) return Math.max(1, sc.cap);
        if (sc.capMin != null && sc.capMax != null){
            int lo = Math.min(sc.capMin, sc.capMax);
            int hi = Math.max(sc.capMin, sc.capMax);
            return lo + rnd.nextInt(hi - lo + 1);
        }
        return 150;
    }

    private String subStatus(Session s){
        String sub = plugin.getConfig().getString("lucky.title-sub-status","&e스테이지 {stage}/{max}  &f{paid}/{cap}");
        return sub.replace("{stage}", String.valueOf(s.stage))
                .replace("{max}", String.valueOf(maxStages()))
                .replace("{paid}", String.valueOf(s.paidTotal))
                .replace("{cap}", String.valueOf(s.cap))
                .replace("&","§");
    }

    public void startLucky(Player p, Machine m){
        if (stages.isEmpty()) return;
        StageCfg sc = stages.get(0);
        Session s = new Session(p, m, decideCapFor(sc));
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
        int perClick;
        if (sc.minPayout!=null && sc.maxPayout!=null){
            int lo = Math.max(1, Math.min(sc.minPayout, sc.maxPayout));
            int hi = Math.max(sc.minPayout, sc.maxPayout);
            perClick = lo + rnd.nextInt(hi - lo + 1);
        } else {
            perClick = Math.max(1, sc.clickGain);
        }
        int give = Math.min(perClick, s.remaining);
        s.remaining -= give;
        s.paidTotal += give;
        ItemStack stack = plugin.createBallItemWith(m, 1);
        Location mouth = s.machine.getPayoutHoleLocation(plugin.getRegistry());
        for (int i=0;i<give;i++){
            p.getWorld().dropItemNaturally(mouth, stack.clone());
        }
        try{ p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.8f);}catch(Exception ignored){}
        sendStatus(s);
        if (s.remaining<=0) stepContinueOrEnd(s);
        return true;
    }

    private void stageupEffects(Session s){
        java.util.List<String> sfx = plugin.getConfig().getStringList("effects.stageup.sounds");
        java.util.List<String> pfx = plugin.getConfig().getStringList("effects.stageup.particles");
        Location at = s.machine.getPayoutHoleLocation(plugin.getRegistry());
        for (String snd : sfx){
            try{ s.player.getWorld().playSound(at, Sound.valueOf(snd), 1.0f, 1.0f);}catch(Exception ignored){}
        }
        for (String pa : pfx){
            try{ s.player.getWorld().spawnParticle(org.bukkit.Particle.valueOf(pa), at, 40, 0.3,0.3,0.3,0.01);}catch(Exception ignored){}
        }
        
    }

    private void stepContinueOrEnd(Session s){
        Player p = s.player;
        StageCfg sc = current(s);
        int chance = sc.continueChance;
        if (s.stage < maxStages() && rnd.nextInt(100) < chance){
            s.stage++;
            StageCfg next = current(s);
            s.remaining = s.cap = Math.max(1, decideCapFor(next));
            try{
                String title = plugin.getConfig().getString("lucky.title-continue","&b계속!").replace("&","§");
                p.sendTitle(title, subStatus(s), 10, 40, 10);
            }catch(Exception ignored){}
            playStageMusic(s);
            stageupEffects(s);
            // 브로드캐스트
            Bukkit.broadcastMessage(
                plugin.getConfig().getString("messages.stageup-broadcast")
                    .replace("{player}", p.getName())
                    .replace("{stage}", String.valueOf(s.stage))
                    .replace("{max}", String.valueOf(maxStages()))
                    .replace("&","§")
            );
            sendStatus(s);
        } else {
            sessions.remove(p.getUniqueId());
            stopAllMusic(p, s.lastMusicKey);
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
            StageCfg next = current(s);
            s.remaining = s.cap = Math.max(1, decideCapFor(next));

            try{
                String title = plugin.getConfig().getString("lucky.title-stageup","&6스테이지 업!").replace("&","§");
                p.sendTitle(title, subStatus(s), 10, 40, 10);
                stageupEffects(s);
            }catch(Exception ignored){}
            playStageMusic(s);
        }
        sendStatus(s);
    }

    public void broadcastJackpot(Player p, Machine m){
        String rewardName = plugin.getBallName(m);
        if (rewardName==null || rewardName.isEmpty()) rewardName = plugin.getBallMaterial(m).name();
        String msg = plugin.getConfig().getString("messages.jackpot-broadcast")
                .replace("{player}", p.getName())
                .replace("{id}", String.valueOf(m.id))
                .replace("{reward}", rewardName.replace("&","§"))
                .replace("{stage}", "1")
                .replace("{max}", String.valueOf(maxStages()))
                .replace("{cap}", String.valueOf(stages.isEmpty()?0:decideCapFor(stages.get(0))))
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


    private void sendHudRich(org.bukkit.entity.Player p, Session s){
        try{
            StageCfg sc = current(s);
            int min = (sc.minPayout>=0 && sc.maxPayout>=0) ? sc.minPayout : sc.clickGain;
            int max = (sc.minPayout>=0 && sc.maxPayout>=0) ? sc.maxPayout : sc.clickGain;
            int cap = s.cap;
            int paid = s.paidTotal;
            int remain = Math.max(0, cap - paid);

            org.bukkit.configuration.file.FileConfiguration c = plugin.getConfig();
            String titleFmt = c.getString("hud.title", "&d&lJACKPOT!");
            String subFmt   = c.getString("hud.subtitle", "&b{min}~{max} &7/ &f{cap} &8(남음 {remain})");
            int fi = c.getInt("hud.title-fadein", 5);
            int st = c.getInt("hud.title-stay", 40);
            int fo = c.getInt("hud.title-fadeout", 10);

            String title = titleFmt
                    .replace("{min}", String.valueOf(min))
                    .replace("{max}", String.valueOf(max))
                    .replace("{cap}", String.valueOf(cap))
                    .replace("{paid}", String.valueOf(paid))
                    .replace("{remain}", String.valueOf(remain))
                    .replace("&","§");

            String sub = subFmt
                    .replace("{min}", String.valueOf(min))
                    .replace("{max}", String.valueOf(max))
                    .replace("{cap}", String.valueOf(cap))
                    .replace("{paid}", String.valueOf(paid))
                    .replace("{remain}", String.valueOf(remain))
                    .replace("&","§");

            p.sendTitle(title, sub, Math.max(0,fi), Math.max(0,st), Math.max(0,fo));

            String barChar = c.getString("hud.bar-char", "■");
            int barSize = Math.max(1, c.getInt("hud.bar-size", 20));
            int filled = (cap>0) ? (int)Math.round(Math.min(1.0D, (double)paid / (double)cap) * (double)barSize) : 0;
            StringBuilder sb = new StringBuilder();
            for (int i2=0;i2<barSize;i2++){
                sb.append(i2<filled ? "§a"+barChar : "§7"+barChar);
            }
            String bar = sb.toString();
            String abFmt = c.getString("hud.actionbar", "&f[ {bar} ] &7{paid}/{cap}");
            String ab = abFmt
                    .replace("{bar}", bar)
                    .replace("{paid}", String.valueOf(paid))
                    .replace("{cap}", String.valueOf(cap))
                    .replace("&","§");
            try{
                p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent(ab));
            }catch(Throwable t){
                try{ p.sendMessage(ab); }catch(Throwable ignored){}
            }
        }catch(Throwable ignored){}
    }
}


    
    
