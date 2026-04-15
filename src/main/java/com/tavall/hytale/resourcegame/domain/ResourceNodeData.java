package com.tavall.hytale.resourcegame.domain;

import com.tavall.hytale.resourcegame.resources.ResourceType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Persisted resource node placement and troop assignment state.
 */
public final class ResourceNodeData {
    private final UUID nodeId;
    private final ResourceType resourceType;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final int assignedTroops;
    private final Instant placedAt;

    public ResourceNodeData(
            UUID nodeId,
            ResourceType resourceType,
            String worldName,
            double x,
            double y,
            double z,
            int assignedTroops,
            Instant placedAt
    ) {
        this.nodeId = Objects.requireNonNull(nodeId, "nodeId");
        this.resourceType = Objects.requireNonNull(resourceType, "resourceType");
        this.worldName = Objects.requireNonNull(worldName, "worldName");
        this.x = x;
        this.y = y;
        this.z = z;
        this.assignedTroops = Math.max(0, assignedTroops);
        this.placedAt = Objects.requireNonNull(placedAt, "placedAt");
    }

    public UUID nodeId() {
        return nodeId;
    }

    public ResourceType resourceType() {
        return resourceType;
    }

    public String worldName() {
        return worldName;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public int assignedTroops() {
        return assignedTroops;
    }

    public Instant placedAt() {
        return placedAt;
    }

    public CastleLocationData location() {
        return new CastleLocationData(worldName, x, y, z);
    }

    public ResourceNodeData withAssignedTroops(int assignedTroops) {
        return new ResourceNodeData(nodeId, resourceType, worldName, x, y, z, assignedTroops, placedAt);
    }
}
