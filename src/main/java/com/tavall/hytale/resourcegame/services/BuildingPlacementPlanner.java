package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.math.vector.Vector3d;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleBuildingService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInteriorInstanceService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerGameStateService;
import com.tavall.hytale.resourcegame.domain.BuildingType;
import com.tavall.hytale.resourcegame.domain.CastleBuildingData;
import com.tavall.hytale.resourcegame.domain.CastleBuildingSummary;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.interior.InteriorLayout;
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
    private final IPlayerGameStateService gameStateService;
    private final InteriorLayoutService interiorLayoutService;

    public BuildingPlacementPlanner(
            ICastleBuildingService buildingService,
            IInteriorInstanceService interiorInstanceService,
            IPlayerGameStateService gameStateService,
            InteriorLayoutService interiorLayoutService
    ) {
        this.buildingService = Objects.requireNonNull(buildingService, "buildingService");
        this.interiorInstanceService = Objects.requireNonNull(interiorInstanceService, "interiorInstanceService");
        this.gameStateService = Objects.requireNonNull(gameStateService, "gameStateService");
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
        return switch (buildingType.areaType()) {
            case CASTLE_SURFACE -> surfaceOffset(state, 8.0D, 0.0D, 8.0D);
            case CASTLE_INTERIOR -> interiorBuildingAnchor(state, buildingType);
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

    private Vector3d interiorOffset(PlayerGameState state, double offsetX, double offsetY, double offsetZ) {
        int interiorIndex = gameStateService.interiorInstanceIndex(state);
        Vector3d origin = interiorLayoutService.originForCastle(state.castleLocation(), interiorIndex);
        return new Vector3d(
                origin.getX() + offsetX,
                origin.getY() + offsetY,
                origin.getZ() + offsetZ
        );
    }

    private Vector3d interiorBuildingAnchor(PlayerGameState state, BuildingType buildingType) {
        int interiorIndex = gameStateService.interiorInstanceIndex(state);
        InteriorLayout layout = interiorLayoutService.createLayoutForCastle(state.castleLocation(), interiorIndex);
        return layout.buildingAnchor(buildingType);
    }
}
