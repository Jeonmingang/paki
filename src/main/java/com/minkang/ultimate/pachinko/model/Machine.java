
package com.minkang.ultimate.pachinko.model;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Machine {
    private final int id;
    private final Location base;
    private ItemStack ballItem;
    private int[] weights = new int[]{14,14,14,6,14,14,14};
    private List<Location> hopperLocs = new ArrayList<>();
    private Location goldButton, diamondBlock, coalButton;

    private boolean inStage = false;
    private int stageIndex = -1;
    private int pendingSpins = 0;
    private int pendingPayout = 0;
    private int stagePayout = 0;
    private boolean drawingNow = false;

    public Machine(int id, Location base){ this.id = id; this.base = base.clone(); }

    public int getId(){ return id; }
    public Location getBase(){ return base; }
    public ItemStack getBallItem(){ return ballItem; }
    public void setBallItem(ItemStack it){ this.ballItem = it; }
    public int[] getWeights(){ return weights; }
    public void setWeights(int[] w){ if (w != null && w.length == 7) this.weights = w; }
    public List<Location> getHopperLocs(){ return hopperLocs; }
    public void setHopperLocs(List<Location> l){ this.hopperLocs = l; }
    public Location getGoldButton(){ return goldButton; }
    public void setGoldButton(Location l){ this.goldButton = l; }
    public Location getDiamondBlock(){ return diamondBlock; }
    public void setDiamondBlock(Location l){ this.diamondBlock = l; }
    public Location getCoalButton(){ return coalButton; }
    public void setCoalButton(Location l){ this.coalButton = l; }

    public boolean isInStage(){ return inStage; }
    public void setInStage(boolean b){ this.inStage = b; }
    public int getStageIndex(){ return stageIndex; }
    public void setStageIndex(int i){ this.stageIndex = i; }
    public int getPendingSpins(){ return pendingSpins; }
    public void setPendingSpins(int p){ this.pendingSpins = Math.max(0,p); }
    public void addPendingSpin(int max){ if (pendingSpins < max) pendingSpins++; }
    public void resetPendingSpins(){ pendingSpins = 0; }

    public int getPendingPayout(){ return pendingPayout; }
    public int getStagePayout(){ return stagePayout; }
    public void addPayout(int amount){ pendingPayout += Math.max(0, amount); }
    public int takePayoutAll(){ int a = pendingPayout; pendingPayout = 0; return a; }

    public boolean isDrawingNow(){ return drawingNow; }
    public void setDrawingNow(boolean b){ drawingNow = b; }
}
