
package com.minkang.ultimate.pachinko.model;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.util.ItemUtil;
import com.minkang.ultimate.pachinko.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class MachineManager {
    private double chanceToDouble(String s){
        if (s==null) return 0;
        s=s.trim();
        try{
            if (s.contains("/")){
                String[] a=s.split("/");
                double n=Double.parseDouble(a[0]); double d=Double.parseDouble(a[1]);
                if (n<=0||d<=0) return 0; return n/d; // expecting 1/x -> 1/x
            }
            return Double.parseDouble(s);
        }catch(Throwable ignored){return 0;}
    }

    private final Main plugin;
    private final Map<Integer, Machine> machines = new HashMap<Integer, Machine>();
    private File file;
    private org.bukkit.configuration.file.YamlConfiguration yaml;
    private final java.util.Random random = new java.util.Random();

    public MachineManager(Main plugin){ this.plugin=plugin; }

    public Map<Integer, Machine> getAll(){ return machines; }

    public void load(){
        file = new File(plugin.getDataFolder(), "machines.yml");
        if (!file.exists()) plugin.saveResource("machines.yml", false);
        yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        machines.clear();
        ConfigurationSection sec = yaml.getConfigurationSection("machines");
        if (sec!=null){
            for (String idStr : sec.getKeys(false)){
                int id = Integer.parseInt(idStr);
                ConfigurationSection msec = sec.getConfigurationSection(idStr);
                Location base = readLoc(msec.getConfigurationSection("base"));
                Machine m = new Machine(id, base);
                m.setGoldButton(readLoc(msec.getConfigurationSection("gold")));
                m.setDiamondBlock(readLoc(msec.getConfigurationSection("diamond")));
                m.setCoalButton(readLoc(msec.getConfigurationSection("coal")));
                if (msec.isList("weights")){
                    List<Integer> w = msec.getIntegerList("weights");
                    int[] arr = new int[7];
                    for (int i=0;i<7;i++) arr[i] = i<w.size()? w.get(i):14;
                    m.setWeights(arr);
                }
                if (msec.isConfigurationSection("ball")) m.setMachineBallItem(ItemUtil.readItem(msec.getConfigurationSection("ball")));
                machines.put(id, m);
            }
        }
    }

    public void save(){
        if (yaml==null) yaml = new org.bukkit.configuration.file.YamlConfiguration();
        yaml.set("machines", null);
        for (Machine m : machines.values()){
            String k = "machines."+m.getId();
            yaml.createSection(k);
            yaml.set(k+".base", writeLoc(m.getBase()));
            yaml.set(k+".gold", writeLoc(m.getGoldButton()));
            yaml.set(k+".diamond", writeLoc(m.getDiamondBlock()));
            yaml.set(k+".coal", writeLoc(m.getCoalButton()));
            List<Integer> w = new ArrayList<Integer>();
            for (int i=0;i<7;i++) w.add(m.getWeights()[i]);
            yaml.set(k+".weights", w);
            if (m.getMachineBallItem()!=null){
                ConfigurationSection bsec = yaml.createSection(k+".ball");
                writeItem(bsec, m.getMachineBallItem());
            }
        }
        try { yaml.save(file); } catch (Exception e){ e.printStackTrace(); }
    }

    private Map<String,Object> writeLoc(Location l){
        if (l==null) return null;
        Map<String,Object> m = new HashMap<String,Object>();
        m.put("w", l.getWorld().getName()); m.put("x", l.getBlockX()); m.put("y", l.getBlockY()); m.put("z", l.getBlockZ());
        return m;
    }
    private Location readLoc(ConfigurationSection sec){
        if (sec==null) return null;
        return new Location(Bukkit.getWorld(sec.getString("w")), sec.getInt("x"), sec.getInt("y"), sec.getInt("z"));
    }
    private void writeItem(ConfigurationSection sec, ItemStack it){
        sec.set("type", it.getType().name());
        if (it.hasItemMeta()){
            if (it.getItemMeta().hasDisplayName()) sec.set("name", it.getItemMeta().getDisplayName());
            if (it.getItemMeta().hasLore()) sec.set("lore", it.getItemMeta().getLore());
        }
    }

    public Machine get(int id){ return machines.get(id); }
    public Machine ensure(int id, Location base){
        Machine m = machines.get(id);
        if (m==null){
            m = new Machine(id, base);
            machines.put(id, m);
        }
        return m;
    }

    public ItemStack getBallItemFor(Machine m){
        if (m!=null && m.getMachineBallItem()!=null) return m.getMachineBallItem();
        return ItemUtil.readItem(plugin.getConfig().getConfigurationSection("defaultBall"));
    }

    
    public void giveBall(org.bukkit.entity.Player p, int amount){
        org.bukkit.inventory.ItemStack ball = getBallItemFor(null);
        if (ball==null) return;
        ball = ball.clone();
        ball.setAmount(amount);
        p.getInventory().addItem(ball);
    }
    
public int chooseSlotWeighted(Machine m){
        int[] w = m.getWeights();
        int sum = 0; for (int v : w) sum += v;
        if (sum <= 0) return 4;
        int r = random.nextInt(sum), acc = 0;
        for (int i=0;i<7;i++){ acc += w[i]; if (r < acc) return i+1; }
        return 4;
    }

    public void launchBall(final Player actor, final Machine m){
        int chosen = chooseSlotWeighted(m);
        new BallRunner(plugin, m, chosen, new java.util.function.IntConsumer(){
            @Override public void accept(int slot){
                if (slot == 4){
                    String mode = plugin.getConfig().getString("stageMode.type", null);
                    if (mode==null) mode = plugin.getConfig().getString("centerEntryBehavior","accumulate");
                    if ("directDraw".equalsIgnoreCase(mode)){
                        new com.minkang.ultimate.pachinko.model.ReelSpin(m, actor).runOne();
                    }else{
                        int max = plugin.getConfig().getInt("stageMode.accumulate.maxPendingSpins",
                                plugin.getConfig().getInt("centerSlot.maxPendingSpins", 5));
                        m.addPendingSpin(max);
                        com.minkang.ultimate.pachinko.util.Text.msg(actor, "&e중앙 진입! &7추첨 기회가 &b"+m.getPendingSpins()+"&7/"+max+" 로 누적되었습니다.");
                    }
                    // 중앙 진입 시 스테이지 상승 시도
                    advanceStageIfLucky(actor, m, true);
                }else{
                    // 측면 진입 시 (설정에 따라) 스테이지 상승 시도
                    advanceStageIfLucky(actor, m, false);
                }
            }
        }).run();
    }

    // Stage BGM
    public void playStageBgm(Player p, int stageIndex){
        if (p==null) return;
        java.util.List<java.util.Map<?,?>> stages = plugin.getConfig().getMapList("stages");
        if (stageIndex<0 || stageIndex>=stages.size()) return;
        Object bgmObj = stages.get(stageIndex).get("bgm");
        String snd = (bgmObj != null) ? String.valueOf(bgmObj) : "BLOCK_NOTE_BLOCK_BIT";
        try { p.stopSound(org.bukkit.Sound.valueOf(snd)); } catch (Throwable ignored){}
        try { p.playSound(p.getLocation(), org.bukkit.Sound.valueOf(snd), 0.8f, 1.0f); } catch (Throwable ignored){}
    }

    public void broadcastStage(int id, int stage, int maxBall){
        if (!plugin.getConfig().getBoolean("broadcast.enabled", true)) return;
        String name = String.valueOf(plugin.getConfig().getMapList("stages").get(stage).get("name"));
        String msg = "&6[파칭코] &f#" + id + " " + name + " &7진입!";
        if (plugin.getConfig().getBoolean("broadcast.showMaxBall", true)){
            msg += " &8(최대 배출 &ex"+maxBall+"&8)";
        }
        Bukkit.broadcastMessage(Text.color(msg));
    }

    // FEVER start
    public void startFever(Player p, Machine m){
        if (!plugin.getConfig().getBoolean("fever.enabled", true)) return;
        int spins = plugin.getConfig().getInt("fever.spins", 5);
        m.setFeverSpinsLeft(spins);
        m.resetMissStreak();
        try {
            String s = plugin.getConfig().getString("fever.bgm","MUSIC_DISC_PIGSTEP");
            if (p!=null) p.playSound(p.getLocation(), org.bukkit.Sound.valueOf(s), 1.0f, 1.0f);
        }catch (Throwable ignored){}
        String title = plugin.getConfig().getString("fever.title","&c&lFEVER TIME!");
        if (p!=null) p.sendTitle(Text.color(title), "", 5, 40, 10);
        if (plugin.getConfig().getBoolean("broadcast.enabled", true)){
            Bukkit.broadcastMessage(Text.color("&6[파칭코] &f#"+m.getId()+" &cFEVER TIME 시작!"));
        }
    
    }
    private boolean isTrue(FileConfiguration c, String path, boolean defV){
        return c.isBoolean(path) ? c.getBoolean(path) : defV;
    }

    /** 중앙/측면 진입 시 스테이지 상승 시도 */
    public void advanceStageIfLucky(Player actor, Machine m, boolean fromCenter){
        java.util.List<java.util.Map<?,?>> stages = plugin.getConfig().getMapList("stages");
        if (stages==null || stages.isEmpty()) return;
        int idx = Math.max(0, Math.min(m.getStageIndex(), stages.size()-1));

        boolean allowCenter = isTrue(plugin.getConfig(), "stageProgression.advanceOnCenter", true);
        boolean allowSide   = isTrue(plugin.getConfig(), "stageProgression.advanceOnSide", false);
        if ((fromCenter && !allowCenter) || (!fromCenter && !allowSide)) return;

        double adv = 0.0;
        Object key = fromCenter ? stages.get(idx).get("advanceOnCenterChance") : null;
        if (key instanceof Number) adv = ((Number)key).doubleValue();
        else {
            Object o = stages.get(idx).get("advanceChance");
            if (o instanceof Number) adv = ((Number)o).doubleValue();
        }
        if (adv <= 0) return;

        if (random.nextDouble() < adv && idx+1 < stages.size()){
            m.setStageIndex(idx+1);
            playStageBgm(actor, m.getStageIndex());
            int cap = 64;
            Object oc = stages.get(m.getStageIndex()).get("payoutCap");
            if (oc instanceof Number) cap = ((Number)oc).intValue();
            broadcastStage(m.getId(), m.getStageIndex(), cap);
        }
    }

    /** 트리플 당첨 브로드캐스트 + 즉시 배출 옵션 처리 */
    public void onTripleWin(Player actor, Machine m, int gained){
        if (actor==null) return;
        if (plugin.getConfig().getBoolean("payout.broadcastOnTriple", true)){
            int stage = m.getStageIndex();
            java.util.List<java.util.Map<?,?>> stages = plugin.getConfig().getMapList("stages");
            String stageName = (stages!=null && stage<stages.size()) ? String.valueOf(stages.get(stage).get("name")) : "";
            int cap = 64;
            if (stages!=null && stage<stages.size()){
                Object oc = stages.get(stage).get("payoutCap");
                if (oc instanceof Number) cap = ((Number)oc).intValue();
            }
            String msg = "&6[파칭코] &e"+actor.getName()+"&f 님이 &6트리플 당첨! &7(기계 #"+m.getId()+")";
            if (plugin.getConfig().getBoolean("payout.showCapInBroadcast", true)){
                msg += " &8| cap: &e"+cap;
            }
            Bukkit.broadcastMessage(com.minkang.ultimate.pachinko.util.Text.color(msg));
        }
        if ("instant".equalsIgnoreCase(plugin.getConfig().getString("payout.mode","instant"))){
            // 바로 드랍
            org.bukkit.inventory.ItemStack ball = getBallItemFor(m);
            if (ball!=null && m.getDiamondBlock()!=null){
                for (int i=0;i<gained;i++){
                    m.getDiamondBlock().getWorld().dropItemNaturally(m.getDiamondBlock(), ball.clone());
                }
                try { actor.playSound(actor.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.0f);}catch(Throwable ignored){}
            }
        }
    }



    // "1/319" or "0.003" -> double
    public double parseChance(Object v, double defV){
        try{
            if (v==null) return defV;
            if (v instanceof Number) return ((Number)v).doubleValue();
            String s = String.valueOf(v).trim();
            if (s.contains("/")){
                String[] sp = s.split("/");
                if (sp.length==2){
                    double den = Double.parseDouble(sp[1].trim());
                    if (den>0) return 1.0/den;
                }
            }
            return Double.parseDouble(s);
        }catch(Exception e){ return defV; }
    }

    /** 현재 상태에 따른 유효 잭팟 확률(ST/時短 우선) */
    public double effectiveJackpotChance(Machine m, double base){
        if (m.inST()){
            Object ch = plugin.getConfig().getConfigurationSection("jpachinko.kakuhen").get("chance");
            return Math.min(1.0, parseChance(ch, base));
        }
        if (m.inJidan()){
            Object ch = plugin.getConfig().getConfigurationSection("jpachinko.jidan").get("chance");
            return Math.min(1.0, parseChance(ch, base));
        }
        return base;
    }

    public void startST(org.bukkit.entity.Player p, Machine m){
        if (!plugin.getConfig().getBoolean("jpachinko.kakuhen.enabled", true)) return;
        int spins = plugin.getConfig().getInt("jpachinko.kakuhen.spins", 100);
        m.setStSpinsLeft(spins);
        m.setJidanSpinsLeft(0);
        m.resetMissStreak();
        String title = plugin.getConfig().getString("jpachinko.kakuhen.title","&c&l확변(ST) 시작!");
        if (p!=null) p.sendTitle(com.minkang.ultimate.pachinko.util.Text.color(title), "", 5, 40, 10);
        try {
            String s = plugin.getConfig().getString("jpachinko.kakuhen.bgm","MUSIC_DISC_STRAD");
            if (p!=null) p.playSound(p.getLocation(), org.bukkit.Sound.valueOf(s), 1.0f, 1.0f);
        }catch (Throwable ignored){}
        if (plugin.getConfig().getBoolean("broadcast.enabled", true)){
            Bukkit.broadcastMessage(com.minkang.ultimate.pachinko.util.Text.color("&6[파칭코] &f#"+m.getId()+" &c확변(ST) 돌입!"));
        }
    }

    public void startJidan(org.bukkit.entity.Player p, Machine m){
        if (!plugin.getConfig().getBoolean("jpachinko.jidan.enabled", true)) return;
        int spins = plugin.getConfig().getInt("jpachinko.jidan.spins", 50);
        m.setJidanSpinsLeft(spins);
        m.setStSpinsLeft(0);
        m.resetMissStreak();
        String title = plugin.getConfig().getString("jpachinko.jidan.title","&b&l시단 시작!");
        if (p!=null) p.sendTitle(com.minkang.ultimate.pachinko.util.Text.color(title), "", 5, 40, 10);
        try {
            String s = plugin.getConfig().getString("jpachinko.jidan.bgm","MUSIC_DISC_WAIT");
            if (p!=null) p.playSound(p.getLocation(), org.bukkit.Sound.valueOf(s), 1.0f, 1.0f);
        }catch (Throwable ignored){}
        if (plugin.getConfig().getBoolean("broadcast.enabled", true)){
            Bukkit.broadcastMessage(com.minkang.ultimate.pachinko.util.Text.color("&6[파칭코] &f#"+m.getId()+" &b시단 시작!"));
        }
    }

    public void endSpecialModes(org.bukkit.entity.Player p, Machine m){
        if (!m.inST() && !m.inJidan()) return;
        m.setStSpinsLeft(0);
        m.setJidanSpinsLeft(0);
        int toStage = plugin.getConfig().getInt("jpachinko.end.toStageIndex", 1);
        m.setStageIndex(Math.max(0, toStage));
        if (p!=null){
            try { p.stopSound(org.bukkit.Sound.MUSIC_DISC_STRAD);}catch(Throwable ignored){}
            try { p.stopSound(org.bukkit.Sound.MUSIC_DISC_WAIT);}catch(Throwable ignored){}
        }
        if (plugin.getConfig().getBoolean("broadcast.enabled", true)){
            Bukkit.broadcastMessage(com.minkang.ultimate.pachinko.util.Text.color("&6[파칭코] &f#"+m.getId()+" &7특수모드 종료."));
        }
    }

    /** 잭팟 발생 시 확변/시단 분기 */
    public void onJackpot(org.bukkit.entity.Player actor, Machine m){
        double rate = plugin.getConfig().getDouble("jpachinko.kakuhen.rateOnJackpot", 0.5);
        if (plugin.getConfig().getBoolean("jpachinko.kakuhen.enabled", true) && random.nextDouble() < rate){
            startST(actor, m);
        }else if (plugin.getConfig().getBoolean("jpachinko.jidan.enabled", true)){
            startJidan(actor, m);
        }else{
            // 아무 모드도 없으면 그대로 유지
        }
    }

}

    public void enterStage(Machine m, org.bukkit.entity.Player p){
        if (m.isStageActive()) return;
        java.util.List<java.util.Map<String,Object>> stages = plugin.getConfig().getMapList("stages");
        if (stages==null || stages.isEmpty()) return;
        m.setStageActive(true);
        m.setStageIndex(1);
        int cup = ((Number)stages.get(0).getOrDefault("cup", 200)).intValue();
        m.setStageCup(cup);
                        // global celebration broadcast + ranking record
                        try{
                            String stageName = String.valueOf(stages.get(Math.min(idx, stages.size()-1)).getOrDefault("name","Stage #"+m.getStageIndex()));
                            String msg = "&6&l★ 축하! &e스테이지 상승 &f-> &b" + stageName + " &7(cup " + m.getStageCup() + ")";
                            org.bukkit.Bukkit.broadcastMessage(com.minkang.ultimate.pachinko.util.Text.color(msg));
                            // play a short celebration sound (avoids BGM overlap)
                            try{
                                org.bukkit.Sound s = org.bukkit.Sound.valueOf("ENTITY_PLAYER_LEVELUP");
                                for (org.bukkit.entity.Player pl: org.bukkit.Bukkit.getOnlinePlayers()) pl.playSound(m.getBase(), s, 1.0f, 1.0f);
                            }catch(Throwable ignored){}
                            // firework (best-effort)
                            try{
                                org.bukkit.entity.Firework fw = (org.bukkit.entity.Firework)m.getBase().getWorld().spawnEntity(m.getBase().clone().add(0.5,1.0,0.5), org.bukkit.entity.EntityType.FIREWORK);
                                org.bukkit.inventory.meta.FireworkMeta meta = fw.getFireworkMeta();
                                meta.addEffect(org.bukkit.FireworkEffect.builder().withColor(org.bukkit.Color.AQUA).withFlicker().withTrail().build());
                                meta.setPower(1);
                                fw.setFireworkMeta(meta);
                            }catch(Throwable ignored){}
                            // ranking append
                            try{
                                java.io.File f = new java.io.File(plugin.getDataFolder(), "ranking.yml");
                                if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
                                org.bukkit.configuration.file.YamlConfiguration y = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
                                java.util.List<java.util.Map<String,Object>> arr = (java.util.List<java.util.Map<String,Object>>)y.getList("records");
                                if (arr==null){ arr=new java.util.ArrayList<>(); }
                                java.util.Map<String,Object> rec = new java.util.HashMap<>();
                                rec.put("player", m.getStageOwner());
                                rec.put("stage", m.getStageIndex());
                                rec.put("payout", m.getStagePayout());
                                rec.put("time", System.currentTimeMillis());
                                rec.put("reason", "stage up");
                                arr.add(rec);
                                y.set("records", arr);
                                y.save(f);
                            }catch(Throwable ignored){}
                        }catch(Throwable ignored){}
        m.setStagePayout(0);
        m.setStageOwner(p!=null? p.getName(): null);
        // stop previous BGM then play stage bgm
        String last = m.getLastBgm();
        if (last!=null){
            try{ org.bukkit.Sound s=org.bukkit.Sound.valueOf(last);
                 for (org.bukkit.entity.Player pl: org.bukkit.Bukkit.getOnlinePlayers()) pl.stopSound(s);
            }catch(Throwable ignored){}
        }
        String bgm = String.valueOf(stages.get(0).getOrDefault("bgm","MUSIC_DISC_WAIT"));
        m.setLastBgm(bgm);
        try{ org.bukkit.Sound s=org.bukkit.Sound.valueOf(bgm);
             for (org.bukkit.entity.Player pl: org.bukkit.Bukkit.getOnlinePlayers()) pl.playSound(m.getBase(), s, 1.0f,1.0f);
        }catch(Throwable ignored){}
        scheduleAutoAdvance(m);
        com.minkang.ultimate.pachinko.util.Text.msg(p,"&6[스테이지]&f 진입: &e#"+m.getStageIndex()+" &7(cup "+m.getStageCup()+")");
    }
    public void endStage(Machine m, org.bukkit.entity.Player p, String reason){
        cancelAuto(m);
        // stop current bgm
        String last = m.getLastBgm();
        if (last!=null){
            try{ org.bukkit.Sound s=org.bukkit.Sound.valueOf(last);
                 for (org.bukkit.entity.Player pl: org.bukkit.Bukkit.getOnlinePlayers()) pl.stopSound(s);
            }catch(Throwable ignored){}
        }
        // ranking
        try{
            java.io.File f = new java.io.File(plugin.getDataFolder(), "ranking.yml");
            if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
            org.bukkit.configuration.file.YamlConfiguration y = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
            java.util.List<java.util.Map<String,Object>> arr = (java.util.List<java.util.Map<String,Object>>)y.getList("records");
            if (arr==null){ arr=new java.util.ArrayList<>(); }
            java.util.Map<String,Object> rec = new java.util.HashMap<>();
            rec.put("player", m.getStageOwner());
            rec.put("stage", m.getStageIndex());
            rec.put("payout", m.getStagePayout());
            rec.put("time", System.currentTimeMillis());
            rec.put("reason", reason==null? "": reason);
            arr.add(rec);
            y.set("records", arr);
            y.save(f);
        }catch(Throwable ignored){}
        com.minkang.ultimate.pachinko.util.Text.msg(p, "&6[스테이지 종료] &7이유: &f"+(reason==null?"":reason));
        // reset
        m.setStageActive(false); m.setStageIndex(0); m.setStageCup(0); m.setStagePayout(0); m.setStageOwner(null);
    }
    private void scheduleAutoAdvance(Machine m){
        cancelAuto(m);
        int interval = plugin.getConfig().getInt("autoAdvance.intervalTicks", 20);
        if (interval <= 0) interval = 20;
        int taskId = org.bukkit.Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable(){
            public void run(){
                if (!m.isStageActive()) return;
                // roll for stage advance based on current stage's advanceChance
                java.util.List<java.util.Map<String,Object>> stages = plugin.getConfig().getMapList("stages");
                int idx = Math.max(1, m.getStageIndex());
                if (idx > stages.size()) idx = stages.size();
                String ac = String.valueOf(stages.get(idx-1).getOrDefault("advanceChance", "0"));
                double prob = chanceToDouble(ac);
                if (prob > 0){
                    if (Math.random() < prob){
                        // advance stage
                        m.setStageIndex(idx+1);
                        int cup = ((Number)stages.get(Math.min(idx, stages.size()-1)).getOrDefault("cup", m.getStageCup()+50)).intValue();
                        m.setStageCup(cup);
                        // global celebration broadcast + ranking record
                        try{
                            String stageName = String.valueOf(stages.get(Math.min(idx, stages.size()-1)).getOrDefault("name","Stage #"+m.getStageIndex()));
                            String msg = "&6&l★ 축하! &e스테이지 상승 &f-> &b" + stageName + " &7(cup " + m.getStageCup() + ")";
                            org.bukkit.Bukkit.broadcastMessage(com.minkang.ultimate.pachinko.util.Text.color(msg));
                            // play a short celebration sound (avoids BGM overlap)
                            try{
                                org.bukkit.Sound s = org.bukkit.Sound.valueOf("ENTITY_PLAYER_LEVELUP");
                                for (org.bukkit.entity.Player pl: org.bukkit.Bukkit.getOnlinePlayers()) pl.playSound(m.getBase(), s, 1.0f, 1.0f);
                            }catch(Throwable ignored){}
                            // firework (best-effort)
                            try{
                                org.bukkit.entity.Firework fw = (org.bukkit.entity.Firework)m.getBase().getWorld().spawnEntity(m.getBase().clone().add(0.5,1.0,0.5), org.bukkit.entity.EntityType.FIREWORK);
                                org.bukkit.inventory.meta.FireworkMeta meta = fw.getFireworkMeta();
                                meta.addEffect(org.bukkit.FireworkEffect.builder().withColor(org.bukkit.Color.AQUA).withFlicker().withTrail().build());
                                meta.setPower(1);
                                fw.setFireworkMeta(meta);
                            }catch(Throwable ignored){}
                            // ranking append
                            try{
                                java.io.File f = new java.io.File(plugin.getDataFolder(), "ranking.yml");
                                if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
                                org.bukkit.configuration.file.YamlConfiguration y = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
                                java.util.List<java.util.Map<String,Object>> arr = (java.util.List<java.util.Map<String,Object>>)y.getList("records");
                                if (arr==null){ arr=new java.util.ArrayList<>(); }
                                java.util.Map<String,Object> rec = new java.util.HashMap<>();
                                rec.put("player", m.getStageOwner());
                                rec.put("stage", m.getStageIndex());
                                rec.put("payout", m.getStagePayout());
                                rec.put("time", System.currentTimeMillis());
                                rec.put("reason", "stage up");
                                arr.add(rec);
                                y.set("records", arr);
                                y.save(f);
                            }catch(Throwable ignored){}
                        }catch(Throwable ignored){}
                        String bgm = String.valueOf(stages.get(Math.min(idx, stages.size()-1)).getOrDefault("bgm","MUSIC_DISC_STRAD"));
                        // stop previous then play new
                        String last = m.getLastBgm();
                        if (last!=null){
                            try{ org.bukkit.Sound s=org.bukkit.Sound.valueOf(last);
                                 for (org.bukkit.entity.Player pl: org.bukkit.Bukkit.getOnlinePlayers()) pl.stopSound(s);
                            }catch(Throwable ignored){}
                        }
                        m.setLastBgm(bgm);
                        try{ org.bukkit.Sound s=org.bukkit.Sound.valueOf(bgm);
                             for (org.bukkit.entity.Player pl: org.bukkit.Bukkit.getOnlinePlayers()) pl.playSound(m.getBase(), s, 1.0f,1.0f);
                        }catch(Throwable ignored){}
                        com.minkang.ultimate.pachinko.util.Text.msg(org.bukkit.Bukkit.getConsoleSender(), "[Pachinko] Stage advanced to #"+m.getStageIndex()+" cup="+cup);
                    }
                }
                // if payout reached cup and not advanced further just end
                if (m.getStagePayout() >= m.getStageCup()){
                    endStage(m, org.bukkit.Bukkit.getPlayerExact(m.getStageOwner()), "cup 도달");
                }
            }
        }, interval, interval);
        m.setAutoTaskId(taskId);
    }
    private void cancelAuto(Machine m){
        int id = m.getAutoTaskId();
        if (id != -1){ try{ org.bukkit.Bukkit.getScheduler().cancelTask(id);}catch(Throwable ignored){} }
        m.setAutoTaskId(-1);
    }
    public void payoutOne(Machine m, org.bukkit.entity.Player p){
        if (!m.isStageActive()){ com.minkang.ultimate.pachinko.util.Text.msg(p, "&7스테이지 상태가 아닙니다."); return; }
        if (m.getStagePayout() >= m.getStageCup()){ com.minkang.ultimate.pachinko.util.Text.msg(p, "&7이미 상한 도달"); return; }
        // determine exit location (diamond block); fallback to base top
        org.bukkit.Location out = m.getDiamondBlock()!=null ? m.getDiamondBlock().clone().add(0.5,1.1,0.5) : m.getBase().clone().add(0.5,1.1,0.5);
        // choose marble item (machine > default)
        org.bukkit.inventory.ItemStack ball = m.getMachineBallItem();
        if (ball==null){ try{ ball = com.minkang.ultimate.pachinko.util.ItemUtil.readItem(plugin.getConfig().getConfigurationSection("defaultBall")); }catch(Throwable ignored){} }
        if (ball==null){ ball = new org.bukkit.inventory.ItemStack(org.bukkit.Material.SLIME_BALL); }
        // if exit is a container (hopper/chest/barrel), insert; else drop item toward it
        try{
            org.bukkit.block.BlockState st = out.getBlock().getState();
            if (st instanceof org.bukkit.inventory.InventoryHolder){
                ((org.bukkit.inventory.InventoryHolder)st).getInventory().addItem(ball.clone());
            }else{
                org.bukkit.entity.Item it = out.getWorld().dropItem(out, ball.clone());
                try{ it.setVelocity(new org.bukkit.util.Vector(0, -0.2, 0)); }catch(Throwable ignored){}
                try{ it.setPickupDelay(5); }catch(Throwable ignored){}
            }
        }catch(Throwable ignored){
            out.getWorld().dropItem(out, ball.clone());
        }
        m.setStagePayout(m.getStagePayout()+1);
        com.minkang.ultimate.pachinko.util.Text.msg(p, "&b배출 &f"+m.getStagePayout()+"&7/&e"+m.getStageCup());
        if (m.getStagePayout() >= m.getStageCup()){
            endStage(m, p, "cup 도달");
        }
    }
