
package com.minkang.ultimate.pachinko.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class Locs {
    private Locs(){}
    public static String toString(Location l){
        if (l == null) return null;
        return l.getWorld().getName()+","+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ();
        }
    public static Location fromString(String s){
        if (s == null) return null;
        String[] sp = s.split(",");
        World w = Bukkit.getWorld(sp[0]);
        return new Location(w, Integer.parseInt(sp[1]), Integer.parseInt(sp[2]), Integer.parseInt(sp[3]));
    }
}
