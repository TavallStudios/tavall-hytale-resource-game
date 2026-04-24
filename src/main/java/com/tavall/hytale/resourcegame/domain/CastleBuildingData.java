package com.tavall.hytale.resourcegame.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Persisted building placement and construction state anchored to the castle surface or interior.
 */
public final class CastleBuildingData {
    private final UUID buildingId;
    private final BuildingType buildingType;
    private final double relativeX;
    private final double relativeY;
    private final double relativeZ;
    private final int currentLevel;
    private final int targetLevel;
    private final Instant placedAt;
    private final Instant constructionStartedAt;
    private final Instant constructionEndsAt;

    public CastleBuildingData(
            UUID buildingId,
            BuildingType buildingType,
            double relativeX,
            double relativeY,
            double relativeZ,
            int currentLevel,
            int targetLevel,
            Instant placedAt,
            Instant constructionStartedAt,
            Instant constructionEndsAt
    ) {
        this.buildingId = Objects.requireNonNull(buildingId, "buildingId");
        this.buildingType = Objects.requireNonNull(buildingType, "buildingType");
        this.relativeX = relativeX;
        this.relativeY = relativeY;
        this.relativeZ = relativeZ;
        this.currentLevel = Math.max(0, currentLevel);
        this.targetLevel = Math.max(this.currentLevel, targetLevel);
        this.placedAt = Objects.requireNonNull(placedAt, "placedAt");
        this.constructionStartedAt = constructionStartedAt;
        this.constructionEndsAt = constructionEndsAt;
    }

    public UUID buildingId() {
        return buildingId;
    }

    public BuildingType buildingType() {
        return buildingType;
    }

    public BuildingAreaType areaType() {
        return buildingType.areaType();
    }

    public double relativeX() {
        return relativeX;
    }

    public double relativeY() {
        return relativeY;
    }

    public double relativeZ() {
        return relativeZ;
    }

    public int currentLevel() {
        return currentLevel;
    }

    public int targetLevel() {
        return targetLevel;
    }

    public Instant placedAt() {
        return placedAt;
    }

    public Instant constructionStartedAt() {
        return constructionStartedAt;
    }

    public Instant constructionEndsAt() {
        return constructionEndsAt;
    }

    @JsonIgnore
    public boolean isUnderConstruction() {
        return constructionStartedAt != null && constructionEndsAt != null && targetLevel > currentLevel;
    }

    @JsonIgnore
    public boolean canUpgrade() {
        return !isUnderConstruction() && currentLevel < buildingType.maxLevel();
    }

    public CastleBuildingData withConstruction(int targetLevel, Instant startedAt, Instant endsAt) {
        return new CastleBuildingData(
                buildingId,
                buildingType,
                relativeX,
                relativeY,
                relativeZ,
                currentLevel,
                targetLevel,
                placedAt,
                startedAt,
                endsAt
        );
    }

    public CastleBuildingData completeConstruction(Instant completedAt) {
        Instant safeCompletedAt = completedAt == null ? constructionEndsAt : completedAt;
        return new CastleBuildingData(
                buildingId,
                buildingType,
                relativeX,
                relativeY,
                relativeZ,
                targetLevel,
                targetLevel,
                placedAt,
                null,
                safeCompletedAt
        );
    }

    public CastleBuildingData cancelConstruction() {
        return new CastleBuildingData(
                buildingId,
                buildingType,
                relativeX,
                relativeY,
                relativeZ,
                currentLevel,
                currentLevel,
                placedAt,
                null,
                null
        );
    }

    public CastleBuildingData withRelativePosition(double relativeX, double relativeY, double relativeZ) {
        return new CastleBuildingData(
                buildingId,
                buildingType,
                relativeX,
                relativeY,
                relativeZ,
                currentLevel,
                targetLevel,
                placedAt,
                constructionStartedAt,
                constructionEndsAt
        );
    }
}
