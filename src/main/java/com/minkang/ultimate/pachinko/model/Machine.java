package com.minkang.ultimate.pachinko.model;

import org.bukkit.Location;
import java.util.UUID;

public class Machine {
    private final String id;
    private final Location base;

    // ---- stage / runtime fields ----
    private boolean stageActive = false;
    private int stageIndex = 0;
    private int stageCup = 0;
    private int stagePayout = 0;
    private String stageOwner = "";
    private String lastBgm = "";
    private int autoTaskId = -1;

    // coin/spin state (simple)
    private int pendingSpins = 0;

    public Machine(String id, Location base) {
        this.id = id;
        this.base = base;
    }

    public String getId() { return id; }
    public Location getBase() { return base; }

    public boolean isStageActive() { return stageActive; }
    public void setStageActive(boolean stageActive) { this.stageActive = stageActive; }

    public int getStageIndex() { return stageIndex; }
    public void setStageIndex(int stageIndex) { this.stageIndex = stageIndex; }

    public int getStageCup() { return stageCup; }
    public void setStageCup(int stageCup) { this.stageCup = stageCup; }

    public int getStagePayout() { return stagePayout; }
    public void setStagePayout(int stagePayout) { this.stagePayout = stagePayout; }

    public String getStageOwner() { return stageOwner; }
    public void setStageOwner(String stageOwner) { this.stageOwner = stageOwner; }

    public String getLastBgm() { return lastBgm; }
    public void setLastBgm(String lastBgm) { this.lastBgm = lastBgm; }

    public int getAutoTaskId() { return autoTaskId; }
    public void setAutoTaskId(int autoTaskId) { this.autoTaskId = autoTaskId; }

    public int getPendingSpins() { return pendingSpins; }
    public void setPendingSpins(int pendingSpins) { this.pendingSpins = Math.max(0, pendingSpins); }
    public boolean consumeOneSpin() {
        if (pendingSpins > 0) {
            pendingSpins--;
            return true;
        }
        return false;
    }
}