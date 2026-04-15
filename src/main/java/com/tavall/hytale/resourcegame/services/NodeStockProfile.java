package com.tavall.hytale.resourcegame.services;

/**
 * Immutable stock defaults for a resource node type.
 */
public final class NodeStockProfile {
    private final int maxStock;
    private final int regenerationPerTick;

    public NodeStockProfile(int maxStock, int regenerationPerTick) {
        this.maxStock = Math.max(0, maxStock);
        this.regenerationPerTick = Math.max(0, regenerationPerTick);
    }

    public int maxStock() {
        return maxStock;
    }

    public int regenerationPerTick() {
        return regenerationPerTick;
    }
}
