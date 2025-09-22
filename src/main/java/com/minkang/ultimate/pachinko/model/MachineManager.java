
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
    private final Map<Integer, Machine> machines = new HashMap<>();
    private File file;
    private org.bukkit.configuration.file.YamlConfiguration yaml;

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
            List<Integer> w = new ArrayList<>();
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
        Map<String,Object> m = new HashMap<>();
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
        return machines.computeIfAbsent(id, k-> new Machine(k, base));
    }

    public ItemStack getBallItemFor(Machine m){
        if (m!=null && m.getMachineBallItem()!=null) return m.getMachineBallItem();
        return ItemUtil.readItem(plugin.getConfig().getConfigurationSection("defaultBall"));
    }

    // Stage BGM
    public void playStageBgm(Player p, int stageIndex){
        if (p==null) return;
        List<Map<?,?>> stages = plugin.getConfig().getMapList("stages");
        if (stageIndex<0 || stageIndex>=stages.size()) return;
        String snd = String.valueOf(stages.get(stageIndex).getOrDefault("bgm","BLOCK_NOTE_BLOCK_BIT"));
        try {
            p.stopSound(org.bukkit.Sound.valueOf(snd));
        }catch (Throwable ignored){}
        try {
            p.playSound(p.getLocation(), org.bukkit.Sound.valueOf(snd), 0.8f, 1.0f);
        }catch (Throwable ignored){}
    }

    public void broadcastStage(int id, int stage, int maxBall){
        if (!plugin.getConfig().getBoolean("broadcast.enabled", true)) return;
        String name = String.valueOf(plugin.getConfig().getMapList("stages").get(stage).get("name"));
        String msg = "&6[파칭코] &f#" + id + " " + name + " &7진입!";
        if (plugin.getConfig().getBoolean("broadcast.showMaxBall", true)){
            msg += " &8(최대 배출 &ex"+maxBall+"&8)";
        }
        org.bukkit.Bukkit.broadcastMessage(com.minkang.ultimate.pachinko.util.Text.color(msg));
    }

    private final java.util.Random random = new java.util.Random();

    public int chooseSlotWeighted(Machine m){
        int[] w = m.getWeights();
        int sum = 0; for (int v : w) sum += v;
        if (sum <= 0) return 4;
        int r = random.nextInt(sum), acc = 0;
        for (int i=0;i<7;i++){ acc += w[i]; if (r < acc) return i+1; }
        return 4;
    }

    public void launchBall(org.bukkit.entity.Player actor, Machine m){
        int chosen = chooseSlotWeighted(m);
        new BallRunner(plugin, m, chosen, (slot)->{
            if (slot == 4){
                // center entry behavior (back-compat)
                String mode = plugin.getConfig().getString("stageMode.type", null);
                if (mode==null) mode = plugin.getConfig().getString("centerEntryBehavior","accumulate");
                if ("directDraw".equalsIgnoreCase(mode)){
                    // consume immediate one spin or just run one draw
                    m.addPendingSpin(plugin.getConfig().getInt("centerSlot.maxPendingSpins", 5)); // grant 1 spin
                    if (m.consumeOneSpin()){
                        new com.minkang.ultimate.pachinko.model.ReelSpin(m, actor).runOne();
                    }
                    return;
                }
                int max = plugin.getConfig().getInt("stageMode.accumulate.maxPendingSpins",
                        plugin.getConfig().getInt("centerSlot.maxPendingSpins", 5));
                m.addPendingSpin(max);
                com.minkang.ultimate.pachinko.util.Text.msg(actor, "&6[파칭코] &f#" + m.getId() + " &a센터 슬롯 진입! &7추첨 기회가 &b" + m.getPendingSpins() + "&7/" + max + " &7로 누적되었습니다.");
}else{
                // stage advance chance
                java.util.List<java.util.Map<?,?>> stages = plugin.getConfig().getMapList("stages");
                int idx = Math.max(0, Math.min(m.getStageIndex(), stages.size()-1));
                double adv = 0.0;
                Object o = stages.get(idx).get("advanceChance");
                if (o instanceof Number) adv = ((Number)o).doubleValue();
                if (random.nextDouble() < adv && idx+1 < stages.size()){
                    m.setStageIndex(idx+1);
                    playStageBgm(actor, m.getStageIndex());
                    broadcastStage(m.getId(), m.getStageIndex(),
                        plugin.getConfig().getInt("goldEject.supplyCount", 5));
                }
            }
        }).run();
    }