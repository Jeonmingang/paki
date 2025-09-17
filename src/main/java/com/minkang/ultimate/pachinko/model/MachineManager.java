
package com.minkang.ultimate.pachinko.model;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.data.DataStore;
import com.minkang.ultimate.pachinko.util.ItemSerializer;
import com.minkang.ultimate.pachinko.util.Locs;
import com.minkang.ultimate.pachinko.util.Text;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class MachineManager {

    private final Plugin plugin;
    private final DataStore store;
    private final Map<Integer, Machine> machines = new HashMap<>();
    private final Random random = new Random();

    public MachineManager(Plugin plugin, DataStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public Map<Integer, Machine> getMachines() { return machines; }

    public void loadMachines() {
        machines.clear();
        for (String id : store.getMachineIds()) {
            Map<String,Object> map = store.loadMachine(id);
            if (map == null) continue;
            Location base = Locs.fromString((String)map.get("base"));
            Machine m = new Machine(Integer.parseInt(id), base);
            m.setGoldButton(Locs.fromString((String)map.get("gold")));
            List<String> hop = (List<String>) map.get("hoppers");
            List<Location> hlocs = new ArrayList<>();
            if (hop != null) for (String s : hop) hlocs.add(Locs.fromString(s));
            m.setHopperLocations(hlocs);
            String ball64 = (String)map.get("ball");
            if (ball64 != null) m.setBallItem(ItemSerializer.fromBase64(ball64));
            List<Integer> ws = (List<Integer>)map.get("weights");
            if (ws != null && ws.size()==7) {
                int[] arr = new int[7];
                for (int i=0;i<7;i++) arr[i]=ws.get(i);
                m.setWeights(arr);
            }
            machines.put(m.getId(), m);
        }
    }

    public void saveMachines() {
        for (Machine m : machines.values()) {
            Map<String,Object> map = new HashMap<>();
            map.put("base", Locs.toString(m.getBase()));
            map.put("gold", Locs.toString(m.getGoldButton()));
            List<String> hop = new ArrayList<>();
            for (Location l : m.getHopperLocations()) hop.add(Locs.toString(l));
            map.put("hoppers", hop);
            map.put("weights", Arrays.asList(
                    m.getWeights()[0],m.getWeights()[1],m.getWeights()[2],m.getWeights()[3],
                    m.getWeights()[4],m.getWeights()[5],m.getWeights()[6]
            ));
            if (m.getBallItem()!=null) map.put("ball", ItemSerializer.toBase64(m.getBallItem()));
            store.saveMachine(String.valueOf(m.getId()), map);
        }
        store.save();
    }

    public void shutdown() {
        // nothing yet; tasks are per-runner and cancel on disable
    }

    // Installation: builds a 7-hopper frame like the screenshot (simple version)
    public Machine installMachine(Player p, int id) {
        Location base = p.getLocation().getBlock().getLocation();
        base.setY(base.getY()); // ground

        // Build frame: glass/iron bars grid, top with 7 hoppers, bottom with diamond/iron/gold (cosmetic)
        World w = base.getWorld();
        int x0 = base.getBlockX();
        int y0 = base.getBlockY();
        int z0 = base.getBlockZ();

        // clear area (7 wide, 8 tall, 1 deep)
        for (int y=0;y<9;y++) for (int x=-4;x<=4;x++) {
            w.getBlockAt(x0+x, y0+y, z0).setType(Material.AIR);
        }

        // Build wall (brick back plane behind)
        for (int y=0;y<8;y++) for (int x=-4;x<=4;x++) {
            w.getBlockAt(x0+x, y0+y, z0-1).setType(Material.BRICKS);
        }

        // Place gold block (payout button) at base center
        Block gold = w.getBlockAt(x0, y0, z0);
        gold.setType(Material.GOLD_BLOCK);

        // Decorative bottom
        w.getBlockAt(x0-2, y0, z0).setType(Material.DIAMOND_BLOCK);
        w.getBlockAt(x0+2, y0, z0).setType(Material.GOLD_BLOCK);

        // Glass & iron bar rails (simple lanes)
        for (int row=1; row<=6; row++) {
            for (int col=-3; col<=3; col++) {
                Material mat = (row % 2 == 0) ? Material.IRON_BARS : Material.GLASS;
                w.getBlockAt(x0+col, y0+row, z0).setType(mat);
            }
        }

        // Top shelf and hoppers
        List<Location> hopperLocs = new ArrayList<>();
        for (int i=-3;i<=3;i++) {
            Block slab = w.getBlockAt(x0+i, y0+7, z0);
            slab.setType(Material.SMOOTH_STONE_SLAB);
            Block hop = w.getBlockAt(x0+i, y0+8, z0);
            hop.setType(Material.HOPPER);
            hopperLocs.add(hop.getLocation());
        }

        Machine m = new Machine(id, base);
        m.setGoldButton(gold.getLocation());
        m.setHopperLocations(hopperLocs);

        // default ball from config
        ItemStack def = defaultBall();
        m.setBallItem(def);

        machines.put(id, m);
        saveMachines();

        Text.msg(p, "&a파칭코 기계 #" + id + " 설치 완료! 중앙 금블럭: 우클릭으로 보상 수령.");
        return m;
    }

    private ItemStack defaultBall() {
        FileConfiguration cfg = plugin.getConfig();
        String type = cfg.getString("defaultBall.type", "SLIME_BALL");
        String name = cfg.getString("defaultBall.name", "&a파칭코 구슬");
        List<String> lore = cfg.getStringList("defaultBall.lore");
        Material mat = Material.matchMaterial(type);
        if (mat == null) mat = Material.SLIME_BALL;
        ItemStack it = new ItemStack(mat, 1);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(name));
            List<String> ll = new ArrayList<>();
            for (String s : lore) ll.add(Text.color(s));
            meta.setLore(ll);
            it.setItemMeta(meta);
        }
        return it;
    }

    public boolean deleteMachine(Player p, int id) {
        Machine m = machines.remove(id);
        if (m == null) return false;
        // Try to clear placed blocks (hopper+glass area)
        World w = m.getBase().getWorld();
        int x0 = m.getBase().getBlockX();
        int y0 = m.getBase().getBlockY();
        int z0 = m.getBase().getBlockZ();
        for (int y=0;y<10;y++) for (int x=-4;x<=4;x++) {
            Block b = w.getBlockAt(x0+x, y0+y, z0);
            if (b.getType()!=Material.AIR) b.setType(Material.AIR);
        }
        // leave back wall
        store.removeMachine(String.valueOf(id));
        store.save();
        Text.msg(p, "&c기계 #" + id + " 삭제 완료.");
        return true;
    }

    public Machine get(int id) { return machines.get(id); }

    public int chooseSlotWeighted(Machine m) {
        int[] w = m.getWeights();
        int sum = 0;
        for (int v : w) sum += v;
        if (sum <= 0) return 4; // center fallback
        int r = random.nextInt(sum);
        int acc = 0;
        for (int i=0;i<7;i++) {
            acc += w[i];
            if (r < acc) return i+1;
        }
        return 4;
    }

    public void launchBall(Player actor, Machine m) {
        // Visual path -> choose slot
        int chosen = chooseSlotWeighted(m);

        new BallRunner(plugin, m, chosen).start();

        // Stage/logic hooks
        if (chosen == 4) {
            // center: pending spins up to max
            int max = plugin.getConfig().getInt("centerSlot.maxPendingSpins", 5);
            m.addPendingSpin(max);
            Text.msg(actor, "&e중앙 진입! &7추첨 기회가 &b" + m.getPendingSpins() + "&7/"
                    + max + " 로 누적되었습니다.");
            playBgm(actor, currentStageSound(m));
            if (actor!=null) actor.sendTitle(com.minkang.ultimate.pachinko.util.Text.color("&e"+stageName(m)), com.minkang.ultimate.pachinko.util.Text.color("&7추첨기회: &e"+m.getPendingSpins()), 5, 30, 5);
            showHud(actor, m);
            // After spin animation, actually roll later when requested by command or auto
            // We auto-run a spin here each time.
            new ReelSpin(plugin, m, actor, store).runOnce();
        } else {
            // side slots: maybe bonus
            List<Integer> bonusSlots = plugin.getConfig().getIntegerList("sideBonus.slots");
            if (bonusSlots.contains(chosen)) {
                int min = plugin.getConfig().getInt("sideBonus.min",1);
                int max = plugin.getConfig().getInt("sideBonus.max",3);
                int give = min + random.nextInt(Math.max(1, (max - min + 1)));
                int given = addPayoutWithCap(actor, m, give);
                Text.msg(actor, "&a보너스! &f슬롯 " + chosen + " &7→ 추가 구슬 &b+" + given + (given<give?" &7(천장으로 감소)":""));
            } else {
                Text.msg(actor, "&7슬롯 " + chosen + " (꽝)");
            }
        }
    }

    public void payOut(Player p, Machine m) {
        int amount = m.takePayoutAll();
        if (amount <= 0) {
            Text.msg(p, "&7받을 보상이 없습니다.");
            return;
        }
        ItemStack it = m.getBallItem();
        if (it == null) {
            Text.msg(p, "&c이 기계의 구슬 아이템이 설정되지 않았습니다.");
            return;
        }
        ItemStack give = it.clone();
        give.setAmount(Math.min(64, amount));
        int left = amount;
        while (left > 0) {
            give.setAmount(Math.min(64, left));
            HashMap<Integer, ItemStack> remain = p.getInventory().addItem(give.clone());
            if (!remain.isEmpty()) {
                p.getWorld().dropItemNaturally(p.getLocation(), give.clone());
            }
            left -= give.getAmount();
        }
        Text.msg(p, "&a보상 지급: &e" + amount + "개");
    }

    public void giveMarble(Player sender, OfflinePlayer target, int amount, Machine m) {
        if (target == null) return;
        if (m.getBallItem() == null) return;
        ItemStack it = m.getBallItem().clone();
        int left = amount;
        if (target.isOnline()) {
            Player tp = target.getPlayer();
            while (left > 0) {
                int stack = Math.min(64, left);
                ItemStack gi = it.clone();
                gi.setAmount(stack);
                HashMap<Integer, ItemStack> remain = tp.getInventory().addItem(gi);
                if (!remain.isEmpty()) tp.getWorld().dropItemNaturally(tp.getLocation(), gi);
                left -= stack;
            }
            Text.msg(tp, "&a관리자로부터 파칭코 구슬 " + amount + "개를 지급받았습니다. (#" + m.getId() + ")");
        }
    }

    private String currentStageSound(Machine m) {
        List<Map<?,?>> list = plugin.getConfig().getMapList("stages");
        int idx = Math.min(Math.max(0, m.getStageIndex()), list.size()-1);
        Map<?,?> st = list.get(idx);
        Object o = st.get("bgmSound");
        return (o == null) ? "music_disc.cat" : String.valueOf(o);
    }



    private java.util.Map<?,?> stageMap(Machine m) {
        java.util.List<java.util.Map<?,?>> list = plugin.getConfig().getMapList("stages");
        int idx = Math.min(Math.max(0, m.getStageIndex()), list.size()-1);
        return list.get(idx);
    }
    private int stageCap(Machine m) {
        Object o = stageMap(m).get("payoutCap");
        if (o instanceof Number) return ((Number)o).intValue();
        return 128;
    }
    private String stageName(Machine m) {
        Object o = stageMap(m).get("name");
        return o==null? "&7(스테이지)" : String.valueOf(o);
    }
    private void showHud(org.bukkit.entity.Player p, Machine m) {
        if (p==null) return;
        String msg = com.minkang.ultimate.pachinko.util.Text.color("&b"+stageName(m)+" &7| &f지급:&e"+m.getCurrentPayout()+"&7/&e"+stageCap(m)+" &7| &f추첨:&e"+m.getPendingSpins());
        try {
            p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(msg));
        } catch (Throwable t) {
            p.sendActionBar(msg);
        }
    }
    private int addPayoutWithCap(org.bukkit.entity.Player p, Machine m, int amount) {
        int cap = stageCap(m);
        int can = cap - m.getCurrentPayout();
        if (can <= 0) {
            com.minkang.ultimate.pachinko.util.Text.msg(p, "&7해당 스테이지 천장에 도달했습니다. (지급 정지)");
            showHud(p, m);
            return 0;
        }
        int give = Math.min(can, Math.max(0, amount));
        if (give > 0) {
            m.addPayout(give);
        }
        showHud(p, m);
        return give;
    }
    private void stageUpBroadcast(org.bukkit.entity.Player p, Machine m) {
        int idx = m.getStageIndex();
        String stars = new String(new char[Math.max(1, idx+1)]).replace("\0", "★");
        String msg = com.minkang.ultimate.pachinko.util.Text.color("&6&l"+stars+" 스테이지 상승! &r&f현재: "+stageName(m));
        org.bukkit.Bukkit.broadcastMessage(msg);
        // BGM & Title
        playBgm(p, currentStageSound(m));
        if (p!=null) p.sendTitle(com.minkang.ultimate.pachinko.util.Text.color("&e스테이지 상승!"), com.minkang.ultimate.pachinko.util.Text.color(stageName(m)), 10, 40, 10);
        showHud(p, m);
    }

    private void playBgm(Player p, String sound) {
        if (p == null) return;
        try {
            p.playSound(p.getLocation(), Sound.valueOf(sound.toUpperCase().replace('.', '_')), 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            // fallback generic
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
        }
    }


    public java.util.List<java.util.Map.Entry<String,Integer>> topWins(int limit) {
        return store.topWins(limit);
    }

}
