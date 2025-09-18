package com.minkang.ultimate.pachinko.model;

import org.bukkit.Location;
import org.bukkit.block.Hopper;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Machine {
    private final int id;
    private final Location base;
    private ItemStack ballItem;
    private int[] weights = new int[]{14,14,14,6,14,14,14};
    private List<Location> hopperLocations = new ArrayList<>(7);
    private Location goldButton;
    private int stageIndex = 0;
    private int pendingSpins = 0;
    private int pendingPayout = 0;
    private int currentPayout = 0;
    private boolean drawingNow = false;

    public Machine(int id, Location base) { this.id = id; this.base = base.clone(); }

    public int getId() { return id; }
    public Location getBase() { return base.clone(); }
    public ItemStack getBallItem() { return ballItem; }
    public void setBallItem(ItemStack it) { this.ballItem = it; }
    public int[] getWeights() { return weights; }
    public void setWeights(int[] w) { if (w != null && w.length == 7) this.weights = w; }
    public List<Location> getHopperLocations() { return hopperLocations; }
    public void setHopperLocations(List<Location> list) { this.hopperLocations = list; }
    public Location getGoldButton() { return goldButton; }
    public void setGoldButton(Location l) { this.goldButton = l; }
    public int getStageIndex() { return stageIndex; }
    public void setStageIndex(int i) { this.stageIndex = i; }
    public int getPendingSpins() { return pendingSpins; }
    public void setPendingSpins(int p) { this.pendingSpins = p; }
    public void addPendingSpin(int max) { if (pendingSpins < max) pendingSpins++; }
    public void resetPendingSpins() { pendingSpins = 0; }
    public int getPendingPayout() { return pendingPayout; }
    public void addPayout(int n) { pendingPayout += n; currentPayout += n; }
    public int takePayoutAll() { int v = pendingPayout; pendingPayout = 0; return v; }
    public int getCurrentPayout() { return currentPayout; }
    public void resetCurrentPayout() { currentPayout = 0; }
    public boolean isDrawingNow(){ return drawingNow; }
    public void setDrawingNow(boolean b){ this.drawingNow = b; }

    public org.bukkit.inventory.Inventory getHopperInv(int slotIndexOneBased) {
        if (slotIndexOneBased < 1 || slotIndexOneBased > 7) return null;
        Location hl = hopperLocations.get(slotIndexOneBased - 1);
        Block b = hl.getBlock();
        if (b.getState() instanceof Hopper) {
            return ((Hopper)b.getState()).getInventory();
        }
        return null;
    }
}
