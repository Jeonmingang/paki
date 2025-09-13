package com.minkang.ultimate.pachinko;

import org.bukkit.*;

public class Machine {
    public final int id;
    public final Location base;
    public final int rows;
    public final int cols;

    public Machine(int id, Location base, int rows, int cols){
        this.id = id;
        this.base = base.clone();
        this.rows = rows;
        this.cols = cols;
    }

    public Location getStartBlockLocation(MachineRegistry reg){
        int[] off = reg.cfgIntArray("structure.start-block.offset");
        return base.clone().add(off[0], off[1], off[2]);
    }
    public Material getStartBlockMaterial(MachineRegistry reg){
        String m = reg.cfgStr("structure.start-block.material");
        Material mat = Material.matchMaterial(m);
        return mat != null ? mat : Material.GOLD_BLOCK;
    }
    public Location getPayoutHoleLocation(MachineRegistry reg){
        int[] off = reg.cfgIntArray("structure.payout-hole-offset");
        return base.clone().add(off[0], off[1], off[2]).add(0.5,0.5,0);
    }

    public void build(MachineRegistry reg){
        World w = base.getWorld();
        Material glass = Material.matchMaterial(reg.cfgStr("structure.glass"));
        Material pin = Material.matchMaterial(reg.cfgStr("structure.pin"));
        if (glass==null) glass = Material.WHITE_STAINED_GLASS;
        if (pin==null) pin = Material.IRON_BARS;

        // Top cap glass line (photo-style)
        for (int x=0;x<cols;x++) w.getBlockAt(base.clone().add(x, rows+1, 0)).setType(glass, false);
        // Hopper line
        if (reg.plugin().getConfig().getBoolean("structure.top-hopper", true)){
            for (int x=0;x<cols;x++) w.getBlockAt(base.clone().add(x, rows, 0)).setType(Material.HOPPER, false);
        }
        // Board glass
        for (int y=0;y<rows+1;y++){
            for (int x=0;x<cols;x++){
                w.getBlockAt(base.clone().add(x,y,0)).setType(glass, false);
            }
        }
        // Checker pins
        for (int y=1;y<rows;y++){
            for (int x=(y%2); x<cols; x+=2){
                w.getBlockAt(base.clone().add(x,y,0)).setType(pin, false);
            }
        }
        // Emerald deco on the top cap middle
        if (reg.plugin().getConfig().getBoolean("structure.deco-emerald", true)){
            int mid = cols/2;
            w.getBlockAt(base.clone().add(mid, rows+2, 0)).setType(Material.EMERALD_BLOCK, false);
        }
        // Start block
        getStartBlockLocation(reg).getBlock().setType(getStartBlockMaterial(reg), false);
    }

    public void clear(){
        World w = base.getWorld();
        for (int y=0;y<20;y++){
            for (int x=0;x<Math.max(11, cols+4);x++){
                w.getBlockAt(base.clone().add(x,y,0)).setType(Material.AIR, false);
            }
        }
    }
}
