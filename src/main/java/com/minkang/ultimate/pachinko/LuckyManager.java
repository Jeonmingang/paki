
package com.minkang.ultimate.pachinko;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

public class LuckyManager {

    public static class Session{
        public final Player player;
        public int stage = 1;
        public int paidTotal = 0;
        public int cap;
        public int remaining(){ return Math.max(0, cap - paidTotal); }

        public Session(Player p, int cap){ this.player = p; this.cap = cap; }
    }

    private final Main plugin;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Random rnd = new Random();

    public LuckyManager(Main p){ this.plugin = p; }

    public Session getSession(Player p){
        Session s = sessions.get(p.getUniqueId());
        if (s==null){
            int baseCap = plugin.getConfig().getInt("cap.base", 64);
            s = new Session(p, baseCap);
            sessions.put(p.getUniqueId(), s);
        }
        return s;
    }

    public int maxStages(){
        java.util.List<?> list = plugin.getConfig().getList("stages");
        return list==null?1:list.size();
    }

    /** Called when jackpot occurs and marbles would be paid; centerHole indicates ball fell into center hole. */
    public void onJackpotAndBall(Player p, boolean centerHole){
        Session s = getSession(p);

        if (centerHole){
            // draw for stage advance instead of re-jackpot
            double chance = plugin.getConfig().getDouble("center-next-stage-chance", 0.5);
            if (rnd.nextDouble() < chance){
                advanceStage(s);
                return;
            }else{
                p.sendMessage("§7중앙 홀 도달! §c다음 스테이지 진입 실패...");
                try{ p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.8f);}catch(Exception ignored){}
            }
        }

        // pay normal reward for current stage
        payReward(s);
    }

    public void advanceStage(Session s){
        s.stage++;
        // increase CAP
        int inc = plugin.getConfig().getInt("cap.per-stage", 32);
        s.cap += Math.max(0, inc);
        plugin.announceStage(s.player, s.stage, maxStages());
        // celebratory sound
        try{ s.player.playSound(s.player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);}catch(Exception ignored){}
        sendStatus(s);
    }

    public void payReward(Session s){
        int min = plugin.getConfig().getInt("reward.min", 2);
        int max = plugin.getConfig().getInt("reward.max", 5);
        if (min>max){ int t = min; min = max; max = t; }
        int amount = min + rnd.nextInt(max - min + 1);
        // respect remaining cap
        amount = Math.min(amount, s.remaining());
        s.paidTotal += amount;

        s.player.sendMessage("§a구슬 지급: §f" + amount + " §7(누적 " + s.paidTotal + "/" + s.cap + ")");
        try{ s.player.playSound(s.player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);}catch(Exception ignored){}
        sendStatus(s);
    }

    private void sendStatus(Session s){
        String msg = plugin.getConfig().getString("messages.lucky-status",
                "&e{stage}/{max} &7단계 | &b지급합계: &f{paid} &7/ &b맥시멈: &f{cap} &7(남음 {remain})")
                .replace("{stage}", String.valueOf(s.stage))
                .replace("{max}", String.valueOf(maxStages()))
                .replace("{paid}", String.valueOf(s.paidTotal))
                .replace("{cap}", String.valueOf(s.cap))
                .replace("{remain}", String.valueOf(s.remaining()))
                .replace("&","§");
        s.player.sendMessage(msg);
    }
}
