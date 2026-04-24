package com.tavall.hytale.resourcegame.domain;

import java.util.Objects;

/**
 * Read model for resource-node UI and command output.
 */
public final class ResourceNodeSummary {
    private final ResourceNodeData node;
    private final int availableTroops;
    private final int assignedTroops;
    private final int assignedWorkers;
    private final int gainPerTick;
    private final int troopGainPerTick;
    private final int workerGainPerTick;
    private final int pillageReward;
    private final int currentStock;
    private final int maxStock;
    private final int regenerationPerTick;
    private final int stockPercent;
    private final int visibleRouteCount;
    private final String stockStatus;

    public ResourceNodeSummary(
            ResourceNodeData node,
            int availableTroops,
            int assignedTroops,
            int gainPerTick,
            int currentStock,
            int maxStock,
            int regenerationPerTick,
            int stockPercent,
            int visibleRouteCount,
            String stockStatus
    ) {
        this(node, availableTroops, assignedTroops, 0, gainPerTick, gainPerTick, 0, 0, currentStock, maxStock, regenerationPerTick, stockPercent, visibleRouteCount, stockStatus);
    }

    public ResourceNodeSummary(
            ResourceNodeData node,
            int availableTroops,
            int assignedTroops,
            int assignedWorkers,
            int gainPerTick,
            int troopGainPerTick,
            int workerGainPerTick,
            int pillageReward,
            int currentStock,
            int maxStock,
            int regenerationPerTick,
            int stockPercent,
            int visibleRouteCount,
            String stockStatus
    ) {
        this.node = Objects.requireNonNull(node, "node");
        this.availableTroops = Math.max(0, availableTroops);
        this.assignedTroops = Math.max(0, assignedTroops);
        this.assignedWorkers = Math.max(0, assignedWorkers);
        this.gainPerTick = Math.max(0, gainPerTick);
        this.troopGainPerTick = Math.max(0, troopGainPerTick);
        this.workerGainPerTick = Math.max(0, workerGainPerTick);
        this.pillageReward = Math.max(0, pillageReward);
        this.currentStock = Math.max(0, currentStock);
        this.maxStock = Math.max(0, maxStock);
        this.regenerationPerTick = Math.max(0, regenerationPerTick);
        this.stockPercent = Math.max(0, stockPercent);
        this.visibleRouteCount = Math.max(0, visibleRouteCount);
        this.stockStatus = Objects.requireNonNull(stockStatus, "stockStatus");
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

    public int assignedWorkers() {
        return assignedWorkers;
    }

    public int gainPerTick() {
        return gainPerTick;
    }

    public int troopGainPerTick() {
        return troopGainPerTick;
    }

    public int workerGainPerTick() {
        return workerGainPerTick;
    }

    public int pillageReward() {
        return pillageReward;
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

    public String stockStatus() {
        return stockStatus;
    }
}
