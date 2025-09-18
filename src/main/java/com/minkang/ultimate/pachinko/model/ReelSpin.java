
package com.minkang.ultimate.pachinko.model;

import com.minkang.ultimate.pachinko.data.DataStore;
import com.minkang.ultimate.pachinko.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class ReelSpin {
    private final Plugin plugin;
    private final Machine m;
    private final Player actor;
    private final DataStore store;
    private final Random random = new Random();

    public ReelSpin(Plugin plugin, Machine m, Player actor, DataStore store) {
        this.plugin = plugin;
        this.m = m;
        this.actor = actor;
        this.store = store;
    }

    public void runBatch() {
        final int spins = Math.max(1, m.getPendingSpins());
        m.setPendingSpins(0);

        new BukkitRunnable() {
            int done = 0;
            @Override public void run() {
                if (done >= spins) { m.setDrawingNow(false); cancel(); return; }
                doOne();
                done++;
            }
            void doOne() {
                final int duration = 40;
                new BukkitRunnable() {
                    int tick = 0;
                    int a,b,c;
                    @Override public void run() {
                        tick++;
                        a = random.nextInt(9); b = random.nextInt(9); c = random.nextInt(9);
                        String bar = ChatColor.GOLD + "🎰 " + a + "  " + b + "  " + c;
                        if (actor != null && actor.isOnline()) {
                            actor.sendTitle("§e숫자판 추첨", bar, 0, 10, 0);
                            actor.playSound(actor.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                        }
                        if (tick >= duration) {
                            cancel();
                            boolean triple = (a==b && b==c);
                            if (triple) {
                                Bukkit.broadcastMessage(Text.color("&6&l★ 대박! &e" + (actor!=null?actor.getName():"알 수 없음") + " &7님이 기계 #"+m.getId()+" 에서 숫자 3개 일치!"));
                                advanceStageOrJackpot(actor, m);
                            } else {
                                Text.msg(actor, "&7아쉽게도 숫자가 맞지 않았습니다.");
                            }
                        }
                    }
                }.runTaskTimer(plugin, 1L, 2L);
            }
        }.runTaskTimer(plugin, 0L, 50L);
    }

    private void advanceStageOrJackpot(Player p, Machine m) {
        List<Map<?,?>> stages = plugin.getConfig().getMapList("stages");
        int idx = m.getStageIndex();
        if (idx >= stages.size()-1) {
            int pay = 16 + random.nextInt(16);
            int cap = 128; try { Object o = stages.get(Math.min(Math.max(0,idx), stages.size()-1)).get("payoutCap"); if (o instanceof Number) cap = ((Number)o).intValue(); } catch(Exception ignored){}
            int can = cap - m.getCurrentPayout();
            int give = Math.max(0, Math.min(pay, can));
            if (give>0) m.addPayout(give);
            store.addWin(p!=null?p.getName():"(unknown)", give);
            Text.msg(p, "&6JACKPOT! &a보상 구슬 +" + give + (give<pay?" &7(천장으로 감소)":"") + " &7(금블럭 우클릭으로 수령)");
            if (p!=null) p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            try { if (p!=null) p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(Text.color("&b지급:&e"+m.getCurrentPayout()+"&7/&e"+cap+" &7| &f추첨:&e"+m.getPendingSpins()))); } catch (Throwable t) {}
            return;
        }
        double chance = 0.1;
        Object ch = stages.get(idx).get("advanceChance");
        if (ch instanceof Number) chance = ((Number)ch).doubleValue();
        if (new Random().nextDouble() < chance) {
            m.setStageIndex(idx+1);
            String name = String.valueOf(stages.get(idx+1).get("name"));
            String stars = new String(new char[Math.max(1, (idx+1)+1)]).replace("\0", "★");
            Bukkit.broadcastMessage(Text.color("&6&l"+stars+" 스테이지 상승! &r&f현재: "+name));
            if (p!=null) {
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                try { p.playSound(p.getLocation(), Sound.valueOf(String.valueOf(stages.get(idx+1).get("bgmSound")).toUpperCase().replace('.', '_')), 1.0f, 1.0f); } catch (Exception ignored) {}
                p.sendTitle(Text.color("&e스테이지 진입!"), Text.color(name), 10, 40, 10);
                try { p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(Text.color("&b"+name+" &7| &f지급:&e"+m.getCurrentPayout()+"&7/&e"+String.valueOf(stages.get(idx+1).get("payoutCap"))+" &7| &f추첨:&e"+m.getPendingSpins()))); } catch (Throwable t) { }
            }
        } else {
            Text.msg(p, "&7다음 단계 진입 실패. 현재 스테이지 유지.");
        }
    }
}
