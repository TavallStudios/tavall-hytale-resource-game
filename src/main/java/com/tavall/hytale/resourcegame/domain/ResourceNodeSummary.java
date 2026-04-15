package com.tavall.hytale.resourcegame.domain;

import java.util.Objects;

/**
 * Read model for resource-node UI and command output.
 */
public final class ResourceNodeSummary {
    private final ResourceNodeData node;
    private final int availableTroops;
    private final int assignedTroops;
    private final int gainPerTick;
    private final int currentStock;
    private final int maxStock;
    private final int regenerationPerTick;
    private final int stockPercent;
    private final int visibleRouteCount;

    public ResourceNodeSummary(
            ResourceNodeData node,
            int availableTroops,
            int assignedTroops,
            int gainPerTick,
            int currentStock,
            int maxStock,
            int regenerationPerTick,
            int stockPercent,
            int visibleRouteCount
    ) {
        this.node = Objects.requireNonNull(node, "node");
        this.availableTroops = Math.max(0, availableTroops);
        this.assignedTroops = Math.max(0, assignedTroops);
        this.gainPerTick = Math.max(0, gainPerTick);
        this.currentStock = Math.max(0, currentStock);
        this.maxStock = Math.max(0, maxStock);
        this.regenerationPerTick = Math.max(0, regenerationPerTick);
        this.stockPercent = Math.max(0, stockPercent);
        this.visibleRouteCount = Math.max(0, visibleRouteCount);
    }

    public ResourceNodeData node() {
        return node;
    }

    public int availableTroops() {
        return availableTroops;
    }

    public int assignedTroops() {
        return assignedTroops;
    }

    public int gainPerTick() {
        return gainPerTick;
    }

    public int currentStock() {
        return currentStock;
    }

    public int maxStock() {
        return maxStock;
    }

    public int regenerationPerTick() {
        return regenerationPerTick;
    }

    public int stockPercent() {
        return stockPercent;
    }

    public int visibleRouteCount() {
        return visibleRouteCount;
    }
}
