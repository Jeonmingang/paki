
package com.example.pachinko.machine;

import java.util.UUID;

public class PlayerState {
    public final UUID uuid;
    public int stageIndex = 0;
    public int payoutProgress = 0;
    public boolean payoutComplete = false;
    public int tokens = 0;
    public long lastInteract = 0L;
    public PlayerState(UUID uuid){ this.uuid=uuid; }
}
