package com.minkang.ultimate.pachinko.model;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class BgmController {
    private static volatile String lastSoundName;
    private static volatile Sound lastSoundEnum;

    public static void stopAll(){
        for (Player p : Bukkit.getOnlinePlayers()){
            try {
                if (lastSoundEnum != null) {
                    p.stopSound(lastSoundEnum);
                    try { p.stopSound(lastSoundEnum, SoundCategory.MUSIC); } catch (Throwable ignored) {}
                    try { p.stopSound(lastSoundEnum, SoundCategory.RECORDS); } catch (Throwable ignored) {}
                    try { p.stopSound(lastSoundEnum, SoundCategory.MASTER); } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
            try {
                if (lastSoundName != null) {
                    p.stopSound(lastSoundName);
                    try { p.stopSound(lastSoundName, SoundCategory.MUSIC); } catch (Throwable ignored) {}
                    try { p.stopSound(lastSoundName, SoundCategory.RECORDS); } catch (Throwable ignored) {}
                    try { p.stopSound(lastSoundName, SoundCategory.MASTER); } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
        }
    }
    public static void playAll(String sound, float vol, float pitch){
        Sound s = null;
        try { s = Sound.valueOf(sound); } catch(Throwable ignored){}
        lastSoundEnum = s;
        lastSoundName = sound;
        for (Player p : Bukkit.getOnlinePlayers()){
            if (s != null){
                p.playSound(p.getLocation(), s, vol, pitch);
            }else{
                try{ p.playSound(p.getLocation(), sound, SoundCategory.MASTER, vol, pitch); }catch(Throwable ex){
                    try{ p.playSound(p.getLocation(), sound, vol, pitch); }catch(Throwable ignored){}
                }
            }
        }
    }
}
