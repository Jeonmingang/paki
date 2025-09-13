package com.minkang.ultimate.pachinko;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.util.*;

public class LuckyManager {
    private final Main plugin;
    private final Map<java.util.UUID, Session> sessions = new HashMap<>();
    private final Random rnd = new Random();

    static class Session {
        final Player player;
        final Machine machine;
        int remaining;
        int stage;
        int cap;
        int paidTotal;
        Session(Player p, Machine m, int cap){
            this.player=p; this.machine=m; this.cap=cap;
            this.remaining=cap; this.stage=1; this.paidTotal=0;
        }
    }

    public LuckyManager(Main p){ this.plugin = p; }

    public boolean isInLucky(Player p){ return sessions.containsKey(p.getUniqueId()); }

    private String subStatus(Session s){
        String sub = plugin.getConfig().getString("lucky.title-sub-status","&e스테이지 {stage}/{max}  &f{paid}/{cap}");
        return sub.replace("{stage}", String.valueOf(s.stage))
                .replace("{max}", String.valueOf(plugin.getConfig().getInt("lucky.max-stages",3)))
                .replace("{paid}", String.valueOf(s.paidTotal))
                .replace("{cap}", String.valueOf(s.cap))
                .replace("&","§");
    }

    public void startLucky(Player p, Machine m){
        if (!plugin.getConfig().getBoolean("lucky.enabled", true)) return;
        int cap = Math.max(1, plugin.getConfig().getInt("lucky.round-cap", 150));
        Session s = new Session(p, m, cap);
        sessions.put(p.getUniqueId(), s);

        // title + music
        try {
            String title = plugin.getConfig().getString("lucky.title-start","&d럭키 타임!").replace("&","§");
            p.sendTitle(title, subStatus(s), 10, 60, 10);
            Sound mus = Sound.valueOf(plugin.getConfig().getString("lucky.music","MUSIC_DISC_CAT"));
            p.playSound(p.getLocation(), mus, 1.0f, 1.0f);
        } catch (Exception ignored){}
        sendStatus(s);
    }

    public boolean handleClick(Player p, Machine m){
        Session s = sessions.get(p.getUniqueId());
        if (s==null || s.machine != m) return false;
        if (s.remaining <= 0){
            stepContinueOrEnd(s);
            return true;
        }
        int perClick = Math.max(1, plugin.getConfig().getInt("lucky.click-gain", 3));
        int give = Math.min(perClick, s.remaining);
        s.remaining -= give;
        s.paidTotal += give;
        Material mat = plugin.getBallMaterial();
        Location mouth = s.machine.getPayoutHoleLocation(plugin.getRegistry());
        for (int i=0;i<give;i++){
            p.getWorld().dropItemNaturally(mouth, new ItemStack(mat,1));
        }
        try{
            p.playSound(p.getLocation(), Sound.valueOf(plugin.getConfig().getString("effects.lucky-click-sound","BLOCK_NOTE_BLOCK_PLING")), 0.6f, 1.8f);
        }catch(Exception ignored){}
        sendStatus(s);
        if (s.remaining<=0) stepContinueOrEnd(s);
        return true;
    }

    private void stepContinueOrEnd(Session s){
        Player p = s.player;
        int chance = plugin.getConfig().getInt("lucky.continue-chance", 40);
        int maxStages = plugin.getConfig().getInt("lucky.max-stages", 3);
        if (s.stage < maxStages && rnd.nextInt(100) < chance){
            s.stage++;
            s.remaining = s.cap = Math.max(1, plugin.getConfig().getInt("lucky.round-cap", 150));
            try{
                String title = plugin.getConfig().getString("lucky.title-continue","&b계속!").replace("&","§");
                p.sendTitle(title, subStatus(s), 10, 40, 10);
                p.playSound(p.getLocation(), Sound.valueOf(plugin.getConfig().getString("effects.continue-sound","ENTITY_EXPERIENCE_ORB_PICKUP")), 1.0f, 1.2f);
            }catch(Exception ignored){}
            sendStatus(s);
        } else {
            sessions.remove(p.getUniqueId());
            p.sendMessage(plugin.getConfig().getString("messages.lucky-finish").replace("{total}", String.valueOf(s.paidTotal)).replace("&","§"));
        }
    }

    public void onCenterDuringLucky(Player p, Machine m){
        Session s = sessions.get(p.getUniqueId());
        if (s==null || s.machine != m) return;
        boolean capUp = plugin.getConfig().getInt("lucky.cap-increase-on-center",0) > 0;
        int add = plugin.getConfig().getInt("lucky.cap-increase-on-center",0);
        if (capUp){
            s.cap += add;
            s.remaining += add;
            try{
                String title = plugin.getConfig().getString("lucky.title-capup","&a천장 +{amount}!").replace("{amount}", String.valueOf(add)).replace("&","§");
                p.sendTitle(title, subStatus(s), 5, 30, 5);
                p.playSound(p.getLocation(), Sound.valueOf(plugin.getConfig().getString("effects.capup-sound","BLOCK_PISTON_EXTEND")), 1.0f, 0.9f);
            }catch(Exception ignored){}
        }
        int cont = plugin.getConfig().getInt("lucky.center-hit-continue-chance",0);
        int maxStages = plugin.getConfig().getInt("lucky.max-stages", 3);
        if (s.stage < maxStages && cont>0 && rnd.nextInt(100)<cont){
            s.stage++;
            try{
                String title = plugin.getConfig().getString("lucky.title-stageup","&6스테이지 업!").replace("&","§");
                p.sendTitle(title, subStatus(s), 10, 40, 10);
                p.playSound(p.getLocation(), Sound.valueOf(plugin.getConfig().getString("effects.stageup-sound","ENTITY_PLAYER_LEVELUP")), 1.0f, 1.1f);
            }catch(Exception ignored){}
        }
        sendStatus(s);
    }

    private void sendStatus(Session s){
        String msg = plugin.getConfig().getString("messages.lucky-status")
                .replace("{stage}", String.valueOf(s.stage))
                .replace("{max}", String.valueOf(plugin.getConfig().getInt("lucky.max-stages",3)))
                .replace("{paid}", String.valueOf(s.paidTotal))
                .replace("{cap}", String.valueOf(s.cap))
                .replace("{remain}", String.valueOf(s.remaining))
                .replace("&","§");
        s.player.sendMessage(msg);
        // actionbar style using Title with shorter times (1.16 API: sendTitle used as a hacky action bar substitute)
        s.player.sendTitle("", msg, 0, 10, 0);
    }
}
