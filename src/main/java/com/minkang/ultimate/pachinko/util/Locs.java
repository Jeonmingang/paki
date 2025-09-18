package com.minkang.ultimate.pachinko.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class Locs {
    private Locs(){}
    public static String toString(Location l) {
        if (l == null) return null;
        return l.getWorld().getName() + "," + l.getX() + "," + l.getY() + "," + l.getZ() + "," + l.getYaw() + "," + l.getPitch();
    }
    public static Location fromString(String s) {
        if (s == null) return null;
        try {
            String[] a = s.split(",");
            World w = Bukkit.getWorld(a[0]);
            double x = Double.parseDouble(a[1]);
            double y = Double.parseDouble(a[2]);
            double z = Double.parseDouble(a[3]);
            float yaw = Float.parseFloat(a[4]);
            float pitch = Float.parseFloat(a[5]);
            return new Location(w, x, y, z, yaw, pitch);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[UltimatePachinko] Bad location: " + s);
            return null;
        }
    }
}
