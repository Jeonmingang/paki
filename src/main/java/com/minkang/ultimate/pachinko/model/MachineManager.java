
package com.minkang.ultimate.pachinko.model;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.data.DataStore;
import com.minkang.ultimate.pachinko.util.Locs;
import com.minkang.ultimate.pachinko.util.Text;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class MachineManager {
    private final Plugin plugin;
    private final DataStore store;
    private final Random random = new Random();
    private final Map<Integer, Machine> machines = new HashMap<>();

    public MachineManager(Plugin plugin, DataStore store){
        this.plugin = plugin; this.store = store;
    }
    public Map<Integer, Machine> getMachines(){ return machines; }

    public void loadMachines(){
        machines.clear();
        Map<String,Object> all = store.getAll();
        if (all == null) return;
        for (String id : all.keySet()){
            Map<?,?> map = store.getMachine(id);
            if (map == null) continue;
            Location base = Locs.fromString(String.valueOf(map.get("base")));
            Machine m = new Machine(Integer.parseInt(id), base);
            m.setGoldButton(Locs.fromString(String.valueOf(map.get("gold"))));
            m.setDiamondBlock(Locs.fromString(String.valueOf(map.get("diamond"))));
            m.setCoalButton(Locs.fromString(String.valueOf(map.get("coal"))));
            List<String> hop = (List<String>) map.get("hoppers");
            List<Location> hops = new ArrayList<>();
            if (hop != null) for (String s : hop) hops.add(Locs.fromString(s));
            m.setHopperLocs(hops);
            ItemStack ball = readBallFromConfig(plugin.getConfig());
            m.setBallItem(ball);
            List<Integer> w = (List<Integer>) map.get("weights");
            if (w != null && w.size()==7){
                int[] ww = new int[7];
                for (int i=0;i<7;i++) ww[i]=w.get(i);
                m.setWeights(ww);
            }
            machines.put(m.getId(), m);
        }
    }

    public void saveMachines(){
        for (Machine m : machines.values()){
            Map<String,Object> map = new HashMap<>();
            map.put("base", Locs.toString(m.getBase()));
            map.put("gold", Locs.toString(m.getGoldButton()));
            map.put("diamond", Locs.toString(m.getDiamondBlock()));
            map.put("coal", Locs.toString(m.getCoalButton()));
            List<String> hop = new ArrayList<>();
            for (Location l : m.getHopperLocs()) hop.add(Locs.toString(l));
            map.put("hoppers", hop);
            int[] w = m.getWeights();
            List<Integer> wl = new ArrayList<>();
            for (int i=0;i<7;i++) wl.add(w[i]);
            map.put("weights", wl);
            store.setMachine(String.valueOf(m.getId()), map);
        }
        store.save();
    }

    public Machine installMachine(Player p, int id){
        Location base = p.getLocation().getBlock().getLocation();
        World w = base.getWorld();
        int x0=base.getBlockX(), y0=base.getBlockY(), z0=base.getBlockZ();

        // 설치 전 영역 클리어 (10x9)
        for (int y=0;y<=9;y++) for (int x=-4;x<=4;x++) w.getBlockAt(x0+x, y0+y, z0).setType(Material.AIR);

        // 하단 버튼 블럭: 다이아(좌) / 금(중앙) / 석탄(우)
        Block dia = w.getBlockAt(x0-2, y0, z0); dia.setType(Material.DIAMOND_BLOCK);
        Block gold = w.getBlockAt(x0, y0, z0); gold.setType(Material.GOLD_BLOCK);
        Block coal = w.getBlockAt(x0+2, y0, z0); coal.setType(Material.COAL_BLOCK);

        // 유리/철창 그리드 (사진 유사)
        for (int row=1; row<=6; row++){
            for (int col=-3; col<=3; col++){
                w.getBlockAt(x0+col, y0+row, z0).setType((row%2==0)?Material.IRON_BARS:Material.LIGHT_BLUE_STAINED_GLASS);
            }
        }
        // 상단 스톤 슬랩 + 홉퍼
        List<Location> hopperLocs = new ArrayList<>();
        for (int i=-3;i<=3;i++){
            w.getBlockAt(x0+i, y0+7, z0).setType(Material.SMOOTH_STONE_SLAB);
            Block hop = w.getBlockAt(x0+i, y0+8, z0); hop.setType(Material.HOPPER);
            hopperLocs.add(hop.getLocation());
        }

        Machine m = new Machine(id, base);
        m.setGoldButton(gold.getLocation());
        m.setDiamondBlock(dia.getLocation());
        m.setCoalButton(coal.getLocation());
        m.setHopperLocs(hopperLocs);
        m.setBallItem(readBallFromConfig(plugin.getConfig()));
        int[] wts = readWeights(plugin.getConfig());
        m.setWeights(wts);
        machines.put(id, m);

        saveMachines();
        Text.msg(p, "&a파칭코 기계 #"+id+" 설치 완료.");
        return m;
    }

    public boolean deleteMachine(Player p, int id){
        Machine m = machines.remove(id);
        if (m == null) return false;
        World w = m.getBase().getWorld();
        int x0=m.getBase().getBlockX(), y0=m.getBase().getBlockY(), z0=m.getBase().getBlockZ();
        for (int y=0;y<=9;y++) for (int x=-4;x<=4;x++){
            Block b = w.getBlockAt(x0+x, y0+y, z0);
            if (b.getType()!=Material.AIR) b.setType(Material.AIR);
        }
        store.remove(String.valueOf(id)); store.save();
        return true;
    }

    public int chooseSlotWeighted(Machine m){
        int[] w = m.getWeights();
        int sum = 0; for (int v : w) sum += v;
        if (sum <= 0) return 4;
        int r = random.nextInt(sum), acc = 0;
        for (int i=0;i<7;i++){ acc += w[i]; if (r < acc) return i+1; }
        return 4;
    }

    public void launchBall(Player actor, Machine m){
        int chosen = chooseSlotWeighted(m);
        // 애니메이션 종료 후 결과 처리 (선꽝 방지)
        new BallRunner(plugin, m, chosen, (slot)->{
            if (slot == 4){
                int max = plugin.getConfig().getInt("centerSlot.maxPendingSpins", 5);
                m.addPendingSpin(max);
                Text.msg(actor, "&e중앙 진입! &7추첨 기회가 &b"+m.getPendingSpins()+"&7/"+max+" 로 누적되었습니다.");
                if (actor!=null) actor.sendTitle("§e중앙 진입", "§f추첨 기회 +1", 5, 30, 5);
            }else{
                // 사이드 보너스 (선택 사항)
                int give = 0;
                int slotIdx = slot-1;
                if (slotIdx>=0 && slotIdx<7){
                    // 간단히 1~3 보너스 (좌우 2칸 가중치 부여는 생략)
                    give = 0;
                }
                if (give > 0){
                    m.addPayout(give);
                    Text.msg(actor, "&7슬롯 "+slot+" &7→ 추가 구슬 &b+"+give);
                }else{
                    Text.msg(actor, "&7슬롯 "+slot+" (꽝)");
                }
            }
        }).start();
    }

    public void payOutAtDiamond(Player p, Machine m){
        int amt = m.takePayoutAll();
        if (amt <= 0){ Text.msg(p, "&7받을 보상이 없습니다."); return; }
        ItemStack it = m.getBallItem(); if (it==null){ Text.msg(p, "&c구슬 아이템 미설정."); return; }
        int left = amt;
        while (left > 0){
            int stack = Math.min(64, left);
            ItemStack give = it.clone(); give.setAmount(stack);
            m.getBase().getWorld().dropItemNaturally(m.getDiamondBlock().clone().add(0.5,1.0,0.5), give);
            left -= stack;
        }
        Text.msg(p, "&a보상 지급: &e"+amt+"개");
    }

    public void dispenseStageMarbles(Player p, Machine m){
        ItemStack it = m.getBallItem(); if (it==null){ Text.msg(p, "&c구슬 아이템 미설정."); return; }
        int cnt = plugin.getConfig().getInt("stage.dispenseCountPerClick", m.getHopperLocs().size());
        for (int i=0;i<cnt;i++){
            ItemStack give = it.clone(); give.setAmount(1);
            m.getBase().getWorld().dropItemNaturally(m.getDiamondBlock().clone().add(0.5,1.0,0.5), give);
        }
        Text.msg(p, "&b스테이지 보급: 구슬 "+cnt+"개 배출");
    }

    public String stageName(Machine m){
        List<Map<?,?>> list = plugin.getConfig().getMapList("stages");
        int idx = m.getStageIndex();
        if (idx < 0 || idx >= list.size()) return plugin.getConfig().getString("hud.normalName","&7일반 모드");
        Object n = list.get(idx).get("name");
        return n==null? "&7(스테이지)" : String.valueOf(n);
    }

    public void showHud(Player p, Machine m){
        if (p==null) return;
        String mode = plugin.getConfig().getString("hud.countMode","claimable");
        int shown = "stageTotal".equalsIgnoreCase(mode) ? m.getStagePayout() : m.getPendingPayout();
        String msg = Text.color("&b"+stageName(m)+" &7| &f추첨:&e"+m.getPendingSpins()+" &7| &f지급:&e"+shown);
        p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(msg));
    }

    private int[] readWeights(FileConfiguration cfg){
        List<Integer> w = cfg.getIntegerList("globalWeights");
        if (w==null || w.size()!=7) return new int[]{14,14,14,6,14,14,14};
        int[] a = new int[7]; for (int i=0;i<7;i++) a[i]=w.get(i); return a;
    }
    private ItemStack readBallFromConfig(FileConfiguration cfg){
        String type = cfg.getString("defaultBall.type","SLIME_BALL");
        String name = Text.color(cfg.getString("defaultBall.name","&a파칭코 구슬"));
        java.util.List<String> lore = cfg.getStringList("defaultBall.lore");
        Material mat = Material.matchMaterial(type); if (mat==null) mat = Material.SLIME_BALL;
        ItemStack it = new ItemStack(mat, 1);
        ItemMeta meta = it.getItemMeta();
        if (meta != null){
            meta.setDisplayName(name);
            if (lore!=null){
                java.util.List<String> ll = new java.util.ArrayList<>();
                for (String s : lore) ll.add(Text.color(s));
                meta.setLore(ll);
            }
            it.setItemMeta(meta);
        }
        return it;
    }
}
