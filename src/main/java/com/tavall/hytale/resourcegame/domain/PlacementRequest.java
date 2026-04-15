package com.tavall.hytale.resourcegame.domain;

import com.tavall.hytale.resourcegame.resources.ResourceType;

import java.time.Instant;
import java.util.Objects;

/**
 * In-memory placement mode state for a player.
 */
public final class PlacementRequest {
    private final PlacementModeType modeType;
    private final ResourceType resourceType;
    private final String armedWorldName;
    private final Instant armedAt;

    public PlacementRequest(
            PlacementModeType modeType,
            ResourceType resourceType,
            String armedWorldName,
            Instant armedAt
    ) {
        this.modeType = Objects.requireNonNull(modeType, "modeType");
        this.resourceType = resourceType;
        this.armedWorldName = Objects.requireNonNull(armedWorldName, "armedWorldName");
        this.armedAt = Objects.requireNonNull(armedAt, "armedAt");
    }

    public PlacementModeType modeType() {
        return modeType;
    }

    public ResourceType resourceType() {
        return resourceType;
    }

    public String armedWorldName() {
        return armedWorldName;
    }

    public Instant armedAt() {
        return armedAt;
    }

    public String summary() {
        if (modeType == PlacementModeType.CASTLE) {
            return "Castle placement";
        }
        return resourceType == null
                ? "Resource node placement"
                : resourceType.name() + " node placement";
    }
}
