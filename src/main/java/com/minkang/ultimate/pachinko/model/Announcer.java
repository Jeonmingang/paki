package com.minkang.ultimate.pachinko.model;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.util.Text;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

public class Announcer {

    public enum Type { ENTRY, UP, CLEAR }

    public static void announce(Main plugin, Machine m, Player actor, Type type){
        int id = m.getId();
        int stage = m.getStage();
        int cup = m.getStageCup();
        String stageName = getStageName(plugin, stage);

        // 1) Chat (stage-specific template > global)
        String chat = resolveTemplate(plugin, stage, type, "chat");
        if (chat != null && !chat.isEmpty()){
            String out = replace(chat, actor, id, stage, cup, stageName);
            Bukkit.broadcastMessage(Text.color(out));
        }

        // 2) Title/Subtitle (server-wide 여부)
        boolean serverTitle = getServerTitle(plugin, type);
        String title = resolveTemplate(plugin, stage, type, "title");
        String subtitle = resolveTemplate(plugin, stage, type, "subtitle");
        int fi = plugin.getConfig().getInt("announce.title.fadeIn", 5);
        int st = plugin.getConfig().getInt("announce.title.stay", 40);
        int fo = plugin.getConfig().getInt("announce.title.fadeOut", 10);
        if (title != null || subtitle != null){
            if (serverTitle){
                for (Player p : Bukkit.getOnlinePlayers()){
                    safeTitle(p, replace(title, actor, id, stage, cup, stageName), replace(subtitle, actor, id, stage, cup, stageName), fi, st, fo);
                }
            }else{
                for (Player p : actor.getWorld().getPlayers()){
                    if (p.getLocation().distanceSquared(m.getBase()) <= 40*40){
                        safeTitle(p, replace(title, actor, id, stage, cup, stageName), replace(subtitle, actor, id, stage, cup, stageName), fi, st, fo);
                    }
                }
            }
        }

        // 3) Sound
        String soundSpec = resolveSound(plugin, stage, type);
        playSoundToAll(soundSpec, m.getBase());

        // 4) Fireworks / Spark
        int fw = resolveFireworks(plugin, stage, type);
        if (fw > 0){
            spawnFireworks(m.getBase(), fw);
        } else {
            try { m.getBase().getWorld().spawnParticle(Particle.FIREWORKS_SPARK, m.getBase().clone().add(0.5,1.2,0.5), 25, 1, 0.5, 1, 0.01); } catch(Throwable ignored){}
        }
    }

    private static String getStageName(Main plugin, int stage){
        java.util.List<java.util.Map<String,Object>> stages = (java.util.List<java.util.Map<String,Object>>) (java.util.List<?>) plugin.getConfig().getMapList("stages");
        if (stages != null && stage >= 0 && stage < stages.size()){
            Object v = stages.get(stage).get("name");
            if (v != null) return String.valueOf(v);
        }
        return "&7(알수없음)";
    }

    private static String replace(String s, Player actor, int id, int stage, int cup, String stageName){
        if (s == null) return null;
        return s.replace("%player%", actor.getName())
                .replace("%id%", String.valueOf(id))
                .replace("%stage%", String.valueOf(stage))
                .replace("%stageName%", stageName)
                .replace("%cup%", String.valueOf(cup));
    }

    private static boolean getServerTitle(Main plugin, Type t){
        String key = "announce.entry.serverTitle";
        if (t == Type.UP) key = "announce.up.serverTitle";
        else if (t == Type.CLEAR) key = "announce.clear.serverTitle";
        return plugin.getConfig().getBoolean(key, true);
    }

    private static String resolveTemplate(Main plugin, int stage, Type t, String field){
        // stage-level override: stages[stage].announce.<field>
        java.util.List<java.util.Map<String,Object>> stages = (java.util.List<java.util.Map<String,Object>>) (java.util.List<?>) plugin.getConfig().getMapList("stages");
        if (stages != null && stage >= 0 && stage < stages.size()){
            Object an = stages.get(stage).get("announce");
            if (an instanceof java.util.Map){
                java.util.Map<?,?> amap = (java.util.Map<?,?>) an;
                Object v = amap.get(field);
                if (v != null) return String.valueOf(v);
            }
        }
        // global fallback: announce.templates.<type>.<field>
        String path = "announce.templates.entry."+field;
        if (t == Type.UP) path = "announce.templates.up."+field;
        else if (t == Type.CLEAR) path = "announce.templates.clear."+field;
        return plugin.getConfig().getString(path, null);
    }

    private static String resolveSound(Main plugin, int stage, Type t){
        // stage-level override: stages[stage].announce.sound
        java.util.List<java.util.Map<String,Object>> stages = (java.util.List<java.util.Map<String,Object>>) (java.util.List<?>) plugin.getConfig().getMapList("stages");
        if (stages != null && stage >= 0 && stage < stages.size()){
            Object an = stages.get(stage).get("announce");
            if (an instanceof java.util.Map){
                Object v = ((java.util.Map<?,?>)an).get("sound");
                if (v != null) return String.valueOf(v);
            }
        }
        String key = "announce.entry.sound";
        if (t == Type.UP) key = "announce.up.sound";
        else if (t == Type.CLEAR) key = "announce.clear.sound";
        return plugin.getConfig().getString(key, "UI_TOAST_CHALLENGE_COMPLETE,1.0,1.0");
    }

    private static int resolveFireworks(Main plugin, int stage, Type t){
        java.util.List<java.util.Map<String,Object>> stages = (java.util.List<java.util.Map<String,Object>>) (java.util.List<?>) plugin.getConfig().getMapList("stages");
        if (stages != null && stage >= 0 && stage < stages.size()){
            Object an = stages.get(stage).get("announce");
            if (an instanceof java.util.Map){
                Object v = ((java.util.Map<?,?>)an).get("fireworks");
                if (v instanceof Number) return ((Number)v).intValue();
                if (v != null) try { return Integer.parseInt(String.valueOf(v)); } catch(Throwable ignored){}
            }
        }
        String key = "announce.entry.fireworks";
        if (t == Type.UP) key = "announce.up.fireworks";
        else if (t == Type.CLEAR) key = "announce.clear.fireworks";
        return Main.get().getConfig().getInt(key, 2);
    }

    private static void safeTitle(Player p, String title, String sub, int fi, int st, int fo){
        try { p.sendTitle(Text.color(title==null?"":title), Text.color(sub==null?"":sub), fi, st, fo); } catch(Throwable ignored){}
    }

    private static void playSoundToAll(String spec, Location center){
        if (spec == null || spec.isEmpty()) return;
        String[] sp = spec.split(",");
        String name = sp[0].trim();
        float vol = 1.0f, pit = 1.0f;
        if (sp.length>=2) try{ vol = Float.parseFloat(sp[1].trim()); }catch(Throwable ignored){}
        if (sp.length>=3) try{ pit = Float.parseFloat(sp[2].trim()); }catch(Throwable ignored){}
        Sound s = null;
        try{ s = Sound.valueOf(name); }catch(Throwable ignored){}
        for (Player p : Bukkit.getOnlinePlayers()){
            if (s != null) p.playSound(p.getLocation(), s, vol, pit);
            else {
                try { p.playSound(p.getLocation(), name, SoundCategory.MASTER, vol, pit); }
                catch(Throwable ex){ try { p.playSound(p.getLocation(), name, vol, pit); }catch(Throwable ignored){} }
            }
        }
    }

    private static void spawnFireworks(Location base, int count){
        World w = base.getWorld(); if (w == null) return;
        for (int i=0;i<count;i++){
            try{
                Firework fw = w.spawn(base.clone().add(0.5, 1.2, 0.5), Firework.class);
                FireworkMeta meta = fw.getFireworkMeta();
                meta.setPower(0);
                meta.addEffect(FireworkEffect.builder()
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .withColor(Color.AQUA, Color.WHITE)
                        .withFade(Color.SILVER)
                        .trail(true).flicker(true).build());
                fw.setFireworkMeta(meta);
                final Firework fwr = fw;
                Bukkit.getScheduler().runTaskLater(Main.get(), () -> {
                    try { fwr.detonate(); } catch(Throwable ignored){}
                }, 10L);
            }catch(Throwable ignored){}
        }
        try { w.spawnParticle(Particle.FIREWORKS_SPARK, base.clone().add(0.5,1.2,0.5), 80, 1, 0.5, 1, 0.02); } catch(Throwable ignored){}
    }
}
