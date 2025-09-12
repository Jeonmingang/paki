package com.minkang.ultimate.pachinko;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import java.util.ArrayList;
import java.util.List;

public class MachineRegistry {
    private final Plugin plugin;
    private final List<Location> bases = new ArrayList<>();
    public MachineRegistry(Plugin p){ this.plugin = p; }

    public void add(Location base){ bases.add(base.clone()); }
    public Location nearest(World w, Location from){
        Location best = null; double bd = Double.MAX_VALUE;
        for (Location b : bases){
            boolean ok = b.getWorld().equals(w);
            if (!ok) continue;
            double d = b.distanceSquared(from);
            boolean better = d < bd;
            if (!better) continue;
            bd = d; best = b;
        }
        return best;
    }
}