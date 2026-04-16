package com.tavall.hytale.resourcegame.domain;

import com.hypixel.hytale.math.vector.Vector3i;
import com.tavall.hytale.resourcegame.resources.ResourceType;

import java.time.Instant;
import java.util.Objects;

/**
 * In-memory placement mode state for a player.
 */
public final class PlacementRequest {
    private final PlacementModeType modeType;
    private final ResourceType resourceType;
    private final BuildingType buildingType;
    private final String armedWorldName;
    private final Instant armedAt;
    private final Vector3i stagedTargetBlock;

    public PlacementRequest(
            PlacementModeType modeType,
            ResourceType resourceType,
            BuildingType buildingType,
            String armedWorldName,
            Instant armedAt
    ) {
        this(modeType, resourceType, buildingType, armedWorldName, armedAt, null);
    }

    public PlacementRequest(
            PlacementModeType modeType,
            ResourceType resourceType,
            BuildingType buildingType,
            String armedWorldName,
            Instant armedAt,
            Vector3i stagedTargetBlock
    ) {
        this.modeType = Objects.requireNonNull(modeType, "modeType");
        this.resourceType = resourceType;
        this.buildingType = buildingType;
        this.armedWorldName = Objects.requireNonNull(armedWorldName, "armedWorldName");
        this.armedAt = Objects.requireNonNull(armedAt, "armedAt");
        this.stagedTargetBlock = stagedTargetBlock;
    }

    public PlacementModeType modeType() {
        return modeType;
    }

    public ResourceType resourceType() {
        return resourceType;
    }

    public BuildingType buildingType() {
        return buildingType;
    }

    public String armedWorldName() {
        return armedWorldName;
    }

    public Instant armedAt() {
        return armedAt;
    }

    public Vector3i stagedTargetBlock() {
        return stagedTargetBlock;
    }

    public String summary() {
        if (modeType == PlacementModeType.CASTLE) {
            return "Castle placement";
        }
        if (modeType == PlacementModeType.BUILDING) {
            return buildingType == null
                    ? "Building placement"
                    : buildingType.displayName() + " placement";
        }
        return resourceType == null
                ? "Resource node placement"
                : resourceType.name() + " node placement";
    }
}
