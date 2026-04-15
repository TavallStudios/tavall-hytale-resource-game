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

    public ResourceNodeSummary(ResourceNodeData node, int availableTroops, int assignedTroops, int gainPerTick) {
        this.node = Objects.requireNonNull(node, "node");
        this.availableTroops = Math.max(0, availableTroops);
        this.assignedTroops = Math.max(0, assignedTroops);
        this.gainPerTick = Math.max(0, gainPerTick);
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
}
