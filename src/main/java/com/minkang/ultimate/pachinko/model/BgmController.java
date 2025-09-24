package com.minkang.ultimate.pachinko.model;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class BgmController {
    public static void stopAll(){
        for (Player p : Bukkit.getOnlinePlayers()){
            try { p.stopSound(SoundCategory.MUSIC); } catch (Throwable ignored){}
            try { p.stopSound(SoundCategory.RECORDS); } catch (Throwable ignored){}
            try { p.stopSound(SoundCategory.MASTER); } catch (Throwable ignored){}
        }
    }
    public static void playAll(String sound, float vol, float pitch){
        Sound s = null;
        try { s = Sound.valueOf(sound); } catch(Throwable ignored){}
        for (Player p : Bukkit.getOnlinePlayers()){
            if (s != null){
                p.playSound(p.getLocation(), s, vol, pitch);
            }else{
                try{ p.playSound(p.getLocation(), sound, SoundCategory.MASTER, vol, pitch); }catch(Throwable ignored){}
            }
        }
    }
}
