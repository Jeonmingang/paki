
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
