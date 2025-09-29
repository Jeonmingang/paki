package com.minkang.ultimate.pachinko.model;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Machine {
    private final String id;
    private Location coalBase;   // 좌(석탄)
    private Location goldBase;   // 중(금)
    private Location diamondBase;// 우(다이아)
    private Location hopperLeft; // 상단 좌
    private Location hopperCenter; // 상단 중
    private Location hopperRight; // 상단 우
    private ItemStack exclusiveBall;
    private UUID occupier;
    private long lastInteract;
    private int stage; // 0 = idle
    private int tokens; // 0..max
    private int stageDrops; // 스테이지 동안 드롭한 수
    private int promotions; // 이번 스테이지 승급 성공 횟수

    public Machine(String id) { this.id = id; }

    public String getId() { return id; }

    public Location getCoalBase() { return coalBase; }
    public void setCoalBase(Location coalBase) { this.coalBase = coalBase; }
    public Location getGoldBase() { return goldBase; }
    public void setGoldBase(Location goldBase) { this.goldBase = goldBase; }
    public Location getDiamondBase() { return diamondBase; }
    public void setDiamondBase(Location diamondBase) { this.diamondBase = diamondBase; }
    public Location getHopperLeft() { return hopperLeft; }
    public void setHopperLeft(Location hopperLeft) { this.hopperLeft = hopperLeft; }
    public Location getHopperCenter() { return hopperCenter; }
    public void setHopperCenter(Location hopperCenter) { this.hopperCenter = hopperCenter; }
    public Location getHopperRight() { return hopperRight; }
    public void setHopperRight(Location hopperRight) { this.hopperRight = hopperRight; }

    public ItemStack getExclusiveBall() { return exclusiveBall; }
    public void setExclusiveBall(ItemStack exclusiveBall) { this.exclusiveBall = exclusiveBall; }

    public UUID getOccupier() { return occupier; }
    public void setOccupier(UUID occupier) { this.occupier = occupier; }
    public long getLastInteract() { return lastInteract; }
    public void setLastInteract(long lastInteract) { this.lastInteract = lastInteract; }

    public int getStage() { return stage; }
    public void setStage(int stage) {
        this.stage = stage;
        this.stageDrops = 0;
        this.promotions = 0;
    }
    public int getTokens() { return tokens; }
    public void setTokens(int tokens) { this.tokens = tokens; }
    public void addToken(int delta) { this.tokens += delta; }
    public int getStageDrops() { return stageDrops; }
    public void addStageDrops(int d) { this.stageDrops += d; }
    public int getPromotions() { return promotions; }
    public void addPromotion() { this.promotions++; }
}
