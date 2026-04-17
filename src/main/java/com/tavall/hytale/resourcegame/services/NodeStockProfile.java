package com.tavall.hytale.resourcegame.services;

/**
 * Immutable stock defaults for a resource node type.
 */
public final class NodeStockProfile {
    private final int maxStock;
    private final int regenerationPerTick;
    private final long lifetimeSeconds;

    public NodeStockProfile(int maxStock, int regenerationPerTick) {
        this(maxStock, regenerationPerTick, 0L);
    }

    public NodeStockProfile(int maxStock, int regenerationPerTick, long lifetimeSeconds) {
        this.maxStock = Math.max(0, maxStock);
        this.regenerationPerTick = Math.max(0, regenerationPerTick);
        this.lifetimeSeconds = Math.max(0L, lifetimeSeconds);
    }

    public int maxStock() {
        return maxStock;
    }

    public int regenerationPerTick() {
        return regenerationPerTick;
    }

    public long lifetimeSeconds() {
        return lifetimeSeconds;
    }
}
