package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.math.vector.Vector3d;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleBuildingService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInteriorInstanceService;
import com.tavall.hytale.resourcegame.domain.BuildingType;
import com.tavall.hytale.resourcegame.domain.CastleBuildingData;
import com.tavall.hytale.resourcegame.domain.CastleBuildingSummary;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.interior.InteriorLayoutService;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Computes deterministic world-space staging anchors for building placement and inspection.
 */
public final class BuildingPlacementPlanner implements IDependencyInjectableConcrete {
    private final ICastleBuildingService buildingService;
    private final IInteriorInstanceService interiorInstanceService;
    private final InteriorLayoutService interiorLayoutService;

    public BuildingPlacementPlanner(
            ICastleBuildingService buildingService,
            IInteriorInstanceService interiorInstanceService,
            InteriorLayoutService interiorLayoutService
    ) {
        this.buildingService = Objects.requireNonNull(buildingService, "buildingService");
        this.interiorInstanceService = Objects.requireNonNull(interiorInstanceService, "interiorInstanceService");
        this.interiorLayoutService = Objects.requireNonNull(interiorLayoutService, "interiorLayoutService");
    }

    public String recommendedWorldName(UUID playerId, PlayerGameState state, BuildingType buildingType) {
        if (playerId == null || state == null || buildingType == null) {
            return null;
        }
        Optional<CastleBuildingData> existing = buildingService.resolveBuilding(state, buildingType.shortKey());
        if (existing.isPresent()) {
            return buildingService.summary(playerId, state, existing.get(), Instant.now()).worldName();
        }
        return switch (buildingType.areaType()) {
            case CASTLE_SURFACE -> state.castleLocation() == null ? null : state.castleLocation().worldName();
            case CASTLE_INTERIOR -> interiorInstanceService.worldNameFor(playerId);
        };
    }

    public Vector3d recommendedPosition(UUID playerId, PlayerGameState state, BuildingType buildingType) {
        if (playerId == null || state == null || buildingType == null) {
            return null;
        }
        Optional<CastleBuildingData> existing = buildingService.resolveBuilding(state, buildingType.shortKey());
        if (existing.isPresent()) {
            CastleBuildingSummary summary = buildingService.summary(playerId, state, existing.get(), Instant.now());
            return new Vector3d(summary.worldX(), summary.worldY(), summary.worldZ());
        }
        return switch (buildingType) {
            case FARMSTEAD -> surfaceOffset(state, 8.0D, 0.0D, 8.0D);
            case LUMBER_MILL -> surfaceOffset(state, -8.0D, 0.0D, 8.0D);
            case IRON_WORKS -> surfaceOffset(state, 8.0D, 0.0D, -8.0D);
            case BARRACKS -> interiorOffset(playerId, -5.0D, 0.0D, 0.0D);
            case WORKSHOP -> interiorOffset(playerId, 5.0D, 0.0D, 0.0D);
        };
    }

    private Vector3d surfaceOffset(PlayerGameState state, double offsetX, double offsetY, double offsetZ) {
        if (state.castleLocation() == null) {
            return null;
        }
        return new Vector3d(
                state.castleLocation().x() + offsetX,
                state.castleLocation().y() + offsetY,
                state.castleLocation().z() + offsetZ
        );
    }

    private Vector3d interiorOffset(UUID playerId, double offsetX, double offsetY, double offsetZ) {
        Vector3d origin = interiorLayoutService.originFor(playerId);
        return new Vector3d(
                origin.getX() + offsetX,
                origin.getY() + offsetY,
                origin.getZ() + offsetZ
        );
    }
}
