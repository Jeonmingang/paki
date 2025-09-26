
package com.example.pachinko.machine;

public class StageConfig {
    public int cup;
    public double upgradeChance;
    public int payoutBurst;
    public StageConfig(int cup, double upgradeChance, int payoutBurst){
        this.cup=cup; this.upgradeChance=upgradeChance; this.payoutBurst=payoutBurst;
    }
    @Override public String toString(){ return "Stage{cup="+cup+", upChance="+upgradeChance+", burst="+payoutBurst+"}"; }
}
