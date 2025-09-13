package com.minkang.ultimate.pachinko;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Directional;

public class Machine {
    public final int id;
    public final Location base;

    public Machine(int id, Location base){
        this.id = id;
        this.base = base.clone();
    }

    public Location getStartButtonLocation(MachineRegistry reg){
        int[] off = reg.cfgIntArray("structure.start-button-offset");
        return base.clone().add(off[0], off[1], off[2]);
    }
    public Location getMouthLocation(MachineRegistry reg){
        int[] off = reg.cfgIntArray("structure.mouth-offset");
        return base.clone().add(off[0], off[1], off[2]).add(0.5,0.5,0);
    }

    public void build(MachineRegistry reg){
        World w = base.getWorld();
        int rows = reg.cfg("machine.rows");
        int cols = reg.cfg("machine.cols");
        Material glass = Material.matchMaterial(reg.cfgStr("structure.glass"));
        Material pin = Material.matchMaterial(reg.cfgStr("structure.pin"));
        if (glass==null) glass = Material.WHITE_STAINED_GLASS;
        if (pin==null) pin = Material.IRON_BARS;

        for (int y=0;y<rows+2;y++){
            for (int x=0;x<cols;x++){
                w.getBlockAt(base.clone().add(x,y,0)).setType(glass, false);
            }
        }
        for (int y=1;y<rows;y++){
            for (int x=(y%2); x<cols; x+=2){
                w.getBlockAt(base.clone().add(x,y,0)).setType(pin, false);
            }
        }
        Location btn = getStartButtonLocation(reg);
        Block b = w.getBlockAt(btn);
        b.setType(Material.STONE_BUTTON, false);
        if (b.getBlockData() instanceof Directional){
            Directional d = (Directional)b.getBlockData();
            d.setFacing(BlockFace.SOUTH);
            b.setBlockData(d, false);
        }
    }

    public void clear(){
        World w = base.getWorld();
        for (int y=0;y<12;y++){
            for (int x=0;x<9;x++){
                w.getBlockAt(base.clone().add(x,y,0)).setType(Material.AIR, false);
            }
        }
    }
}
