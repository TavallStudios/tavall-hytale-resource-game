package com.tavall.hytale.resourcegame.domain;

/**
 * Aggregated metadata for troop combat capabilities.
 */
public final class TroopMetaData {
    private final double combatMedian;
    private final double disciplineMedian;
    private final double moraleMedian;

    public TroopMetaData(double combatMedian, double disciplineMedian, double moraleMedian) {
        this.combatMedian = combatMedian;
        this.disciplineMedian = disciplineMedian;
        this.moraleMedian = moraleMedian;
    }

    public double combatMedian() {
        return combatMedian;
    }

    public double disciplineMedian() {
        return disciplineMedian;
    }

    public double moraleMedian() {
        return moraleMedian;
    }

    /**
     * Returns current aggregate military power. Until per-tier troop counts are persisted,
     * every stored troop is treated as tier 1 and therefore contributes 1 Might.
     */
    public int estimatedMight(int troopCount) {
        return Math.max(0, troopCount);
    }

    public static TroopMetaData defaults() {
        return new TroopMetaData(0.3, 0.3, 0.6);
    }
}
