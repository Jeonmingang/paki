package com.minkang.ultimate.pachinko;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Store {
    private final Map<UUID,Integer> balls = new HashMap<>();
    private final Map<UUID,Integer> medals = new HashMap<>();
    public int getBalls(UUID u){ return balls.getOrDefault(u,0); }
    public int getMedals(UUID u){ return medals.getOrDefault(u,0); }
    public void addBalls(UUID u,int n){ balls.put(u, getBalls(u)+n); }
    public boolean takeBall(UUID u){
        int cur = getBalls(u);
        boolean ok = cur>0;
        if (!ok) return false;
        balls.put(u, cur-1);
        return true;
    }
    public void addMedal(UUID u,int n){ medals.put(u, getMedals(u)+n); }
    public boolean takeMedals(UUID u,int n){
        int cur = getMedals(u);
        boolean ok = cur>=n;
        if (!ok) return false;
        medals.put(u, cur-n);
        return true;
    }
}