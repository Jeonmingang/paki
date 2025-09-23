
package com.minkang.ultimate.pachinko.model;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Machine {
    private final int id;
    private final Location base;
    private int[] weights = new int[]{14,14,14,6,14,14,14};
    private Location goldButton, diamondBlock, coalButton;
    private int stageIndex = 0;
    private int pendingSpins = 0;
    private int pendingPayout = 0;
    private int missStreak = 0;
    private int feverSpinsLeft = 0;
    private boolean drawingNow = false;
    private ItemStack machineBallItem; // null이면 전역 defaultBall 사용

    public Machine(int id, Location base){ this.id=id; this.base=base; }
    public int getId(){ return id; }
    public Location getBase(){ return base; }
    public int[] getWeights(){ return weights; }
    public void setWeights(int[] w){ this.weights = w; }
    public Location getGoldButton(){ return goldButton; }
    public void setGoldButton(Location l){ goldButton=l; }
    public Location getDiamondBlock(){ return diamondBlock; }
    public void setDiamondBlock(Location l){ diamondBlock=l; }
    public Location getCoalButton(){ return coalButton; }
    public void setCoalButton(Location l){ coalButton=l; }

    public int getStageIndex(){ return stageIndex; }
    public void setStageIndex(int s){ stageIndex = s; }
    public int getPendingSpins(){ return pendingSpins; }
    public void addPendingSpin(int max){ if (pendingSpins<max) pendingSpins++; }
    public int takeSpinsAll(){ int s=pendingSpins; pendingSpins=0; return s; }
    public boolean consumeOneSpin(){ if (pendingSpins>0){ pendingSpins--; return true; } return false; }
    public boolean takeOneSpin(){ if (pendingSpins>0){ pendingSpins--; return true; } return false; }

    public int getPendingPayout(){ return pendingPayout; }
    public void addPayout(int v, int cap){
        if (v<=0) return;
        int allow = Math.max(0, cap - pendingPayout);
        pendingPayout += Math.min(allow, v);
    }
    public ItemStack getMachineBallItem(){ return machineBallItem; }
    public void setMachineBallItem(ItemStack it){ machineBallItem = it; }

    public boolean isDrawingNow(){ return drawingNow; }
    public void setDrawingNow(boolean b){ this.drawingNow = b; }
}


    public int getMissStreak(){ return missStreak; }
    public void resetMissStreak(){ missStreak = 0; }
    public void incMissStreak(){ missStreak++; }
    public int getFeverSpinsLeft(){ return feverSpinsLeft; }
    public void setFeverSpinsLeft(int v){ feverSpinsLeft = v; }
    public boolean inFever(){ return feverSpinsLeft > 0; }
    public void decFever(){ if (feverSpinsLeft>0) feverSpinsLeft--; }
