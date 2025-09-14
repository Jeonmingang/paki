
package com.minkang.ultimate.pachinko;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

public class Machine {
    public int id;
    public Location base;
    public int rows;
    public int cols;

    public void build(){
        // Build a frame similar to the screenshot:
        //  - Top header: coal blocks spanning width, center diamond block
        //  - Under header: a line of hoppers facing DOWN
        //  - Glass grid with iron bars as pins
        //  - Bottom right sign with id
        World w = base.getWorld();
        int width = Math.max(cols + 2, 9); // a little margin
        int height = rows + 6;
        // Clear area
        for (int y=0; y<=height; y++){
            for (int x=0; x<=width; x++){
                w.getBlockAt(base.clone().add(x, y, 0)).setType(Material.AIR, false);
            }
        }
        // Header
        int topY = height;
        for (int x=0; x<=width; x++){
            Block b = w.getBlockAt(base.clone().add(x, topY, 0));
            b.setType(Material.COAL_BLOCK, false);
        }
        int midX = width/2;
        w.getBlockAt(base.clone().add(midX, topY, 0)).setType(Material.DIAMOND_BLOCK, false);

        // Hopper row
        for (int x=0; x<=width; x++){
            Block b = w.getBlockAt(base.clone().add(x, topY-1, 0));
            b.setType(Material.HOPPER, false);
        }

        // Glass panel and pins (iron bars)
        int left = 1;
        int innerWidth = width-2;
        for (int y=2; y<height-1; y++){
            for (int x=left; x<left+innerWidth; x++){
                w.getBlockAt(base.clone().add(x, y, 0)).setType(Material.GLASS, false);
            }
        }
        // vertical pins every ~(innerWidth/cols)
        double step = (double)innerWidth/(double)cols;
        for (int c=0; c<cols; c++){
            int px = left + (int)Math.round(step * (c+0.5));
            for (int y=2; y<height-1; y+=2){
                w.getBlockAt(base.clone().add(px, y, 0)).setType(Material.IRON_BARS, false);
            }
        }

        // Base glass
        for (int x=0; x<=width; x++){
            w.getBlockAt(base.clone().add(x, 0, 0)).setType(Material.GLASS, false);
        }
        // Gold block center-bottom to mimic screenshot
        w.getBlockAt(base.clone().add(midX, 0, 0)).setType(Material.GOLD_BLOCK, false);

        // Sign bottom-right (adjustable)
        int offX = Main.get().getConfig().getInt("structure.sign-offset-x", -1);
        int offY = Main.get().getConfig().getInt("structure.sign-offset-y", 1);
        int offZ = Main.get().getConfig().getInt("structure.sign-offset-z", 0);
        Block signBlock = w.getBlockAt(base.clone().add(width + offX, 1 + offY, 0 + offZ));
        signBlock.setType(Material.OAK_WALL_SIGN, false);
        if (signBlock.getState() instanceof Sign){
            Sign s = (Sign)signBlock.getState();
            s.setLine(0, "§6[파칭코]");
            s.setLine(1, "§fID: §a#" + id);
            s.setLine(2, "§f슬롯: §b" + cols);
            s.setLine(3, "§7우클릭");
            s.update();
        }
    }

    
    public void updateSign(MachineRegistry reg){
        if (!reg.plugin().getConfig().getBoolean("structure.sign.enabled", true)) return;
        Location sLoc = getSignLocation(reg);
        if (sLoc.getBlock().getType().name().endsWith("_SIGN")){
            try{
                org.bukkit.block.Sign sign = (org.bukkit.block.Sign) sLoc.getBlock().getState();
                sign.setLine(0, "§6[파칭코]");
                sign.setLine(1, "§7중앙: §f" + reg.plugin().getGlobalCenterHitPercent() + "%");
                sign.setLine(2, "§7잭팟: §f" + reg.plugin().getGlobalJackpotPercent() + "%");
                sign.setLine(3, "§7#"+id);
                sign.update();
            }catch(Exception ignored){}
        }
    }
    public void clear(){
        World w = base.getWorld();
        for (int y=0; y< rows + 10; y++){
            for (int x=0; x< Math.max(cols + 6, 12); x++){
                w.getBlockAt(base.clone().add(x, y, 0)).setType(Material.AIR, false);
            }
        }
    }
}
