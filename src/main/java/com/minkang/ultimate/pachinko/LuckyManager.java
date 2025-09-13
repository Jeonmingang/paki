package com.minkang.ultimate.pachinko;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.util.*;

public class LuckyManager {
    private final Main plugin;
    private final Map<java.util.UUID, Session> sessions = new HashMap<>();

    static class Session {
        final Player player;
        final Machine machine;
        int remaining;
        int stage;
        Session(Player p, Machine m, int cap){ this.player=p; this.machine=m; this.remaining=cap; this.stage=1; }
    }

    public LuckyManager(Main p){ this.plugin = p; }

    public boolean isInLucky(Player p){ return sessions.containsKey(p.getUniqueId()); }

    public void startLucky(Player p, Machine m){
        if (!plugin.getConfig().getBoolean("lucky.enabled", true)) return;
        int cap = Math.max(1, plugin.getConfig().getInt("lucky.round-cap", 150));
        Session s = new Session(p, m, cap);
        sessions.put(p.getUniqueId(), s);

        // title + music
        try {
            String title = plugin.getConfig().getString("lucky.start-title","&dLUCKY TIME!").replace("&","§");
            p.sendTitle(title, "", 10, 60, 10);
            Sound mus = Sound.valueOf(plugin.getConfig().getString("lucky.music","MUSIC_DISC_CAT"));
            p.playSound(p.getLocation(), mus, 1.0f, 1.0f);
        } catch (Exception ignored){}
    }

    public boolean handleClick(Player p, Machine m){
        Session s = sessions.get(p.getUniqueId());
        if (s==null || s.machine != m) return false;
        if (s.remaining <= 0){
            // decide continuation
            int chance = plugin.getConfig().getInt("lucky.continue-chance", 40);
            int maxStages = plugin.getConfig().getInt("lucky.max-stages", 3);
            if (s.stage < maxStages && new java.util.Random().nextInt(100) < chance){
                s.stage++;
                s.remaining = Math.max(1, plugin.getConfig().getInt("lucky.round-cap", 150));
                try{
                    String title = plugin.getConfig().getString("lucky.continue-title","&b継続!").replace("&","§");
                    p.sendTitle(title, "", 10, 40, 10);
                }catch(Exception ignored){}
                return true;
            } else {
                sessions.remove(p.getUniqueId());
                p.sendMessage("§dLucky Time 종료!");
                return true;
            }
        }
        int perClick = Math.max(1, plugin.getConfig().getInt("lucky.click-gain", 3));
        int give = Math.min(perClick, s.remaining);
        s.remaining -= give;
        Material mat = plugin.getBallMaterial();
        Location mouth = m.getPayoutHoleLocation(plugin.getRegistry());
        for (int i=0;i<give;i++){
            p.getWorld().dropItemNaturally(mouth, new ItemStack(mat,1));
        }
        try{
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.8f);
        }catch(Exception ignored){}
        return true;
    }

    public void cancel(Player p){
        sessions.remove(p.getUniqueId());
    }
}
