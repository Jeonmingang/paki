
package com.minkang.ultimate.pachinko.model;

public class Stage {
    private final String name;
    private final int cup;
    private final double advanceChance;

    public Stage(String name, int cup, double advanceChance) {
        this.name = name;
        this.cup = cup;
        this.advanceChance = advanceChance;
    }

    public String getName() { return name; }
    public int getCup() { return cup; }
    public double getAdvanceChance() { return advanceChance; }
}
