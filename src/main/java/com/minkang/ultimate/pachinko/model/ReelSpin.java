
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

    public void runOnce() {
        final int spins = Math.max(1, m.getPendingSpins());
        m.resetPendingSpins();

        new BukkitRunnable() {
            int tick = 0;
            int displayA = 0, displayB = 0, displayC = 0;
            @Override public void run() {
                tick++;
                displayA = random.nextInt(9); displayB = random.nextInt(9); displayC = random.nextInt(9);
                String bar = ChatColor.GOLD + "🎰 " + displayA + "  " + displayB + "  " + displayC;
                if (actor != null && actor.isOnline()) {
                    actor.sendTitle("§e숫자판 추첨", bar, 0, 10, 0);
                    actor.playSound(actor.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                }
                if (tick >= 40) { // stop
                    cancel();
                    boolean triple = isTripleSuccess(spins);
                    if (triple) {
                        Bukkit.broadcastMessage(Text.color("&6&l★ 대박! &e" + actor.getName() + " &7님이 기계 #"+m.getId()+" 에서 숫자 3개 일치!"));
                        advanceStageOrJackpot(actor, m);
                    } else {
                        Text.msg(actor, "&7아쉽게도 숫자가 맞지 않았습니다.");
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }

    private boolean isTripleSuccess(int spins) {
        // Each spin attempt: if any hit success with chance tripleMatchChance -> true
        double base = plugin.getConfig().getDouble("centerSlot.tripleMatchChance", 0.09);
        for (int i=0;i<spins;i++) {
            if (random.nextDouble() < base) return true;
        }
        return false;
    }

    private void advanceStageOrJackpot(Player p, Machine m) {
        List<Map<?,?>> stages = plugin.getConfig().getMapList("stages");
        int idx = m.getStageIndex();
        if (idx >= stages.size()-1) {
            // Last stage -> Jackpot payout buffer
            int pay = 16 + random.nextInt(16);
            m.addPayout(pay);
            store.addWin(p.getName(), pay);
            Text.msg(p, "&6JACKPOT! &a보상 구슬 +" + pay + " &7(금블럭 우클릭으로 수령)");
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            return;
        }
        double chance = 0.1;
        Object ch = stages.get(idx).get("advanceChance");
        if (ch instanceof Number) chance = ((Number)ch).doubleValue();
        if (new Random().nextDouble() < chance) {
            m.setStageIndex(idx+1);
            Text.msg(p, "&b스테이지 상승! 현재 단계: &e" + stages.get(idx+1).get("name"));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        } else {
            Text.msg(p, "&7다음 단계 진입 실패. 현재 스테이지 유지.");
        }
    }
}
