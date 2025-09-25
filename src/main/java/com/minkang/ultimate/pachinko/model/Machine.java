
package com.minkang.ultimate.pachinko.model;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.util.ItemUtil;
import com.minkang.ultimate.pachinko.util.Text;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class Machine {

    public enum State { IDLE, IN_STAGE, PAYOUT }

    private final Main plugin;
    private final int id;
    private final World world;
    private final Location base; // 바닥 중앙(금블럭이 중앙에 오도록)
    private final BlockFace facing;

    private List<Stage> stages = new ArrayList<Stage>();
    private int stageIndex = -1;
    private State state = State.IDLE;

    // 인터랙션 및 진행
    private int drawTickets = 0;
    private int payoutRemaining = 0;
    private boolean animating = false;
    private boolean bgmPlaying = false;
    private UUID lastActor = null;

    // 구슬 템플릿 및 잠금설정
    private Material ballMaterial = null;
    private String ballName = null;
    private java.util.List<String> ballLore = null;
    private boolean lockBallToMachine = false;

    // 구조의 특수 블럭 좌표(회전 포함 계산)
    private Location locDiamond;
    private Location locGold;
    private Location locCoal;

    public Machine(Main plugin, int id, World world, Location base, BlockFace facing) {
        this.plugin = plugin;
        this.id = id;
        this.world = world;
        this.base = base.clone();
        this.facing = facing;
    }

    public int getId() { return id; }
    public World getWorld() { return world; }
    public Location getBase() { return base.clone(); }
    public BlockFace getFacing() { return facing; }
    public State getState() { return state; }
    public List<Stage> getStages() { return stages; }

    
    public boolean isLockBallToMachine() { return lockBallToMachine; }
    public void setLockBallToMachine(boolean v) { this.lockBallToMachine = v; }
    public Material getBallMaterial() { return ballMaterial; }
    public String getBallName() { return ballName; }
    public java.util.List<String> getBallLore() { return ballLore; }
    public void setBallTemplate(Material mat, String name, java.util.List<String> lore) {
        this.ballMaterial = mat;
        this.ballName = name;
        this.ballLore = lore;
    }
    public boolean acceptsBall(org.bukkit.inventory.ItemStack item) {
        if (!com.minkang.ultimate.pachinko.util.ItemUtil.isBall(item)) return false;
        Integer mid = com.minkang.ultimate.pachinko.util.ItemUtil.getMachineId(item);
        if (lockBallToMachine) {
            if (mid == null || mid.intValue() != this.id) return false;
        }
        if (ballMaterial != null && item.getType() != ballMaterial) return false;
        return true;
    }
public void setStages(List<Stage> list) {
        this.stages = list;
    }

    // ----------------------------------------------------
    // 설치
    // ----------------------------------------------------
    public void buildStructure() {
        // 기준점: base (바닥 중앙 금블럭 자리)
        // 좌->우: 다이아, 금, 석탄 (플레이어가 보는 방향 기준)
        Location gold = relative(0, 0, 0);
        Location dia = relative(-1, 0, 0);
        Location coal = relative(1, 0, 0);

        setBlock(dia, Material.DIAMOND_BLOCK);
        setBlock(gold, Material.GOLD_BLOCK);
        setBlock(coal, Material.COAL_BLOCK);

        this.locDiamond = dia;
        this.locGold = gold;
        this.locCoal = coal;

        // 위쪽 프레임(유리/철창/호퍼) - 9칸 호퍼 / 8칸 유리
        final int heightStart = 1;
        // 1단: 유리 8칸 (중앙 정렬 위해 좌-4 ~ +3)
        for (int x = -4; x <= 3; x++) {
            setBlock(relative(x, heightStart, 0), Material.GLASS);
        }
        // 2단: 철창 기둥
        for (int x = -4; x <= 3; x++) {
            setBlock(relative(x, heightStart + 1, 0), Material.IRON_BARS);
        }
        // 3단: 유리 8칸
        for (int x = -4; x <= 3; x++) {
            setBlock(relative(x, heightStart + 2, 0), Material.GLASS);
        }
        // 4단: 철창 기둥
        for (int x = -4; x <= 3; x++) {
            setBlock(relative(x, heightStart + 3, 0), Material.IRON_BARS);
        }
        // 5단: 호퍼 9칸 (좌 -4 ~ +4)
        for (int x = -4; x <= 4; x++) {
            Location l = relative(x, heightStart + 4, 0);
            setBlock(l, Material.HOPPER);
            // 호퍼 방향은 아래로 (시각상 균일)
            try {
                Block b = l.getBlock();
                Hopper hopper = (Hopper) b.getState();
                hopper.update();
            } catch (Exception ignored) {}
        }
    }

    private void setBlock(Location l, Material mat) {
        Block b = world.getBlockAt(l);
        b.setType(mat, false);
    }

    // 상대 좌표 계산 (facing 회전)
    private Location relative(int dx, int dy, int dz) {
        // facing 기준으로 X/Z 회전
        int rx = dx;
        int rz = dz;
        if (facing == BlockFace.SOUTH) {
            rx = dx;  rz = dz;
        } else if (facing == BlockFace.NORTH) {
            rx = -dx; rz = -dz;
        } else if (facing == BlockFace.EAST) {
            rx = dz;  rz = -dx;
        } else if (facing == BlockFace.WEST) {
            rx = -dz; rz = dx;
        }
        return new Location(world, base.getBlockX() + rx, base.getBlockY() + dy, base.getBlockZ() + rz);
    }

    // ----------------------------------------------------
    // 진행
    // ----------------------------------------------------
    public void beginStage(Player trigger) {
        if (stages.isEmpty()) {
            // 기본 스테이지 제공
            List<Stage> def = new ArrayList<Stage>();
            def.add(new Stage("Stage 1", 126, 0.40));
            def.add(new Stage("Stage 2", 223, 0.30));
            setStages(def);
        }

        if (state != State.IDLE) {
            return;
        }
        this.stageIndex = 0;
        this.state = State.IN_STAGE;
        this.drawTickets = 0;
        this.payoutRemaining = 0;
        this.lastActor = trigger != null ? trigger.getUniqueId() : null;

        String msg = plugin.getConfig().getString("messages.broadcastEnter", "")
                .replace("%player%", trigger != null ? trigger.getName() : "SYSTEM")
                .replace("%stage%", stages.get(stageIndex).getName())
                .replace("%id%", String.valueOf(id));
        Bukkit.broadcastMessage(Text.prefix(plugin) + msg);

        if (plugin.getConfig().getBoolean("sounds.bgm.enabled", true)) {
            if (!bgmPlaying) {
                bgmPlaying = true;
                playBgm();
            }
        }
    }

    private void playBgm() {
        String s = plugin.getConfig().getString("sounds.bgm.sound", "MUSIC_DISC_CHIRP");
        float v = (float) plugin.getConfig().getDouble("sounds.bgm.volume", 1.0);
        float p = (float) plugin.getConfig().getDouble("sounds.bgm.pitch", 1.0);
        for (Player pl : Bukkit.getOnlinePlayers()) {
            pl.playSound(base, Sound.valueOf(s), v, p);
        }
    }

    public void onGoldClicked(Player p) {
        if (state != State.IN_STAGE) {
            p.sendMessage(Text.prefix(plugin) + "지금은 스테이지가 아닙니다.");
            return;
        }
        int max = plugin.getConfig().getInt("chance.maxTickets", 5);
        if (drawTickets >= max) {
            p.sendMessage(Text.prefix(plugin) + "추첨권이 이미 최대치입니다.");
            return;
        }
        // 구슬 1개 소모
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!ItemUtil.consumeOneBall(hand)) {
            p.sendMessage(Text.prefix(plugin) + "구슬이 부족합니다.");
            return;
        }
        drawTickets++;
        this.lastActor = p.getUniqueId();

        String msg = plugin.getConfig().getString("messages.ticketAdded", "")
                .replace("%now%", String.valueOf(drawTickets))
                .replace("%max%", String.valueOf(max));
        p.sendMessage(Text.prefix(plugin) + msg);

        // 1개씩만 올라가는 애니메이션
        raiseBallVisual();
    }

    public void onCoalClicked(Player p) {
        if (state != State.IN_STAGE) return;
        if (drawTickets <= 0) {
            p.sendMessage(Text.prefix(plugin) + "추첨권이 없습니다.");
            return;
        }
        int toUse = drawTickets;
        drawTickets = 0;
        String msg = plugin.getConfig().getString("messages.drawStart", "")
                .replace("%count%", String.valueOf(toUse));
        p.sendMessage(Text.prefix(plugin) + msg);

        boolean advanced = false;
        for (int i = 0; i < toUse; i++) {
            if (tryAdvance()) {
                advanced = true;
                break;
            }
        }
        if (advanced) {
            stageIndex++;
            if (stageIndex >= stages.size()) {
                // 마지막 스테이지를 넘어가면 종료
                endStage(true);
            } else {
                // 다음 스테이지로
                p.sendMessage(Text.prefix(plugin) + plugin.getConfig().getString("messages.drawWin", ""));
                String msg2 = plugin.getConfig().getString("messages.enterStage", "")
                        .replace("%stage%", stages.get(stageIndex).getName());
                p.sendMessage(Text.prefix(plugin) + msg2);
                // 계속 IN_STAGE
            }
        } else {
            // 실패 -> 배출 단계
            p.sendMessage(Text.prefix(plugin) + plugin.getConfig().getString("messages.drawFail", ""));
            startPayout();
        }
    }

    private boolean tryAdvance() {
        Stage st = stages.get(stageIndex);
        double chance = st.getAdvanceChance();
        double r = Math.random();
        return r < chance;
    }

    private void startPayout() {
        Stage st = stages.get(stageIndex);
        this.payoutRemaining = st.getCup();
        this.state = State.PAYOUT;
        this.drawTickets = 0;
        // 진행 타이틀 표기 및 실제 아이템 배출
        new BukkitRunnable() {
            @Override
            public void run() {
                if (state != State.PAYOUT) {
                    cancel();
                    return;
                }
                // 1개 배출
                payoutOne();
                // 타이틀 갱신
                for (Player pl : Bukkit.getOnlinePlayers()) {
                    String title = ChatColor.YELLOW + "배출 " + ChatColor.WHITE + (st.getCup() - payoutRemaining) + ChatColor.GRAY + " / " + ChatColor.WHITE + st.getCup();
                    String sub = "";
                    pl.sendTitle(title, sub, 0, 10, 10);
                }
                if (payoutRemaining <= 0) {
                    cancel();
                    endStage(false);
                }
            }
        }.runTaskTimer(plugin, 0L, 4L); // 0.2초마다 1개
    }

    private void payoutOne() {
        if (payoutRemaining <= 0) return;
        payoutRemaining--;

        // 다이아에서 구슬 아이템 떨어뜨리기 (픽업 지연)
        ItemStack ball = ItemUtil.newBall(1, null);
        Location dropLoc = locDiamond.clone().add(0.5, 1.2, 0.5);
        Item item = world.dropItem(dropLoc, ball);
        item.setPickupDelay(20*3);
        item.setVelocity(new Vector(0, 0.2, 0));
        world.spawnParticle(Particle.CRIT, dropLoc, 6, 0.25, 0.25, 0.25, 0.01);
        world.playSound(dropLoc, Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.6f);
    }

    private void endStage(boolean jackpot) {
        Stage st = stages.get(stageIndex);
        String msg = plugin.getConfig().getString("messages.broadcastEnd", "")
                .replace("%cup%", String.valueOf(st.getCup()))
                .replace("%id%", String.valueOf(id))
                .replace("%player%", lastActor != null ? Bukkit.getOfflinePlayer(lastActor).getName() : "SYSTEM");
        Bukkit.broadcastMessage(Text.prefix(plugin) + msg);

        this.state = State.IDLE;
        this.stageIndex = -1;
        this.drawTickets = 0;
        this.payoutRemaining = 0;
        this.animating = false;
        this.bgmPlaying = false; // 다음 시작에 다시 1회 재생 가능
    }

    private void raiseBallVisual() {
        if (animating) return;
        animating = true;

        final int steps = Math.max(1, plugin.getConfig().getInt("raise.steps", 12));
        final int tick = Math.max(1, plugin.getConfig().getInt("raise.ticksPerStep", 2));
        final Location start = locGold.clone().add(0.5, 1.0, 0.5);
        final Location end = base.clone().add(0.5, 5.2, 0.5); // 대략 상단으로

        ItemStack demo = ItemUtil.newBall(1, null);
        final Item item = world.dropItem(start, demo);
        item.setPickupDelay(20*10);
        item.setGravity(false);
        item.setInvulnerable(true);

        new BukkitRunnable() {
            int i = 0;
            @Override
            public void run() {
                if (item.isDead()) {
                    animating = false;
                    cancel();
                    return;
                }
                double t = (double) i / (double) steps;
                double nx = start.getX() + (end.getX() - start.getX()) * t;
                double ny = start.getY() + (end.getY() - start.getY()) * t;
                double nz = start.getZ() + (end.getZ() - start.getZ()) * t;
                item.teleport(new Location(world, nx, ny, nz));
                world.spawnParticle(Particle.END_ROD, item.getLocation(), 2, 0.02, 0.02, 0.02, 0.0);
                if (i >= steps) {
                    item.remove();
                    animating = false;
                    cancel();
                }
                i++;
            }
        }.runTaskTimer(plugin, 0L, tick);
    }


    public boolean isSpecialBlock(org.bukkit.Location l) {
        if (l == null) return false;
        if (l.getWorld() != world) return false;
        if (locGold == null || locDiamond == null || locCoal == null) return false;
        if (l.getBlock().equals(locGold.getBlock())) return true;
        if (l.getBlock().equals(locDiamond.getBlock())) return true;
        if (l.getBlock().equals(locCoal.getBlock())) return true;
        return false;
    }

    public boolean isGoldBlock(org.bukkit.Location l) {
        if (!isSpecialBlock(l)) return false;
        return l.getBlock().equals(locGold.getBlock());
    }

    public boolean isCoalBlock(org.bukkit.Location l) {
        if (!isSpecialBlock(l)) return false;
        return l.getBlock().equals(locCoal.getBlock());
    }

    public boolean isDiamondBlock(org.bukkit.Location l) {
        if (!isSpecialBlock(l)) return false;
        return l.getBlock().equals(locDiamond.getBlock());
    }
    // ----------------------------------------------------
    // 저장/로드
    // ----------------------------------------------------
    public void save(ConfigurationSection sec) {
        sec.set("world", world.getName());
        sec.set("base.x", base.getBlockX());
        sec.set("base.y", base.getBlockY());
        sec.set("base.z", base.getBlockZ());
        sec.set("facing", facing.name());
        if (ballMaterial != null) sec.set("ball.material", ballMaterial.name());
        if (ballName != null) sec.set("ball.name", ballName);
        if (ballLore != null) sec.set("ball.lore", ballLore);
        sec.set("ball.lock", lockBallToMachine);

        List<Map<String, Object>> st = new ArrayList<Map<String, Object>>();
        for (Stage s : stages) {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("name", s.getName());
            m.put("cup", s.getCup());
            m.put("advanceChance", s.getAdvanceChance());
            st.add(m);
        }
        sec.set("stages.list", st);
    }

    public static Machine load(Main plugin, int id, ConfigurationSection sec) {
        String world = sec.getString("world", plugin.getConfig().getString("worldDefault", "bskyblock_world"));
        World w = Bukkit.getWorld(world);
        if (w == null) w = Bukkit.getWorlds().get(0);
        int x = sec.getInt("base.x");
        int y = sec.getInt("base.y");
        int z = sec.getInt("base.z");
        String f = sec.getString("facing", "SOUTH");
        Machine m = new Machine(plugin, id, w, new Location(w, x, y, z), BlockFace.valueOf(f));

        List<Stage> list = new ArrayList<Stage>();
        List<Map<?, ?>> raw = sec.getMapList("stages.list");
        if (raw != null && !raw.isEmpty()) {
            for (Map<?, ?> map : raw) {
                String name = String.valueOf(map.getOrDefault("name", "Stage"));
                int cup = Integer.parseInt(String.valueOf(map.getOrDefault("cup", 100)));
                double chance = Double.parseDouble(String.valueOf(map.getOrDefault("advanceChance", 0.3)));
                list.add(new Stage(name, cup, chance));
            }
        }
        m.setStages(list);
        String bm = sec.getString("ball.material", null);
        if (bm != null) { try { m.ballMaterial = Material.valueOf(bm); } catch (Exception ignored) {} }
        m.ballName = sec.getString("ball.name", null);
        java.util.List<String> bl = new java.util.ArrayList<String>();
        if (sec.isList("ball.lore")) bl = sec.getStringList("ball.lore");
        m.ballLore = bl;
        m.lockBallToMachine = sec.getBoolean("ball.lock", false);
        m.buildStructure(); // 보정 설치(누락 복구 목적)

        // 특수 위치 계산
        m.locGold = m.relative(0, 0, 0);
        m.locDiamond = m.relative(-1, 0, 0);
        m.locCoal = m.relative(1, 0, 0);

        return m;
    }
}
