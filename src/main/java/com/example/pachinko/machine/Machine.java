
package com.example.pachinko.machine;

import com.example.pachinko.UltimatePachinko;
import com.example.pachinko.util.Text;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class Machine {
    private final UltimatePachinko plugin;
    private final int id;
    private final Location origin;
    private ItemStack ballItem;
    private final List<StageConfig> stages = new ArrayList<>();
    private final Map<UUID, PlayerState> states = new HashMap<>();
    private Location coalLoc, goldLoc, diaLoc;
    private final List<Location> hopperLocs = new ArrayList<>();
    private boolean animating=false; private final ArrayDeque<Runnable> animQueue=new ArrayDeque<>();
    private UUID lockedBy=null; private long lastLockMs=0L;

    public Machine(UltimatePachinko plugin, int id, Location origin){ this.plugin=plugin; this.id=id; this.origin=origin.clone(); }

    public int getId(){ return id; }
    public Location getOrigin(){ return origin.clone(); }
    public ItemStack getBallItem(){ return ballItem==null? null : ballItem.clone(); }
    public void setBallItem(ItemStack it){ this.ballItem = it==null? null : it.clone(); }
    public List<StageConfig> getStages(){ return stages; }

    public boolean isSpecialBlock(Location loc){
        if (loc==null) return false;
        Location l=loc.getBlock().getLocation();
        return (coalLoc!=null && l.equals(coalLoc.getBlock().getLocation())) ||
               (goldLoc!=null && l.equals(goldLoc.getBlock().getLocation())) ||
               (diaLoc!=null && l.equals(diaLoc.getBlock().getLocation()));
    }

    private void lockBy(Player p){
        int sec = plugin.getConfig().getInt("settings.machineLockSeconds", 15);
        long now = System.currentTimeMillis();
        if (lockedBy==null || now-lastLockMs>sec*1000L){ lockedBy=p.getUniqueId(); }
        lastLockMs = now;
    }
    private boolean checkLocked(Player p){
        if (lockedBy==null) return true;
        int sec = plugin.getConfig().getInt("settings.machineLockSeconds", 15);
        long now = System.currentTimeMillis();
        if (now-lastLockMs>sec*1000L){ lockedBy=p.getUniqueId(); lastLockMs=now; return true; }
        if (!lockedBy.equals(p.getUniqueId())){
            Player owner=Bukkit.getPlayer(lockedBy);
            String name = owner!=null? owner.getName() : "알 수 없음";
            String msg = plugin.getConfig().getString("ui.lockDeny","&c다른 플레이어가 사용 중입니다: &f{player}").replace("{player}", name);
            Text.actionbar(p, msg);
            return false;
        }
        return true;
    }

    public void buildStructure(){
        int width=plugin.getConfig().getInt("settings.width", 9);
        int height=plugin.getConfig().getInt("settings.height", 7);
        World w=origin.getWorld();
        coalLoc=origin.clone().add(-2,0,0);
        goldLoc=origin.clone();
        diaLoc=origin.clone().add(2,0,0);
        coalLoc.getBlock().setType(Material.COAL_BLOCK);
        goldLoc.getBlock().setType(Material.GOLD_BLOCK);
        diaLoc.getBlock().setType(Material.DIAMOND_BLOCK);
        for (int y=1;y<=height;y++){ origin.clone().add(-4,y,0).getBlock().setType(Material.GLASS); origin.clone().add(4,y,0).getBlock().setType(Material.GLASS); }
        for (int y=1;y<=height;y++){ for (int x : new int[]{-2,0,2}) origin.clone().add(x,y,0).getBlock().setType(Material.IRON_BARS); }
        hopperLocs.clear(); int startX=-(width/2);
        for (int i=0;i<width;i++){
            Location h=origin.clone().add(startX+i, height+1, 0);
            Block b=h.getBlock(); b.setType(Material.HOPPER);
            org.bukkit.block.data.type.Hopper data=(org.bukkit.block.data.type.Hopper)b.getBlockData(); data.setFacing(BlockFace.NORTH); b.setBlockData(data);
            hopperLocs.add(h);
        }
    }
    public void clearStructure(){ if (coalLoc!=null) coalLoc.getBlock().setType(Material.AIR); if (goldLoc!=null) goldLoc.getBlock().setType(Material.AIR);
        if (diaLoc!=null) diaLoc.getBlock().setType(Material.AIR); for (Location h : hopperLocs) h.getBlock().setType(Material.AIR); }

    private PlayerState state(Player p){ return states.computeIfAbsent(p.getUniqueId(), u->new PlayerState(u)); }

    // ---- interactions ----
    public void onGoldClick(Player p, ItemStack hand, boolean sneaking){
        if (!checkLocked(p)) return; lockBy(p);
        PlayerState st=state(p);
        // payout
        if (st.stageIndex>0){
            StageConfig sc=stages.get(Math.max(0, st.stageIndex-1));
            int burst=Math.max(1, sc.payoutBurst);
            int remain=Math.max(0, sc.cup - st.payoutProgress);
            int drop=Math.min(remain, burst);
            if (drop>0 && ballItem!=null){
                payoutFromDiamond(p, drop);
                st.payoutProgress += drop;
                String main=plugin.getConfig().getString("ui.payoutTitleMain","&e배출 &6{current} &7/&f {cup}")
                        .replace("{current}", String.valueOf(st.payoutProgress)).replace("{cup}", String.valueOf(sc.cup));
                String sub=plugin.getConfig().getString("ui.payoutTitleSub","");
                p.sendTitle(Text.color(main), Text.color(sub), 0, 20, 10);
                if (st.payoutProgress>=sc.cup){
                    st.payoutComplete=true;
                    p.sendTitle(Text.color(plugin.getConfig().getString("ui.capReachedTitle","&a상한선 도달!")),
                            Text.color(plugin.getConfig().getString("ui.capReachedSub","&7업그레이드에 도전해 보세요!")),0,30,10);
                }
            }
        }
        // insert
        if (hand==null || hand.getType()==Material.AIR || ballItem==null) return;
        if (!com.example.pachinko.util.ItemUtils.equalsLoose(hand, ballItem)){ p.sendMessage(Text.prefix()+Text.color("&c이 기계의 전용구슬이 아닙니다!")); return; }
        int count=1; if (sneaking && plugin.getConfig().getBoolean("settings.allowSneakInsertAll",true)) count=hand.getAmount();
        int remain=hand.getAmount()-count; if (remain<=0) p.getInventory().setItemInMainHand(null); else { hand.setAmount(remain); p.getInventory().setItemInMainHand(hand); }
        for (int i=0;i<count;i++){ final boolean upgradeAttempt = (st.stageIndex>0); enqueue(()->animateBall(p, upgradeAttempt)); }
    }

    public void onCoalClick(Player p){
        if (!checkLocked(p)) return; lockBy(p);
        PlayerState st=state(p); int max=plugin.getConfig().getInt("settings.tokensMax",5);
        if (st.stageIndex>0){ Text.actionbar(p, Text.color("&7현재 스테이지 진행중입니다. 중앙 통과로 업그레이드 판정!")); return; }
        if (st.tokens<=0){ String fmt=plugin.getConfig().getString("ui.actionbarTokensFormat","&6추첨권 &f{tokens}&7/&f{max}").replace("{tokens}",String.valueOf(st.tokens)).replace("{max}",String.valueOf(max)); Text.actionbar(p, fmt); return; }
        int times=(st.tokens==max)? max : 1; st.tokens -= times; runEntryDrawSequence(p, times);
        String fmt=plugin.getConfig().getString("ui.actionbarTokensFormat","&6추첨권 &f{tokens}&7/&f{max}").replace("{tokens}",String.valueOf(st.tokens)).replace("{max}",String.valueOf(max)); Text.actionbar(p, fmt);
    }

    // internals
    private void enqueue(Runnable r){ animQueue.addLast(r); tryStartNext(); }
    private void tryStartNext(){ if (animating) return; Runnable n=animQueue.pollFirst(); if (n!=null){ animating=true; Bukkit.getScheduler().runTask(plugin, n);} }
    private void finishAnim(){ animating=false; tryStartNext(); }

    private void onCenterPass(Player p, boolean upgradeAttempt){
        PlayerState st=state(p);
        if (upgradeAttempt) startSpin(p,true,1);
        else { int max=plugin.getConfig().getInt("settings.tokensMax",5); if (st.tokens<max) st.tokens++; String fmt=plugin.getConfig().getString("ui.actionbarTokensFormat","&6추첨권 &f{tokens}&7/&f{max}").replace("{tokens}",String.valueOf(st.tokens)).replace("{max}",String.valueOf(max)); Text.actionbar(p, fmt); p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.6f); }
    }
    private void runEntryDrawSequence(Player p, int times){ startSpin(p,false,times); }

    private void startSpin(Player p, boolean upgradeAttempt, int times){
        if (times<=0) return;
        final int duration=plugin.getConfig().getInt("settings.spinDurationTicks",40);
        final int dps=plugin.getConfig().getInt("settings.digitsPerSecond",20);
        new BukkitRunnable(){ int done=0; @Override public void run(){ if (done>=times){ cancel(); return; }
            new BukkitRunnable(){ int tick=0; @Override public void run(){ int a=(int)(Math.random()*10), b=(int)(Math.random()*10), c=(int)(Math.random()*10);
                String title=plugin.getConfig().getString("ui.spinTitleFormat","&f{a} &7| &f{b} &7| &f{c}").replace("{a}",String.valueOf(a)).replace("{b}",String.valueOf(b)).replace("{c}",String.valueOf(c));
                p.sendTitle(Text.color(title), "", 0, 6, 2); tick++; if (tick>=duration){ this.cancel();
                    boolean success; if (upgradeAttempt){ PlayerState st=state(p); if (st.stageIndex<=0) success=false; else { StageConfig sc=stages.get(Math.max(0, st.stageIndex-1)); success=Math.random()*100.0<sc.upgradeChance; } }
                    else { double base=plugin.getConfig().getDouble("settings.entryMatchChance",0.01); success=Math.random()<base; }
                    if (success){ String msg=upgradeAttempt? plugin.getConfig().getString("ui.upgradeSuccessTitle","&a업그레이드 성공! &e스테이지 {stage}") : plugin.getConfig().getString("ui.enterTitle","&a스테이지 진입! &e{stage}"); onSpinSuccess(p, upgradeAttempt, msg); }
                    else{ String msg=upgradeAttempt? plugin.getConfig().getString("ui.upgradeFailTitle","&c업그레이드 실패") : plugin.getConfig().getString("ui.spinFailTitle","&c&l실패"); onSpinFail(p, upgradeAttempt, msg); }
                }} }.runTaskTimer(plugin, 0L, Math.max(1L, 20L/dps)); done++; } }.runTaskTimer(plugin, 0L, duration + 10L);
    }
    private void onSpinSuccess(Player p, boolean upgradeAttempt, String titleMsg){
        PlayerState st=state(p);
        if (upgradeAttempt){
            if (st.stageIndex<stages.size()){ st.stageIndex++; st.payoutComplete=false; StageConfig sc=stages.get(st.stageIndex-1);
                p.sendTitle(Text.color(titleMsg.replace("{stage}", String.valueOf(st.stageIndex))), "", 0, 50, 10); celebrate(p, st.stageIndex, true); updateRanking(p, st.stageIndex);
                p.sendTitle(Text.color("&e진행 &6"+st.payoutProgress+" &7/&f "+sc.cup), "", 0, 20, 10);
            } else { p.sendTitle(Text.color("&b최고 스테이지 유지!"), "", 0, 30, 10); }
        } else {
            st.stageIndex=1; st.payoutProgress=0; st.payoutComplete=false; StageConfig sc=stages.get(0);
            p.sendTitle(Text.color(titleMsg.replace("{stage}", "1")), "", 0, 60, 10); celebrate(p, 1, false); updateRanking(p, 1);
            p.sendTitle(Text.color("&f보상: &e0&7/&f"+sc.cup+" &7(금블럭 우클릭)"), "", 0, 50, 10);
        }
    }
    private void onSpinFail(Player p, boolean upgradeAttempt, String titleMsg){
        PlayerState st=state(p);
        if (upgradeAttempt){ p.sendTitle(Text.color(titleMsg), "", 0, 30, 10); p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.3f); st.stageIndex=0; st.payoutProgress=0; st.payoutComplete=false; }
        else { p.sendTitle(Text.color(titleMsg), "", 0, 20, 10); p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.8f); }
    }

    private void payoutFromDiamond(Player p, int amount){
        if (ballItem==null) return; Location drop=diaLoc.clone().add(0.5,1.1,0.5); double vel=plugin.getConfig().getDouble("settings.rewardLaunchVelocity",0.35);
        for (int i=0;i<amount;i++){ ItemStack it=ballItem.clone(); it.setAmount(1); Item ent=p.getWorld().dropItem(drop, it);
            ent.setOwner(p.getUniqueId()); ent.setPickupDelay(10); ent.setVelocity(new Vector((Math.random()-0.5)*0.25, vel+Math.random()*0.15, (Math.random()-0.5)*0.25));
            p.spawnParticle(Particle.CRIT, drop, 6, 0.1,0.1,0.1, 0.02); p.playSound(drop, Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.6f); }
    }

    private void animateBall(Player p, boolean upgradeAttempt){
        World w=origin.getWorld(); Location start=goldLoc.clone().add(0.5,1.0,0.5);
        final ItemStack vis=(ballItem==null)? new ItemStack(Material.SLIME_BALL) : ballItem.clone(); vis.setAmount(1);
        final Item item=w.dropItem(start, vis); item.setPickupDelay(Integer.MAX_VALUE); item.setGravity(false); item.setVelocity(new Vector(0,0,0));
        final double yTop=origin.getY()+plugin.getConfig().getInt("settings.height",7)+0.75;
        final double leftX=origin.getX()-3.5, rightX=origin.getX()+3.5, centerX=origin.getX()+0.5, centerY=origin.getY()+3.5;
        final double stepY=0.12, stepX=0.18;
        new BukkitRunnable(){ double x=start.getX(), y=start.getY(), vx=stepX; boolean centered=false;
            @Override public void run(){ y+=stepY; x+=vx; if (x<=leftX || x>=rightX){ vx=-vx; x+=vx; } item.teleport(new Location(w,x,y,start.getZ()));
                if (!centered && Math.abs(x-centerX)<0.3 && Math.abs(y-centerY)<0.25){ centered=true; onCenterPass(p, upgradeAttempt); }
                if (y>=yTop){ this.cancel(); item.remove();
                    Location cur=new Location(w,x,yTop,start.getZ()); Location target=null; double best=Double.MAX_VALUE; int targetIdx=-1;
                    for (int i=0;i<hopperLocs.size();i++){ Location h=hopperLocs.get(i); double d=h.distanceSquared(cur); if (d<best){ best=d; target=h; targetIdx=i; } }
                    if (target!=null && target.getBlock().getState() instanceof Container){
                        Container c=(Container)target.getBlock().getState(); Inventory inv=c.getInventory(); ItemStack it=vis.clone(); it.setAmount(1);
                        java.util.HashMap<Integer,ItemStack> left=inv.addItem(it); if (!left.isEmpty()) w.dropItem(target.clone().add(0.5,0.7,0.5), it);
                        w.playSound(target, Sound.BLOCK_HOPPER_INSIDE, 0.6f, 1.1f); w.spawnParticle(Particle.SPELL_INSTANT, target.clone().add(0.5,0.7,0.5), 12, 0.2,0.2,0.2, 0.05);
                        // ---- NEW: show which hopper it went into (if not center) ----
                        boolean onlyWhenNotCenter = plugin.getConfig().getBoolean("ui.hopperOnlyWhenNotCenter", true);
                        boolean useActionbar = plugin.getConfig().getBoolean("ui.hopperActionbar", true);
                        int width = hopperLocs.size();
                        int centerIdx = width/2; // 0-based
                        boolean isCenter = (targetIdx == centerIdx);
                        if (!onlyWhenNotCenter || !isCenter){
                            String fmt = plugin.getConfig().getString("ui.hopperEnterFormat", "&7상단 {side}{offset}호퍼에 흡수됨 (&f#{index}&7/&f{width})");
                            String sideLeft = plugin.getConfig().getString("ui.hopperSideLeft", "좌측 ");
                            String sideRight = plugin.getConfig().getString("ui.hopperSideRight", "우측 ");
                            String sideCenter = plugin.getConfig().getString("ui.hopperSideCenter", "정중앙 ");
                            String offsetFmt = plugin.getConfig().getString("ui.hopperOffsetFormat", "{n}번 ");
                            String sideStr = isCenter ? sideCenter : (targetIdx < centerIdx ? sideLeft : sideRight);
                            String offsetStr = isCenter ? "" : offsetFmt.replace("{n}", String.valueOf(Math.abs(targetIdx - centerIdx)));
                            String msg = fmt.replace("{side}", sideStr).replace("{offset}", offsetStr)
                                            .replace("{index}", String.valueOf(targetIdx+1)).replace("{width}", String.valueOf(width));
                            if (useActionbar) Text.actionbar(p, Text.color(msg)); else p.sendMessage(Text.prefix()+Text.color(msg));
                        }
                    }
                    finishAnim(); } } }.runTaskTimer(plugin, 1L, 1L);
    }

    private void celebrate(Player p, int stage, boolean upgrade){
        if (!plugin.getConfig().getBoolean("broadcast.enabled", true)) return;
        if (!plugin.getConfig().getBoolean("settings.allowBroadcasts", true)) return;
        String msg; if (!upgrade){ msg=plugin.getConfig().getString("broadcast.enter","&b[파칭코] &f{player}&7 님이 &e스테이지 {stage}&7 에 진입했습니다! &6★"); }
        else { String key="broadcast.upgradeLevels."+stage; if (plugin.getConfig().contains(key)) msg=plugin.getConfig().getString(key);
               else msg=plugin.getConfig().getString("broadcast.upgradeLevels.3","&d[파칭코] &f{player}&7 님이 &e스테이지 {stage}&7 로 상승!! &6★★"); }
        msg=msg.replace("{player}", p.getName()).replace("{stage}", String.valueOf(stage));
        for (Player pl : Bukkit.getOnlinePlayers()) pl.sendMessage(Text.color(msg));
        try{ Sound s=Sound.valueOf(plugin.getConfig().getString("broadcast.sound","UI_TOAST_CHALLENGE_COMPLETE"));
             for (Player pl : Bukkit.getOnlinePlayers()) pl.playSound(pl.getLocation(), s, 0.8f, 1.1f);
        }catch(Exception ignore){}
    }
    private void updateRanking(Player p, int stage){
        String path="ranking."+id+"."+p.getUniqueId().toString()+".bestStage";
        int prev=plugin.getConfig().getInt(path,0); if (stage>prev){ plugin.getConfig().set(path, stage);
            plugin.getConfig().set("ranking."+id+"."+p.getUniqueId().toString()+".name", p.getName()); plugin.saveConfig(); }
    }
}
