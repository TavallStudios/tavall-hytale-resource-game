package com.tavall.hytale.resourcegame.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Resolved world-space focus target for explicit interaction commands and bot-friendly aim flows.
 */
public final class FocusedWorldTarget {
    private final FocusedWorldTargetType type;
    private final UUID nodeId;
    private final UUID buildingId;
    private final String label;
    private final double distance;
    private final double alignmentScore;

    public FocusedWorldTarget(
            FocusedWorldTargetType type,
            UUID nodeId,
            UUID buildingId,
            String label,
            double distance,
            double alignmentScore
    ) {
        this.type = Objects.requireNonNull(type, "type");
        this.nodeId = nodeId;
        this.buildingId = buildingId;
        this.label = Objects.requireNonNull(label, "label");
        this.distance = distance;
        this.alignmentScore = alignmentScore;
    }

    public static FocusedWorldTarget castle(double distance, double alignmentScore) {
        return new FocusedWorldTarget(FocusedWorldTargetType.CASTLE, null, null, "Castle", distance, alignmentScore);
    }

    public static FocusedWorldTarget resourceNode(UUID nodeId, String label, double distance, double alignmentScore) {
        return new FocusedWorldTarget(FocusedWorldTargetType.RESOURCE_NODE, nodeId, null, label, distance, alignmentScore);
    }

    public static FocusedWorldTarget building(UUID buildingId, String label, double distance, double alignmentScore) {
        return new FocusedWorldTarget(FocusedWorldTargetType.BUILDING, null, buildingId, label, distance, alignmentScore);
    }

    public FocusedWorldTargetType type() {
        return type;
    }

    public UUID nodeId() {
        return nodeId;
    }

    public UUID buildingId() {
        return buildingId;
    }

    public String label() {
        return label;
    }

    public double distance() {
        return distance;
    }

    public double alignmentScore() {
        return alignmentScore;
    }
}
