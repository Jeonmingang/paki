package com.example.pachinko.model;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class Machine {

    private final int id;
    private final Location leftBase;   // 석탄
    private final Location midBase;    // 금
    private final Location rightBase;  // 다이아
    private final Location[] topHoppers; // 좌/중/우 상단 호퍼 위치
    private ItemStack exclusiveBall;

    private UUID occupant;
    private long lastActionAt;

    // 진행 상태
    private int drawTokens = 0; // 0~maxTokens
    private int stage = 0; // 0 = no stage, 1..N
    private int cupProduced = 0;
    private boolean advancedThisStage = false;

    // 보호
    private final Set<Integer> runningTasks = new HashSet<>();

    public Machine(int id, Location leftBase, Location midBase, Location rightBase, Location[] topHoppers) {
        this.id = id;
        this.leftBase = leftBase;
        this.midBase = midBase;
        this.rightBase = rightBase;
        this.topHoppers = topHoppers;
    }

    public int getId() { return id; }
    public Location getLeftBase() { return leftBase; }
    public Location getMidBase() { return midBase; }
    public Location getRightBase() { return rightBase; }

    public Location getTopHopper(int lane) { return topHoppers[Math.max(0, Math.min(2, lane))]; }

    public ItemStack getExclusiveBall() { return exclusiveBall; }
    public void setExclusiveBall(ItemStack stack) { this.exclusiveBall = stack; }

    public UUID getOccupant() { return occupant; }
    public void setOccupant(UUID occ) { this.occupant = occ; touch(); }
    public boolean isOccupied() { return occupant != null; }
    public void release() { this.occupant = null; this.drawTokens = 0; this.stage = 0; this.cupProduced = 0; this.advancedThisStage = false; }

    public void touch() { this.lastActionAt = System.currentTimeMillis(); }
    public long getLastActionAt() { return lastActionAt; }

    public int getDrawTokens() { return drawTokens; }
    public void addDrawToken(int maxTokens) { this.drawTokens = Math.min(maxTokens, drawTokens + 1); }
    public void consumeDrawToken() { if (drawTokens > 0) drawTokens--; }
    public int getStage() { return stage; }
    public void enterStage(int stageId) { this.stage = stageId; this.cupProduced = 0; this.advancedThisStage = false; }
    public void leaveStage() { this.stage = 0; this.cupProduced = 0; this.advancedThisStage = false; }
    public int getCupProduced() { return cupProduced; }
    public void incCupProduced() { this.cupProduced++; }
    public boolean isAdvancedThisStage() { return advancedThisStage; }
    public void setAdvancedThisStage(boolean val) { this.advancedThisStage = val; }

    public Set<Integer> getRunningTasks() { return runningTasks; }
}
