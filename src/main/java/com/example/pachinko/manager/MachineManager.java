package com.example.pachinko.manager;

import com.example.pachinko.PachinkoPlugin;
import com.example.pachinko.model.Machine;
import com.example.pachinko.model.StageDef;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Hopper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class MachineManager {

    private final PachinkoPlugin plugin;
    private final Map<Integer, Machine> machines = new HashMap<>();
    private File machinesFile;
    private YamlConfiguration machinesYaml;

    // 확률/설정 캐시
    public static class BaseProb {
        public double left, center, right;
    }
    private BaseProb baseProb;
    private int height;
    private int ascendTicks;
    private boolean showIntoHopper;
    private int maxSimultaneous;
    private boolean requireExclusive;
    private int drawMaxTokens;
    private double drawMatchProbability;
    private int spinTotal;
    private int spinFirstStop;
    private int spinSecondStop;
    private List<StageDef> stages;

    public MachineManager(PachinkoPlugin plugin) {
        this.plugin = plugin;
        reloadConfigValues();
        this.machinesFile = new File(plugin.getDataFolder(), "machines.yml");
        if (!machinesFile.exists()) {
            try {
                machinesFile.getParentFile().mkdirs();
                machinesFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.machinesYaml = YamlConfiguration.loadConfiguration(machinesFile);
    }

    public void reloadConfigValues() {
        FileConfiguration cfg = plugin.getConfig();
        height = cfg.getInt("machine.height", 6);
        ascendTicks = cfg.getInt("machine.ascend-ticks", 2);
        showIntoHopper = cfg.getBoolean("machine.show-into-hopper", true);
        maxSimultaneous = cfg.getInt("machine.max-simultaneous-balls", 12);
        requireExclusive = cfg.getBoolean("machine.require-exclusive-ball", true);
        drawMaxTokens = cfg.getInt("draw.max-tokens", 5);
        drawMatchProbability = cfg.getDouble("draw.match-probability", 0.10);
        spinTotal = cfg.getInt("draw.spin.total-ticks", 80);
        spinFirstStop = cfg.getInt("draw.spin.first-stop", 40);
        spinSecondStop = cfg.getInt("draw.spin.second-stop", 60);

        baseProb = new BaseProb();
        baseProb.left = cfg.getDouble("probability.base.lane.left", 0.33);
        baseProb.center = cfg.getDouble("probability.base.lane.center", 0.34);
        baseProb.right = cfg.getDouble("probability.base.lane.right", 0.33);

        // stages
        stages = new ArrayList<>();
        List<Map<?,?>> list = cfg.getMapList("stages");
        for (Map<?,?> m : list) {
            StageDef s = new StageDef();
            Object idObj = m.get("id");
            if (idObj == null) continue;
            s.id = (idObj instanceof Number) ? ((Number) idObj).intValue() : Integer.parseInt(String.valueOf(idObj));
            s.name = String.valueOf(m.getOrDefault("name", "STAGE " + s.id));
            s.cup = ((Number) m.getOrDefault("cup", 50)).intValue();
            s.nextStageChance = ((Number) m.getOrDefault("next-stage-chance", 0.2)).doubleValue();
            Map<?,?> lane = (Map<?,?>) m.get("lane");
            if (lane != null) {
                s.laneLeft = ((Number) lane.getOrDefault("left", baseProb.left)).doubleValue();
                s.laneCenter = ((Number) lane.getOrDefault("center", baseProb.center)).doubleValue();
                s.laneRight = ((Number) lane.getOrDefault("right", baseProb.right)).doubleValue();
            } else {
                s.laneLeft = baseProb.left; s.laneCenter = baseProb.center; s.laneRight = baseProb.right;
            }
            s.enterBroadcast = String.valueOf(m.getOrDefault("enter-broadcast", ""));
            s.upBroadcast = String.valueOf(m.getOrDefault("up-broadcast", ""));
            stages.add(s);
        }
        // sort by id
        stages.sort(Comparator.comparingInt(s -> s.id));
    }

    public List<StageDef> getStages() { return stages; }
    public StageDef getStageDef(int id) {
        return stages.stream().filter(s -> s.id == id).findFirst().orElse(null);
    }

    public void saveMachines() {
        machinesYaml.set("machines", null);
        for (Map.Entry<Integer, Machine> e : machines.entrySet()) {
            String path = "machines." + e.getKey();
            Machine m = e.getValue();
            machinesYaml.set(path + ".left", serializeLoc(m.getLeftBase()));
            machinesYaml.set(path + ".mid", serializeLoc(m.getMidBase()));
            machinesYaml.set(path + ".right", serializeLoc(m.getRightBase()));
            List<String> tops = new ArrayList<>();
            for (int i=0;i<3;i++) tops.add(serializeLoc(m.getTopHopper(i)));
            machinesYaml.set(path + ".tops", tops);
            if (m.getExclusiveBall() != null) machinesYaml.set(path + ".exclusiveBall", m.getExclusiveBall());
        }
        try { machinesYaml.save(machinesFile); } catch (IOException ex) { ex.printStackTrace(); }
    }

    public void loadMachines() {
        machines.clear();
        ConfigurationSection sec = machinesYaml.getConfigurationSection("machines");
        if (sec == null) return;
        for (String k : sec.getKeys(false)) {
            try {
                int id = Integer.parseInt(k);
                Location left = deserializeLoc(machinesYaml.getString("machines."+k+".left"));
                Location mid = deserializeLoc(machinesYaml.getString("machines."+k+".mid"));
                Location right = deserializeLoc(machinesYaml.getString("machines."+k+".right"));
                List<String> tops = machinesYaml.getStringList("machines."+k+".tops");
                Location[] topLocs = new Location[3];
                for (int i=0;i<3 && i<tops.size();i++) topLocs[i] = deserializeLoc(tops.get(i));
                Machine m = new Machine(id, left, mid, right, topLocs);
                ItemStack ex = machinesYaml.getItemStack("machines."+k+".exclusiveBall");
                if (ex != null) m.setExclusiveBall(ex);
                machines.put(id, m);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed loading machine " + k + ": " + ex.getMessage());
            }
        }
    }

    // 설치
    public boolean installMachine(Player p, int id) {
        if (machines.containsKey(id)) {
            p.sendMessage(ChatColor.RED + "이미 존재하는 기계 번호입니다.");
            return false;
        }
        Location base = p.getLocation().getBlock().getLocation();
        // 앞쪽 한 칸
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        Location front = base.add(dir.multiply(2));
        World w = p.getWorld();

        // 바닥 3블럭: 좌(석탄) 중(금) 우(다이아)
        Location mid = front.getBlock().getLocation();
        // 좌/우는 좌측을 기준 yaw에 따라 결정
        // 간단히 x축/ z축 별로 배치
        boolean alongX = Math.abs(dir.getX()) > Math.abs(dir.getZ());
        Location left, right;
        if (alongX) {
            // X축 진행 -> 좌우는 Z방향
            left = mid.clone().add(0,0,1);
            right = mid.clone().add(0,0,-1);
        } else {
            left = mid.clone().add(-1,0,0);
            right = mid.clone().add(1,0,0);
        }

        setBlock(left, Material.COAL_BLOCK);
        setBlock(mid, Material.GOLD_BLOCK);
        setBlock(right, Material.DIAMOND_BLOCK);

        // 중간/윗부분: 전부 호퍼 + 가운데 기둥은 철창
        // 좌/우는 유리기둥
        for (int i=1;i<=height;i++) {
            Location lc = left.clone().add(0,i,0);
            Location mc = mid.clone().add(0,i,0);
            Location rc = right.clone().add(0,i,0);

            setBlock(lc, Material.HOPPER);
            setBlock(rc, Material.HOPPER);

            // 가운데는 철창(연출)
            setBlock(mc, Material.IRON_BARS);
        }

        // 상단 홉퍼 라인 (좌/중/우 정확 위치)
        Location topY = mid.clone().add(0, height+1, 0);
        Location topLeft = left.clone().add(0, height+1, 0);
        Location topRight = right.clone().add(0, height+1, 0);
        setBlock(topLeft, Material.HOPPER);
        setBlock(topY, Material.HOPPER);
        setBlock(topRight, Material.HOPPER);

        // 호퍼 방향 아래로
        for (Location l : Arrays.asList(topLeft, topY, topRight)) {
            Block b = l.getBlock();
            if (b.getType() == Material.HOPPER) {
                BlockData data = b.getBlockData();
                if (data instanceof Hopper) {
                    Hopper h = (Hopper) data;
                    h.setFacing(BlockFace.DOWN);
                    b.setBlockData(h, false);
                }
            }
        }
        // 시각 연출용 유리기둥
        for (int i=1;i<=height;i++) {
            setBlock(left.clone().add(alongX ? -1:0, i, alongX?0:-1), Material.GLASS);
            setBlock(right.clone().add(alongX ? 1:0, i, alongX?0:1), Material.GLASS);
        }

        Machine m = new Machine(id, left, mid, right, new Location[]{ topLeft, topY, topRight });
        machines.put(id, m);
        saveMachines();
        p.sendMessage(ChatColor.GREEN + "파칭코 기계 #" + id + " 설치 완료!");
        return true;
    }

    private void setBlock(Location loc, Material type) {
        Block b = loc.getBlock();
        b.setType(type, false);
        // 추가 데이터 조정(철창 방향 등) 필요 시 여기서
        if (type == Material.IRON_BARS) {
            BlockData data = b.getBlockData();
            if (data instanceof Orientable) {
                // 기본값이면 됨
            }
        }
    }

    public Machine byBottom(Block b) {
        for (Machine m : machines.values()) {
            if (same(b.getLocation(), m.getLeftBase()) || same(b.getLocation(), m.getMidBase()) || same(b.getLocation(), m.getRightBase())) {
                return m;
            }
        }
        return null;
    }

    public Machine getMachine(int id) { return machines.get(id); }

    private boolean same(Location a, Location b) {
        if (a == null || b == null) return false;
        return a.getWorld().equals(b.getWorld()) && a.getBlockX()==b.getBlockX() && a.getBlockY()==b.getBlockY() && a.getBlockZ()==b.getBlockZ();
    }

    private String serializeLoc(Location l) {
        return l.getWorld().getName()+","+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ();
    }
    private Location deserializeLoc(String s) {
        if (s == null) return null;
        String[] t = s.split(",");
        World w = Bukkit.getWorld(t[0]);
        return new Location(w, Integer.parseInt(t[1]), Integer.parseInt(t[2]), Integer.parseInt(t[3]));
    }

    public int getHeight() { return height; }
    public int getAscendTicks() { return ascendTicks; }
    public boolean isShowIntoHopper() { return showIntoHopper; }
    public int getMaxSimultaneous() { return maxSimultaneous; }
    public boolean isRequireExclusive() { return requireExclusive; }
    public int getDrawMaxTokens() { return drawMaxTokens; }
    public double getDrawMatchProbability() { return drawMatchProbability; }
    public int getSpinTotal() { return spinTotal; }
    public int getSpinFirstStop() { return spinFirstStop; }
    public int getSpinSecondStop() { return spinSecondStop; }

    public BaseProb getBaseProb() { return baseProb; }

    public void tickIdleUnlock() {
        long now = System.currentTimeMillis();
        for (Machine m : machines.values()) {
            if (m.getOccupant() != null && now - m.getLastActionAt() > 120_000L) {
                m.release();
            }
        }
    }

    public double[] laneProbFor(Machine m) {
        if (m.getStage() <= 0) {
            return new double[]{ baseProb.left, baseProb.center, baseProb.right };
        } else {
            StageDef s = getStageDef(m.getStage());
            if (s == null) return new double[]{ baseProb.left, baseProb.center, baseProb.right };
            return new double[]{ s.laneLeft, s.laneCenter, s.laneRight };
        }
    }

    public int chooseLane(Machine m) {
        double[] p = laneProbFor(m);
        double total = p[0]+p[1]+p[2];
        double r = ThreadLocalRandom.current().nextDouble(total);
        if (r < p[0]) return 0;
        if (r < p[0] + p[1]) return 1;
        return 2;
    }

    public int firstStageId() {
        return stages.isEmpty() ? 1 : stages.get(0).id;
    }

    public StageDef nextStage(int current) {
        List<StageDef> list = stages.stream().sorted(Comparator.comparingInt(s->s.id)).collect(Collectors.toList());
        for (int i=0;i<list.size();i++) {
            if (list.get(i).id == current && i+1 < list.size()) return list.get(i+1);
        }
        return null;
    
    public java.util.Collection<Machine> allMachines() { return machines.values(); }

    public Machine findByOccupant(java.util.UUID uuid) {
        for (Machine m : machines.values()) {
            if (uuid != null && uuid.equals(m.getOccupant())) return m;
        }
        return null;
    }

}
