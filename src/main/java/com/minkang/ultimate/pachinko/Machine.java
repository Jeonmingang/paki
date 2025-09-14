package com.minkang.ultimate.pachinko;

import org.bukkit.*;

import java.util.List;

public class Machine {
    public void updateSignLines(MachineRegistry reg){
        try{
            if (!reg.plugin().getConfig().getBoolean("structure.sign.enabled", true)) return;
            org.bukkit.Location sLoc = getSignLocation(reg);
            if (!(sLoc.getBlock().getState() instanceof org.bukkit.block.Sign)) return;
            org.bukkit.block.Sign sign = (org.bukkit.block.Sign) sLoc.getBlock().getState();
            sign.setLine(0, "§6[파칭코]");
            sign.setLine(1, "§7중앙: §f" + reg.plugin().getGlobalCenterHitPercent() + "%");
            sign.setLine(2, "§7잭팟: §f" + reg.plugin().getGlobalJackpotPercent() + "%");
            sign.setLine(3, "§7#"+id);
            sign.update();
        }catch(Throwable ignored){}
    }

    public int id;
    public Location base;
    public int rows;
    public int cols;

    // per-machine coin overrides
    public String ballItem;
    public String ballName;
    public java.util.List<String> ballLore;

    public Machine(int id, Location base, int rows, int cols){
        this(id, base, rows, cols, null, null, null);
    }
    public Machine(int id, Location base, int rows, int cols, String ballItem, String ballName, List<String> ballLore){
        this.id = id;
        this.base = base.clone();
        this.rows = rows;
        this.cols = cols;
        this.ballItem = ballItem;
        this.ballName = ballName;
        this.ballLore = (ballLore==null?null:new java.util.ArrayList<String>(ballLore));
    }

    private boolean autoBottom(MachineRegistry reg){
        return reg.plugin().getConfig().getBoolean("structure.auto-bottom-buttons", true);
    }

    public Location getStartBlockLocation(MachineRegistry reg){
        if (autoBottom(reg)){
            int mid = cols/2;
            return base.clone().add(mid+1, 0, 0);
        }
        int[] off = reg.cfgIntArray("structure.start-block.offset");
        return base.clone().add(off[0], off[1], off[2]);
    }
    public Material getStartBlockMaterial(MachineRegistry reg){
        String m = reg.cfgStr("structure.start-block.material");
        Material mat = Material.matchMaterial(m);
        return mat != null ? mat : Material.GOLD_BLOCK;
    }

    public Location getPayoutBlockLocation(MachineRegistry reg){
        if (autoBottom(reg)){
            int mid = cols/2;
            return base.clone().add(mid-1, 0, 0);
        }
        int[] off = reg.cfgIntArray("structure.payout-block.offset");
        return base.clone().add(off[0], off[1], off[2]);
    }
    public Material getPayoutBlockMaterial(MachineRegistry reg){
        String m = reg.cfgStr("structure.payout-block.material");
        Material mat = Material.matchMaterial(m);
        return mat != null ? mat : Material.DIAMOND_BLOCK;
    }
    public Location getPayoutHoleLocation(MachineRegistry reg){
        return getPayoutBlockLocation(reg).clone().add(0.5, 1.0, 0.0);
    }

    public Location getSignLocation(MachineRegistry reg){
        int[] off = reg.cfgIntArray("structure.sign.offset");
        return base.clone().add(off[0], off[1], off[2]);
    }

    public void build(MachineRegistry reg){
        World w = base.getWorld();
        Material glass = Material.matchMaterial(reg.cfgStr("structure.glass"));
        Material pin = Material.matchMaterial(reg.cfgStr("structure.pin"));
        if (glass==null) glass = Material.WHITE_STAINED_GLASS;
        if (pin==null) pin = Material.IRON_BARS;

        for (int x=0;x<cols;x++) w.getBlockAt(base.clone().add(x, rows+2, 0)).setType(glass, false);
        if (reg.plugin().getConfig().getBoolean("structure.top-hopper", true)){
            for (int x=0;x<cols;x++) w.getBlockAt(base.clone().add(x, rows+1, 0)).setType(Material.HOPPER, false);
        }
        for (int y=0;y<rows+1;y++){
            for (int x=0;x<cols;x++){
                w.getBlockAt(base.clone().add(x,y,0)).setType(glass, false);
            }
        }
        for (int y=1;y<rows;y++){
            for (int x=(y%2); x<cols; x+=2){
                w.getBlockAt(base.clone().add(x,y,0)).setType(pin, false);
            }
        }
        if (reg.plugin().getConfig().getBoolean("structure.deco-emerald", true)){
            int mid = cols/2;
            w.getBlockAt(base.clone().add(mid, rows+3, 0)).setType(Material.EMERALD_BLOCK, false);
        }
        getStartBlockLocation(reg).getBlock().setType(getStartBlockMaterial(reg), false);
        getPayoutBlockLocation(reg).getBlock().setType(getPayoutBlockMaterial(reg), false);

        // Sign
        if (reg.plugin().getConfig().getBoolean("structure.sign.enabled", true)){
            Material signMat = Material.matchMaterial(reg.cfgStr("structure.sign.type"));
            if (signMat == null) signMat = Material.OAK_SIGN;
            Location sLoc = getSignLocation(reg);
            sLoc.getBlock().setType(signMat, false);
            org.bukkit.block.Sign sign = (org.bukkit.block.Sign) sLoc.getBlock().getState();
            sign.setLine(0, "§6[파칭코]");
            sign.setLine(1, "§7중앙: §f" + reg.plugin().getGlobalCenterHitPercent() + "%");
            sign.setLine(2, "§7잭팟: §f" + reg.plugin().getGlobalJackpotPercent() + "%");
            sign.setLine(3, "§7#"+id);
            sign.update();
        }
    }

    public void clear(){
        World w = base.getWorld();
        for (int y=0;y<20;y++){
            for (int x=0;x<Math.max(12, cols+4);x++){
                w.getBlockAt(base.clone().add(x,y,0)).setType(Material.AIR, false);
            }
        }
    }
}
