package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;
import com.tavall.hytale.resourcegame.domain.BuildingType;
import com.tavall.hytale.resourcegame.domain.PlacementRequest;
import com.tavall.hytale.resourcegame.domain.PlacementResult;
import com.tavall.hytale.resourcegame.resources.ResourceType;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Tracks active world placement modes and confirms them from interaction or aim.
 */
public interface IPlacementModeService extends IDependencyInjectableInterface {
    PlacementRequest armCastlePlacement(Player player);

    PlacementRequest armNodePlacement(Player player, ResourceType resourceType);

    PlacementRequest armBuildingPlacement(Player player, BuildingType buildingType);

    PlacementRequest armBuildingPlacement(Player player, BuildingType buildingType, Vector3i stagedTargetBlock);

    Optional<PlacementRequest> activePlacement(UUID playerId);

    boolean hasActivePlacement(UUID playerId);

    PlacementResult cancelPlacement(UUID playerId);

    PlacementResult refreshPreview(Player player);

    PlacementResult confirmPlacement(Player player, Vector3i targetBlock);

    PlacementResult confirmPlacementFromAim(Player player);

    boolean shouldSuppressPrompts(UUID playerId, Instant now);
}
