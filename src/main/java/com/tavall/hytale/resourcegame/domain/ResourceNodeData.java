package com.tavall.hytale.resourcegame.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private final int currentStock;
    private final int maxStock;
    private final int regenerationPerTick;
    private final Instant placedAt;
    private final Instant expiresAt;

    @JsonCreator
    public ResourceNodeData(
            @JsonProperty("nodeId") UUID nodeId,
            @JsonProperty("resourceType") ResourceType resourceType,
            @JsonProperty("worldName") String worldName,
            @JsonProperty("x") double x,
            @JsonProperty("y") double y,
            @JsonProperty("z") double z,
            @JsonProperty("assignedTroops") int assignedTroops,
            @JsonProperty("currentStock") int currentStock,
            @JsonProperty("maxStock") int maxStock,
            @JsonProperty("regenerationPerTick") int regenerationPerTick,
            @JsonProperty("placedAt") Instant placedAt,
            @JsonProperty("expiresAt") Instant expiresAt
    ) {
        this.nodeId = Objects.requireNonNull(nodeId, "nodeId");
        this.resourceType = Objects.requireNonNull(resourceType, "resourceType");
        this.worldName = Objects.requireNonNull(worldName, "worldName");
        this.x = x;
        this.y = y;
        this.z = z;
        this.assignedTroops = Math.max(0, assignedTroops);
        this.maxStock = Math.max(0, maxStock);
        this.currentStock = Math.max(0, Math.min(currentStock, this.maxStock));
        this.regenerationPerTick = Math.max(0, regenerationPerTick);
        this.placedAt = Objects.requireNonNull(placedAt, "placedAt");
        this.expiresAt = expiresAt;
    }

    public ResourceNodeData(
            UUID nodeId,
            ResourceType resourceType,
            String worldName,
            double x,
            double y,
            double z,
            int assignedTroops,
            int currentStock,
            int maxStock,
            int regenerationPerTick,
            Instant placedAt
    ) {
        this(nodeId, resourceType, worldName, x, y, z, assignedTroops, currentStock, maxStock, regenerationPerTick, placedAt, null);
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

    public int currentStock() {
        return currentStock;
    }

    public int maxStock() {
        return maxStock;
    }

    public int regenerationPerTick() {
        return regenerationPerTick;
    }

    public Instant placedAt() {
        return placedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public CastleLocationData location() {
        return new CastleLocationData(worldName, x, y, z);
    }

    public ResourceNodeData withAssignedTroops(int assignedTroops) {
        return new ResourceNodeData(nodeId, resourceType, worldName, x, y, z, assignedTroops, currentStock, maxStock, regenerationPerTick, placedAt, expiresAt);
    }

    public ResourceNodeData withCurrentStock(int currentStock) {
        return new ResourceNodeData(nodeId, resourceType, worldName, x, y, z, assignedTroops, currentStock, maxStock, regenerationPerTick, placedAt, expiresAt);
    }

    public ResourceNodeData withStockProfile(int currentStock, int maxStock, int regenerationPerTick) {
        return new ResourceNodeData(nodeId, resourceType, worldName, x, y, z, assignedTroops, currentStock, maxStock, regenerationPerTick, placedAt, expiresAt);
    }

    public ResourceNodeData withLifetime(Instant expiresAt) {
        return new ResourceNodeData(nodeId, resourceType, worldName, x, y, z, assignedTroops, currentStock, maxStock, regenerationPerTick, placedAt, expiresAt);
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && now != null && !expiresAt.isAfter(now);
    }

    public long remainingLifetimeSeconds(Instant now) {
        if (expiresAt == null || now == null) {
            return -1L;
        }
        return Math.max(0L, expiresAt.getEpochSecond() - now.getEpochSecond());
    }
}
