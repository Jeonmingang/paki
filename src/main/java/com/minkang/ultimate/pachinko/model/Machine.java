package com.minkang.ultimate.pachinko.model;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.util.Easing;
import com.minkang.ultimate.pachinko.util.ItemUtil;
import com.minkang.ultimate.pachinko.util.Text;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Machine {
    private final Main plugin;
    private final int id;
    private final String worldName;
    private final Location base; // 중심 기준 좌표(기계 바닥 중앙)
    private final int slots; // 7 (0..6), center = 3
    private final int centerIndex;
    private final AtomicBoolean drawingNow = new AtomicBoolean(false);

    // stage
    private int stage = 0;
    private int stagePayout = 0;
    private int stageCup = 0;

    // operator lock
    private UUID operator;

    // per-machine ball spec (optional)
    private String ballType; // Material name
    private String ballName; // colored string
    private List<String> ballLore; // colored

    public Machine(Main plugin, int id, String worldName, Location base, int slots){
        this.plugin = plugin;
        this.id = id;
        this.worldName = worldName;
        this.base = base;
        this.slots = slots;
        this.centerIndex = slots/2;
        this.stageCup = getStageCup(0);
    }

    public int getId(){ return id; }
    public Location getBase(){ return base; }
    public boolean isDrawing(){ return drawingNow.get(); }
    public int getStage(){ return stage; }
    public int getStageCup(){ return stageCup; }
    public int getStagePayout(){ return stagePayout; }
    public void setStagePayout(int v){ stagePayout = v; }
    public UUID getOperator(){ return operator; }
    public void setOperator(UUID u){ operator = u; }
    public Player getOperatorPlayer(){
        if (operator == null) return null;
        return Bukkit.getPlayer(operator);
    }

    public void setBallSpec(String type, String name, List<String> lore){
        this.ballType = type;
        this.ballName = name;
        this.ballLore = lore == null ? null : new ArrayList<>(lore);
    }
    public boolean hasBallSpec(){
        return ballType != null && !ballType.isEmpty();
    }
    public Map<String,Object> serializeBallSpec(){
        if (!hasBallSpec()) return null;
        Map<String,Object> m = new HashMap<>();
        m.put("type", ballType);
        if (ballName != null) m.put("name", ballName);
        if (ballLore != null) m.put("lore", new ArrayList<>(ballLore));
        return m;
    }

    // ---- gameplay ----
    public boolean tryInsertBall(Player p, ItemStack hand){
        // check lock
        if (operator != null && !operator.equals(p.getUniqueId())){
            Text.msg(p, "&c현재 기계는 &e"+getOperatorPlayerName()+" &c전용입니다.");
            return false;
        }
        // validate ball (exact match: material+name+lore)
        if (!ItemUtil.isValidBall(plugin, this, hand)){
            Text.msg(p, "&7이 기계 전용 구슬이 아닙니다.");
            return false;
        }
        // consume 1
        if (hand.getAmount() <= 0){
            Text.msg(p, "&7구슬 수량이 없습니다.");
            return false;
        }
        hand.setAmount(hand.getAmount() - 1);

        // lock operator if not set
        if (operator == null){
            operator = p.getUniqueId();
        }

        // entry: rise animation then centralChance → spin numbers → maybe stage1
        animateRiseToHopper(p, () -> {
            boolean centralOk = Math.random() < getEntryCentralChance();
            if (centralOk){
                spinNumbersAndMaybeEnterStage(p);
            }else{
                // 중앙 실패: 좌/우 1칸 안내
                int off = (Math.random() < 0.5 ? -1 : 1);
                int to = centerIndex + off;
                String lr = off < 0 ? "왼쪽" : "오른쪽";
                String msg = "&7[꽝] &f#"+to+" &7("+lr+" 1칸)";
                Text.msg(p, msg);
                // 가벼운 좌/우 한칸 연출
                animateOneStep(centerIndex, to, () -> {});
            }
        });
        return true;
    }
public void startStage(Player actor){
        this.stage = Math.max(1, this.stage);
        this.stageCup = getStageCup(stage);
        this.stagePayout = 0;
        // BGM stop & play
        String bgm = getStageBgm(stage);
        BgmController.stopAll();
        if (bgm != null && !bgm.isEmpty()) BgmController.playAll(bgm, 0.8f, 1.0f);
        Text.msg(actor, "&a스테이지 &e"+stage+" &a시작!");
        Main.get().ranks().addStageUp(actor, stage);
        Bukkit.broadcastMessage(Text.color("&6[파칭코] &e"+actor.getName()+" &f님이 &a스테이지 &e"+stage+" &f진입! &7(기계 #"+id+")"));
    }

    public void endStage(Player actor, String reason){
        // stop BGM
        BgmController.stopAll();
        if (actor != null) Text.msg(actor, "&c스테이지 종료 &7- "+reason);
        if (stage >= getMaxStage()){
            // clear
            if (actor != null){
                Bukkit.broadcastMessage(Text.color("&6[파칭코] &e"+actor.getName()+"&f 님이 &b모든 스테이지 클리어! &7(기계 #"+id+")"));
                Main.get().ranks().addClear(actor);
            }
        }
        this.stage = 0;
        this.stagePayout = 0;
        this.stageCup = getStageCup(0);
        // release operator lock after game ends
        this.operator = null;
    }

    public boolean tryAdvanceStage(Player actor){
        if (drawingNow.get()) return false;
        if (stage <= 0) return false;
        double chance = getAdvanceChance(stage);
        boolean success = Math.random() < chance;
        final int from = centerIndex; // conceptual center draw
        if (success){
            int next = stage + 1;
            if (next > getMaxStage()){
                endStage(actor, "최종 클리어");
                return true;
            }
            this.stage = next;
            this.stageCup = getStageCup(stage);
            this.stagePayout = 0;
            String bgm = getStageBgm(stage);
            BgmController.stopAll();
            if (bgm != null && !bgm.isEmpty()) BgmController.playAll(bgm, 0.8f, 1.0f);
            Bukkit.broadcastMessage(Text.color("&a[스테이지 상승] &f"+actor.getName()+" &7→ &e"+stage+" &7단계 &8(#"+id+")"));
            return true;
        }else{
            // "중앙 실패 시 채팅 알림" with slot index and L/R
            int off = (Math.random() < 0.5 ? -1 : 1);
            int to = centerIndex + off;
            String lr = off < 0 ? "왼쪽" : "오른쪽";
            String msg = "&7[꽝] &f#"+to+" &7("+lr+" 1칸)";
            Text.msg(actor, msg);
            // Visualize one marble step with easing
            animateOneStep(from, to, () -> {});
            return false;
        }
    }

    public void payOut(Player actor){
        // restrict to operator
        if (operator != null && !operator.equals(actor.getUniqueId())){
            Text.msg(actor, "&c현재 기계는 &e"+getOperatorPlayerName()+" &c전용입니다.");
            return;
        }
        // drop 1 ball at diamond block (exit)
        Location exit = findExitDiamond();
        if (exit == null) exit = base.clone().add(0.5, 1.2, 1.5);
        ItemStack ball = ItemUtil.getConfiguredBall(plugin, this);
        try {
            Item it = exit.getWorld().dropItem(exit.clone().add(0.5,0.8,0.5), ball);
            it.setVelocity(new Vector(0, -0.2, 0));
            try { it.setPickupDelay(5); } catch (Throwable ignored){}
            for (int i=0;i<15;i++){
                exit.getWorld().spawnParticle(Particle.CRIT, exit.clone().add(0.5,1.0,0.5), 3, 0.2, 0.2, 0.2, 0.01);
            }
            exit.getWorld().playSound(exit, Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.2f);
        }catch(Throwable t){
            // best-effort
        }
        stagePayout++;
        Text.msg(actor, "&b배출 &f"+stagePayout+" &7/&e"+stageCup);
        if (stagePayout >= stageCup){
            endStage(actor, "cup 도달");
        }
    }

    private Location findExitDiamond(){
        World w = base.getWorld();
        if (w == null) return null;
        int r = 4;
        for (int dx=-r; dx<=r; dx++){
            for (int dy=-r; dy<=r; dy++){
                for (int dz=-r; dz<=r; dz++){
                    Location l = base.clone().add(dx, dy, dz);
                    try {
                        if (l.getBlock().getType() == Material.DIAMOND_BLOCK){
                            return l.clone();
                        }
                    }catch(Throwable ignored){}
                }
            }
        }
        return null;
    }

    private void animateOneStep(int fromSlot, int toSlot, Runnable onFinish){
        if (drawingNow.getAndSet(true)) return;
        final Location start = base.clone().add(0.5, 1.2, 1.5);
        final double slotSize = 0.35; // spacing
        final double sx = (fromSlot - centerIndex) * slotSize;
        final double tx = (toSlot - centerIndex) * slotSize;
        final ItemStack ball = ItemUtil.getConfiguredBall(plugin, this);
        final Item item = start.getWorld().dropItem(start, ball);
        item.setPickupDelay(Integer.MAX_VALUE);
        item.setGravity(false);
        final int totalTicks = Math.max(8, getStepTicks()); // 0.6s
        new BukkitRunnable(){
            int t = 0;
            @Override public void run(){
                t++;
                double tt = com.minkang.ultimate.pachinko.util.Easing.clamp01(t/(double)totalTicks);
                double smooth = com.minkang.ultimate.pachinko.util.Easing.easeInOutQuad(tt);
                double x = sx + (tx - sx) * smooth;
                double y = 1.2 + (0.6 * (1 - (2*smooth-1)*(2*smooth-1))); // arc
                Location cur = base.clone().add(0.5 + x, y, 1.5);
                try { item.teleport(cur); } catch(Throwable ignored){}
                try { cur.getWorld().spawnParticle(Particle.CRIT, cur, 1); } catch(Throwable ignored){}
                if (t >= totalTicks){
                    try { item.remove(); } catch(Throwable ignored){}
                    drawingNow.set(false);
                    if (onFinish != null) onFinish.run();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    
    // --- Entry chances & visuals ---
    private double getEntryCentralChance(){
        org.bukkit.configuration.file.FileConfiguration c = plugin.getConfig();
        Object v = c.get("entry.centralChance");
        return parseChanceObject(v);
    }
    private double getEntryMatchChance(){
        org.bukkit.configuration.file.FileConfiguration c = plugin.getConfig();
        Object v = c.get("entry.matchChance");
        return parseChanceObject(v);
    }
    private int getRiseTicks(){
        return plugin.getConfig().getInt("visuals.entryRiseTicks", 20);
    }
    private int getStepTicks(){
        return plugin.getConfig().getInt("visuals.stepTicks", 12);
    }
    private double parseChanceObject(Object v){
        if (v == null) return 0;
        if (v instanceof Number) return ((Number)v).doubleValue();
        String s = String.valueOf(v);
        if (s.contains("/")){
            String[] sp = s.split("/");
            try{
                double a = Double.parseDouble(sp[0].trim());
                double b = Double.parseDouble(sp[1].trim());
                if (b <= 0) return 0;
                return a/b;
            }catch(Throwable ignored){}
        }
        try { return Double.parseDouble(s); }catch(Throwable ignored){}
        return 0;
    }

    // --- Config helpers ---
    public int getMaxStage(){
        List<Map<String,Object>> stages = getStages();
        return stages.size()-1; // index 0 is "(없음)"
    }

    public List<Map<String,Object>> getStages(){
        List<Map<String,Object>> list = (List<Map<String,Object>>) (List<?>) plugin.getConfig().getMapList("stages");
        return list == null ? Collections.<Map<String,Object>>emptyList() : list;
    }

    public int getStageCup(int idx){
        List<Map<String,Object>> stages = getStages();
        if (idx < 0 || idx >= stages.size()) return 0;
        Object v = stages.get(idx).get("cup");
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Throwable ignored) {}
        return 0;
    }

    public String getStageBgm(int idx){
        List<Map<String,Object>> stages = getStages();
        if (idx < 0 || idx >= stages.size()) return null;
        Object v = stages.get(idx).get("bgm");
        return v==null ? null : String.valueOf(v);
    }

    public double getAdvanceChance(int idx){
        List<Map<String,Object>> stages = getStages();
        if (idx < 0 || idx >= stages.size()) return 0;
        Object v = stages.get(idx).get("advanceChance");
        if (v instanceof Number) return ((Number) v).doubleValue();
        String s = String.valueOf(v);
        if (s.contains("/")){
            String[] sp = s.split("/");
            try{
                double a = Double.parseDouble(sp[0].trim());
                double b = Double.parseDouble(sp[1].trim());
                if (b <= 0) return 0;
                return a / b;
            }catch(Throwable ignored){}
        }
        try { return Double.parseDouble(s); }catch(Throwable ignored){}
        return 0;
    }

    
    // 구슬이 위로 올라가 호퍼 쪽으로 이동하는 연출
    private void animateRiseToHopper(final Player actor, final Runnable onFinish){
        final Location start = base.clone().add(0.5, 1.2, 1.5);
        final ItemStack ball = com.minkang.ultimate.pachinko.util.ItemUtil.getConfiguredBall(plugin, this);
        final Item item = start.getWorld().dropItem(start, ball);
        item.setPickupDelay(Integer.MAX_VALUE);
        item.setGravity(false);

        final Location hopper = findTopHopperLoc();
        final Location end = (hopper != null ? hopper.clone().add(0.5, 0.2, 0.5) : base.clone().add(0.5, 3.5, 1.5));
        final int total = Math.max(10, getRiseTicks());
        new org.bukkit.scheduler.BukkitRunnable(){
            int t=0;
            @Override public void run(){
                t++;
                double tt = com.minkang.ultimate.pachinko.util.Easing.clamp01(t/(double)total);
                double smooth = com.minkang.ultimate.pachinko.util.Easing.easeInOutQuad(tt);
                double x = start.getX() + (end.getX() - start.getX()) * smooth;
                double y = start.getY() + (end.getY() - start.getY()) * smooth;
                double z = start.getZ() + (end.getZ() - start.getZ()) * smooth;
                org.bukkit.Location cur = new org.bukkit.Location(start.getWorld(), x, y, z);
                try { item.teleport(cur); } catch(Throwable ignored){}
                try { cur.getWorld().spawnParticle(org.bukkit.Particle.CRIT, cur, 1); } catch(Throwable ignored){}
                if (t >= total){
                    try { item.remove(); } catch(Throwable ignored){}
                    cancel();
                    if (onFinish != null) onFinish.run();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private Location findTopHopperLoc(){
        org.bukkit.World w = base.getWorld();
        if (w == null) return null;
        for (int dy=1; dy<=8; dy++){
            org.bukkit.Location l = base.clone().add(0, dy, 0);
            try {
                // scan a 7-wide row above base level
                for (int dx=-3; dx<=3; dx++){
                    org.bukkit.Location c = l.clone().add(dx, 0, 0);
                    if (c.getBlock().getType() == org.bukkit.Material.HOPPER) return c;
                }
            }catch(Throwable ignored){}
        }
        return null;
    }

    // 숫자 3개 스핀 연출 (Title) 후, matchChance로 스테이지 진입 여부 결정
    private void spinNumbersAndMaybeEnterStage(final Player actor){
        final int duration = 30; // 1.5s
        new org.bukkit.scheduler.BukkitRunnable(){
            int t=0;
            @Override public void run(){
                t++;
                int a = (int)(Math.random()*10);
                int b = (int)(Math.random()*10);
                int c = (int)(Math.random()*10);
                String title = "§e"+a+" §f- §e"+b+" §f- §e"+c;
                try { actor.sendTitle(title, "§7스테이지 진입 추첨 중...", 0, 5, 5); } catch(Throwable ignored){}
                if (t >= duration){
                    cancel();
                    boolean match = Math.random() < getEntryMatchChance();
                    if (match){
                        beginStage1(actor);
                    }else{
                        com.minkang.ultimate.pachinko.util.Text.msg(actor, "&7[꽝] &f스테이지 진입 실패");
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    // stage control externally
    public void beginStage1(Player actor){
        // operator lock to actor if not set
        if (operator == null && actor != null){
            operator = actor.getUniqueId();
        }
        this.stage = 1;
        this.stagePayout = 0;
        this.stageCup = getStageCup(1);
        startStage(actor);
    }

    private String getOperatorPlayerName(){
        Player op = getOperatorPlayer();
        return (op != null) ? op.getName() : "알 수 없음";
    }
}
