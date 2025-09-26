
package com.minkang.ultimate.pachinko.model;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.util.ItemUtil;
import com.minkang.ultimate.pachinko.util.Text;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class Machine {

    public enum State { IDLE, PRE_ENTRY, IN_STAGE, PAYOUT }

    private final Main plugin;
    private final int id;
    private final World world;
    private final Location base;
    private final BlockFace facing;

    private List<Stage> stages = new ArrayList<Stage>();
    private int stageIndex = -1;
    private State state = State.IDLE;

    private int drawTickets = 0;
    private int payoutRemaining = 0;
    private boolean animating = false;
    private boolean bgmPlaying = false;
    private UUID lastActor = null;

    private Material ballMaterial = null;
    private String ballName = null;
    private java.util.List<String> ballLore = null;
    private boolean lockBallToMachine = false;

    Location locDiamond;
    Location locGold;
    Location locCoal;

    public Machine(Main plugin, int id, World world, Location base, BlockFace facing) {
        this.plugin = plugin;
        this.id = id;
        this.world = world;
        this.base = base.getBlock().getLocation();
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
public void setBallTemplate(Material mat, String name, java.util.List<String> lore) {
        this.ballMaterial = mat;
        this.ballName = name;
        this.ballLore = lore;
    }

    public boolean acceptsBall(ItemStack item) {
        if (item == null) return false;
        if (!ItemUtil.isBall(item)) return false;
        Integer mid = ItemUtil.getMachineId(item);
        if (lockBallToMachine) {
            if (mid == null || mid.intValue() != this.id) return false;
        }
        if (ballMaterial != null && item.getType() != ballMaterial) return false;
        if (ballName != null || (ballLore != null && !ballLore.isEmpty())) {
            if (!item.hasItemMeta()) return false;
            org.bukkit.inventory.meta.ItemMeta im = item.getItemMeta();
            if (ballName != null) {
                if (!im.hasDisplayName()) return false;
                if (!ballName.equals(im.getDisplayName())) return false;
            }
            if (ballLore != null) {
                java.util.List<String> l = im.hasLore() ? im.getLore() : null;
                if (l == null) return false;
                if (!ballLore.equals(l)) return false;
            }
        }
        return true;
    }

    public void setStages(List<Stage> list) { this.stages = list; }
// ---------- build/remove structure ----------
    
    public void buildStructure() {
        // 1층: 다이아(좌) 금(중앙) 석탄(우)
        Location gold = relative(0, 0, 0);
        Location dia  = relative(-1, 0, 0);
        Location coal = relative(1, 0, 0);
        setBlock(dia,  Material.DIAMOND_BLOCK);
        setBlock(gold, Material.GOLD_BLOCK);
        setBlock(coal, Material.COAL_BLOCK);
        this.locDiamond = dia;
        this.locGold = gold;
        this.locCoal = coal;

        // 2~8층: 유리/철창 교차 (가로 7칸 -3..3), y=1..7
        for (int y = 1; y <= 7; y++) {
            Material mat = (y % 2 == 1) ? Material.GLASS : Material.IRON_BARS;
            for (int x = -3; x <= 3; x++) setBlock(relative(x, y, 0), mat);
        }
        // 9층: 호퍼(7칸, 아래방향), y=8
        for (int x = -3; x <= 3; x++) {
            Location hl = relative(x, 8, 0);
            Block b = world.getBlockAt(hl);
            b.setType(Material.HOPPER, false);
            org.bukkit.block.data.type.Hopper data = (org.bukkit.block.data.type.Hopper) b.getBlockData();
            data.setFacing(BlockFace.DOWN);
            b.setBlockData(data, false);
        }
    }


    
    public void removeStructure() {
        for (int x = -3; x <= 3; x++) setBlock(relative(x, 8, 0), Material.AIR);
        for (int y = 1; y <= 7; y++) for (int x = -3; x <= 3; x++) setBlock(relative(x, y, 0), Material.AIR);
        setBlock(relative(-1, 0, 0), Material.AIR);
        setBlock(relative(0, 0, 0), Material.AIR);
        setBlock(relative(1, 0, 0), Material.AIR);
    }


    private void setBlock(Location l, Material mat) {
        world.getBlockAt(l).setType(mat, false);
    }

    private Location relative(int dx, int dy, int dz) {
        int rx = dx, rz = dz;
        if (facing == BlockFace.SOUTH) { rx = dx; rz = dz; }
        else if (facing == BlockFace.NORTH) { rx = -dx; rz = -dz; }
        else if (facing == BlockFace.EAST)  { rx = dz; rz = -dx; }
        else if (facing == BlockFace.WEST)  { rx = -dz; rz = dx; }
        return new Location(world, base.getBlockX() + rx, base.getBlockY() + dy, base.getBlockZ() + rz);
    }

    // ---------- gameplay ----------
    public void beginStage(Player trigger) {
        if (stages.isEmpty()) {
            List<Stage> def = new ArrayList<Stage>();
            def.add(new Stage("Stage 1", 126, 0.20, 0.40));
            def.add(new Stage("Stage 2", 223, 0.20, 0.30));
            setStages(def);
        }
        if (state != State.IDLE) return;

        this.stageIndex = 0;
        this.state = State.PRE_ENTRY;
        this.drawTickets = 0;
        this.payoutRemaining = 0;
        this.lastActor = trigger != null ? trigger.getUniqueId() : null;

        String msg = Main.inst().getConfig().getString("messages.entryReady", "");
        if (trigger != null && msg != null && !msg.isEmpty()) trigger.sendMessage(Text.prefix(plugin) + msg);

        if (plugin.getConfig().getBoolean("sounds.bgm.enabled", true)) {
            if (!bgmPlaying) { bgmPlaying = true; playBgm(); }
        }
    }

    private void playBgm() {
        String s = plugin.getConfig().getString("sounds.bgm.sound", "MUSIC_DISC_CHIRP");
        float v = (float) plugin.getConfig().getDouble("sounds.bgm.volume", 1.0);
        float p = (float) plugin.getConfig().getDouble("sounds.bgm.pitch", 1.0);
        for (Player pl : Bukkit.getOnlinePlayers()) pl.playSound(base, Sound.valueOf(s), v, p);
    }

    public void onGoldClicked(Player p) {
        if (state == State.PRE_ENTRY) {
            int max = plugin.getConfig().getInt("chance.maxTickets", 5);
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (!acceptsBall(hand)) {
                p.sendMessage(Text.prefix(plugin) + "구슬을 손에 들어주세요.");
                return;
            }
            if (!ItemUtil.consumeOneBall(hand)) {
                p.sendMessage(Text.prefix(plugin) + "구슬이 부족합니다.");
                return;
            }
            if (drawTickets >= max) {
                p.sendMessage(Text.prefix(plugin) + "추첨권이 이미 최대치입니다.");
                return;
            }
            drawTickets++;
            this.lastActor = p.getUniqueId();
            String msg = plugin.getConfig().getString("messages.ticketAdded", "")
                    .replace("%now%", String.valueOf(drawTickets)).replace("%max%", String.valueOf(max));
            p.sendMessage(Text.prefix(plugin) + msg);
            raiseBallVisual();
            return;
        }
        if (state == State.IN_STAGE) {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (!acceptsBall(hand)) { p.sendMessage(Text.prefix(plugin) + "구슬을 손에 들어주세요."); return; }
            if (!ItemUtil.consumeOneBall(hand)) { p.sendMessage(Text.prefix(plugin) + "구슬이 부족합니다."); return; }
            this.lastActor = p.getUniqueId();
            // 애니메이션 후 중앙이면 3자리 추첨, 아니면 '꽝'만 안내
            raiseBallAndThen(p, true, new Runnable(){ public void run() {
                doThreeDigitDraw(false, p,
                    new Runnable(){ public void run(){ // 성공 → 상승
                        stageIndex++;
                        String msg = plugin.getConfig().getString("messages.advanceWin", "다음 스테이지로 상승");
                        if (p != null) p.sendMessage(Text.prefix(plugin) + msg);
                        if (stageIndex >= stages.size()) { endStage(true); }
                        else {
                            String msg2 = plugin.getConfig().getString("messages.enterStage", "")
                                    .replace("%stage%", stages.get(stageIndex).getName());
                            if (p != null && msg2 != null && !msg2.isEmpty()) p.sendMessage(Text.prefix(plugin) + msg2);
                        }
                    }},
                    new Runnable(){ public void run(){ // 실패 → 배출
                        String msg = plugin.getConfig().getString("messages.advanceFail", "불일치! 배출 단계로 전환됩니다.");
                        if (p != null) p.sendMessage(Text.prefix(plugin) + msg);
                        startPayout();
                    }}
                );
            }});
            return;
        }
    }

    public void onCoalClicked(Player p) {
        if (state != State.PRE_ENTRY) return;
        int max = plugin.getConfig().getInt("chance.maxTickets", 5);
        if (drawTickets <= 0) {
            p.sendMessage(Text.prefix(plugin) + "추첨권이 없습니다.");
            return;
        }
        if (drawTickets >= max) {
            final int attempts = drawTickets; drawTickets = 0;
            runEntryDrawsSequential(p, attempts);
        } else {
            drawTickets -= 1;
            doThreeDigitDraw(true, p,
                new Runnable(){ public void run(){
                    state = State.IN_STAGE;
                    String msg = plugin.getConfig().getString("messages.entryWin", "스테이지 진입");
                    if (p != null) p.sendMessage(Text.prefix(plugin) + msg);
                    String bc = plugin.getConfig().getString("messages.broadcastEnter", "")
                            .replace("%player%", p != null ? p.getName() : "SYSTEM")
                            .replace("%stage%", stages.get(stageIndex).getName())
                            .replace("%id%", String.valueOf(id));
                    if (bc != null && !bc.isEmpty()) Bukkit.broadcastMessage(Text.prefix(plugin) + bc);
                }},
                new Runnable(){ public void run(){
                    String msg = plugin.getConfig().getString("messages.entryFail", "불일치! 다시 모아서 시도하세요.");
                    if (p != null) p.sendMessage(Text.prefix(plugin) + msg);
                }}
            );
        }
    }

    private void runEntryDrawsSequential(final Player actor, final int attemptsRemaining) {
        if (attemptsRemaining <= 0 || state != State.PRE_ENTRY) return;
        doThreeDigitDraw(true, actor,
            new Runnable(){ public void run(){
                state = State.IN_STAGE;
                String msg = plugin.getConfig().getString("messages.entryWin", "스테이지 진입");
                if (actor != null) actor.sendMessage(Text.prefix(plugin) + msg);
                String bc = plugin.getConfig().getString("messages.broadcastEnter", "")
                        .replace("%player%", actor != null ? actor.getName() : "SYSTEM")
                        .replace("%stage%", stages.get(stageIndex).getName())
                        .replace("%id%", String.valueOf(id));
                if (bc != null && !bc.isEmpty()) Bukkit.broadcastMessage(Text.prefix(plugin) + bc);
            }},
            new Runnable(){ public void run(){
                int left = attemptsRemaining - 1;
                if (left > 0) runEntryDrawsSequential(actor, left);
                else {
                    String msg = plugin.getConfig().getString("messages.entryFail", "불일치! 다시 모아서 시도하세요.");
                    if (actor != null) actor.sendMessage(Text.prefix(plugin) + msg);
                }
            }}
        );
    }

    private void doThreeDigitDraw(final boolean isEntry, final Player actor, final Runnable onSuccess, final Runnable onFail) {
        final int steps = Math.max(1, plugin.getConfig().getInt("draw.spinSteps", 10));
        final int tick = Math.max(1, plugin.getConfig().getInt("draw.spinTick", 2));
        final Stage st = stages.get(stageIndex);
        final double chance = isEntry ? st.getEntryChance() : st.getAdvanceChance();

        new BukkitRunnable() {
            int i = 0;
            @Override
            public void run() {
                if (i < steps) {
                    int a = (int)(Math.random()*10);
                    int b = (int)(Math.random()*10);
                    int c = (int)(Math.random()*10);
                    String title = ChatColor.GOLD + "" + a + " " + b + " " + c;
                    for (Player pl : Bukkit.getOnlinePlayers()) pl.sendTitle(title, "", 0, 5, 5);
                    i++;
                    return;
                }
                boolean success = Math.random() < chance;
                int x, a, b, c;
                if (success) { x = (int)(Math.random()*10); a = x; b = x; c = x; }
                else {
                    a = (int)(Math.random()*10); b = (int)(Math.random()*10); c = (int)(Math.random()*10);
                    if (a == b && b == c) c = (c + 1) % 10;
                }
                String title = (success ? ChatColor.GREEN : ChatColor.RED) + ("" + a + " " + b + " " + c);
                for (Player pl : Bukkit.getOnlinePlayers()) pl.sendTitle(title, "", 0, 20, 10);

                if (success) { if (onSuccess != null) onSuccess.run(); }
                else { if (onFail != null) onFail.run(); }
                cancel();
            }
        }.runTaskTimer(plugin, 0L, tick);
    }

    private void startPayout() {
        final Stage st = stages.get(stageIndex);
        this.payoutRemaining = st.getCup();
        this.state = State.PAYOUT;
        this.drawTickets = 0;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (state != State.PAYOUT) { cancel(); return; }
                payoutOne();
                for (Player pl : Bukkit.getOnlinePlayers()) {
                    String title = ChatColor.YELLOW + "배출 " + ChatColor.WHITE + (st.getCup() - payoutRemaining) + ChatColor.GRAY + " / " + ChatColor.WHITE + st.getCup();
                    pl.sendTitle(title, "", 0, 10, 10);
                }
                if (payoutRemaining <= 0) { cancel(); endStage(false); }
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    private void payoutOne() {
        if (payoutRemaining <= 0) return;
        payoutRemaining--;

        ItemStack ball = com.minkang.ultimate.pachinko.util.ItemUtil.newBallForMachine(this, 1, this.id);
        Location dropLoc = locDiamond.clone().add(0.5, 1.2, 0.5);
        Item item = world.dropItem(dropLoc, ball);
        item.setPickupDelay(20 * 3);
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
        if (msg != null && !msg.isEmpty()) Bukkit.broadcastMessage(com.minkang.ultimate.pachinko.util.Text.prefix(plugin) + msg);

        this.state = State.IDLE;
        this.stageIndex = -1;
        this.drawTickets = 0;
        this.payoutRemaining = 0;
        this.animating = false;
        this.bgmPlaying = false;
    }

    private void raiseBallVisual() {
        if (animating) return;
        animating = true;

        final int steps = Math.max(1, plugin.getConfig().getInt("raise.steps", 20));
        final int tick = Math.max(1, plugin.getConfig().getInt("raise.ticksPerStep", 2));

        final int destX = (int) Math.floor(Math.random() * 7) - 3; // -3..3 (7 lanes)
        final boolean isCenter = (destX == 0);

        final Location start = locGold.clone().add(0.5, 1.0, 0.5);
        final Location end = relative(destX, 8, 0).add(0.5, 1.0, 0.5);

        ItemStack demo = com.minkang.ultimate.pachinko.util.ItemUtil.newBallForMachine(this, 1, this.id);
        final org.bukkit.entity.Item item = world.dropItem(start, demo);
        item.setPickupDelay(20 * 10);
        item.setGravity(false);
        item.setInvulnerable(true);

        new BukkitRunnable() {
            int i = 0;
            @Override
            public void run() {
                if (item.isDead()) { animating = false; cancel(); return; }
                double t = (double) i / (double) steps;
                double nx = start.getX() + (end.getX() - start.getX()) * t;
                double ny = start.getY() + (end.getY() - start.getY()) * t;
                double nz = start.getZ() + (end.getZ() - start.getZ()) * t;
                item.teleport(new Location(world, nx, ny, nz));
                world.spawnParticle(Particle.END_ROD, item.getLocation(), 2, 0.02, 0.02, 0.02, 0.0);
                if (i >= steps) {
                    item.remove();
                    animating = false;
                    String msg = plugin.getConfig().getString(isCenter ? "messages.centerHit" : "messages.centerMiss", isCenter ? "중앙!" : "꽝!");
                    if (lastActor != null) {
                        org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(lastActor);
                        if (op != null && op.isOnline()) ((Player)op).sendMessage(com.minkang.ultimate.pachinko.util.Text.prefix(plugin) + msg);
                    }
                    cancel();
                }
                i++;
            }
        }.runTaskTimer(plugin, 0L, tick);
    }

    public boolean isSpecialBlock(Location l) {
        if (l == null) return false;
        if (l.getWorld() != world) return false;
        if (locGold == null || locDiamond == null || locCoal == null) return false;
        if (l.getBlock().getLocation().equals(locGold.getBlock().getLocation())) return true;
        if (l.getBlock().getLocation().equals(locDiamond.getBlock().getLocation())) return true;
        if (l.getBlock().getLocation().equals(locCoal.getBlock().getLocation())) return true;
        return false;
    }

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

        java.util.List<java.util.Map<String, Object>> st = new java.util.ArrayList<java.util.Map<String, Object>>();
        for (Stage s : stages) {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<String, Object>();
            m.put("name", s.getName());
            m.put("cup", s.getCup());
            m.put("entryChance", s.getEntryChance());
            m.put("advanceChance", s.getAdvanceChance());
            st.add(m);
        }
        sec.set("stages.list", st);
    }

    public static Machine load(Main plugin, int id, ConfigurationSection sec) {
        String world = sec.getString("world", plugin.getConfig().getString("worldDefault", ""));
        org.bukkit.World w = world != null && !world.isEmpty() ? Bukkit.getWorld(world) : null;
        if (w == null) w = Bukkit.getWorlds().get(0);
        int x = sec.getInt("base.x");
        int y = sec.getInt("base.y");
        int z = sec.getInt("base.z");
        String f = sec.getString("facing", "SOUTH");
        Machine m = new Machine(plugin, id, w, new Location(w, x, y, z), BlockFace.valueOf(f));

        java.util.List<Stage> list = new java.util.ArrayList<Stage>();
        java.util.List<java.util.Map<?, ?>> raw = sec.getMapList("stages.list");
        if (raw != null && !raw.isEmpty()) {
            for (java.util.Map<?, ?> map : raw) {
                Object vName = map.get("name");
                String name = vName != null ? String.valueOf(vName) : "Stage";
                Object vCup = map.get("cup");
                int cup = vCup != null ? Integer.parseInt(String.valueOf(vCup)) : 100;
                Object vEntry = map.get("entryChance");
                double entryChance = vEntry != null ? Double.parseDouble(String.valueOf(vEntry)) : 0.2;
                Object vChance = map.get("advanceChance");
                double chance = vChance != null ? Double.parseDouble(String.valueOf(vChance)) : 0.3;
                list.add(new Stage(name, cup, entryChance, chance));
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

        m.buildStructure();
        m.locGold = m.relative(0, 0, 0);
        m.locDiamond = m.relative(-1, 0, 0);
        m.locCoal = m.relative(1, 0, 0);
        return m;
    }
}
