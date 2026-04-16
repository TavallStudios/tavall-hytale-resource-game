package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleBuildingService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleBuildingVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastlePlacementService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlacementModeService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlacementPreviewService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerSessionStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeVisualService;
import com.tavall.hytale.resourcegame.domain.BuildingMutationResult;
import com.tavall.hytale.resourcegame.domain.BuildingType;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.PlacementModeType;
import com.tavall.hytale.resourcegame.domain.PlacementRequest;
import com.tavall.hytale.resourcegame.domain.PlacementResult;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.ResourceNodeData;
import com.tavall.hytale.resourcegame.resources.ResourceType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Duration;

/**
 * Maintains armed placement state and finalizes placements from world interactions.
 */
public final class PlacementModeService implements IPlacementModeService, IDependencyInjectableConcrete {
    private static final double TARGET_DISTANCE = 12.0D;
    private static final Duration PROMPT_SUPPRESSION_WINDOW = Duration.ofSeconds(4);

    private final IPlayerSessionStore sessionStore;
    private final IPlacementPreviewService previewService;
    private final ICastleBuildingService buildingService;
    private final ICastleBuildingVisualService buildingVisualService;
    private final ICastlePlacementService castlePlacementService;
    private final IResourceNodeService resourceNodeService;
    private final IResourceNodeVisualService resourceNodeVisualService;
    private final Map<UUID, PlacementRequest> activeRequests = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> recentPlacementActivity = new ConcurrentHashMap<>();

    public PlacementModeService(
            IPlayerSessionStore sessionStore,
            IPlacementPreviewService previewService,
            ICastleBuildingService buildingService,
            ICastleBuildingVisualService buildingVisualService,
            ICastlePlacementService castlePlacementService,
            IResourceNodeService resourceNodeService,
            IResourceNodeVisualService resourceNodeVisualService
    ) {
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.previewService = Objects.requireNonNull(previewService, "previewService");
        this.buildingService = Objects.requireNonNull(buildingService, "buildingService");
        this.buildingVisualService = Objects.requireNonNull(buildingVisualService, "buildingVisualService");
        this.castlePlacementService = Objects.requireNonNull(castlePlacementService, "castlePlacementService");
        this.resourceNodeService = Objects.requireNonNull(resourceNodeService, "resourceNodeService");
        this.resourceNodeVisualService = Objects.requireNonNull(resourceNodeVisualService, "resourceNodeVisualService");
    }

    @Override
    public PlacementRequest armCastlePlacement(Player player) {
        PlacementRequest request = new PlacementRequest(
                PlacementModeType.CASTLE,
                null,
                null,
                player.getWorld().getName(),
                Instant.now()
        );
        activeRequests.put(player.getUuid(), request);
        refreshPreview(player);
        return request;
    }

    @Override
    public PlacementRequest armNodePlacement(Player player, ResourceType resourceType) {
        PlacementRequest request = new PlacementRequest(
                PlacementModeType.RESOURCE_NODE,
                resourceType,
                null,
                player.getWorld().getName(),
                Instant.now()
        );
        activeRequests.put(player.getUuid(), request);
        refreshPreview(player);
        return request;
    }

    @Override
    public PlacementRequest armBuildingPlacement(Player player, BuildingType buildingType) {
        return armBuildingPlacement(player, buildingType, null);
    }

    @Override
    public PlacementRequest armBuildingPlacement(Player player, BuildingType buildingType, Vector3i stagedTargetBlock) {
        PlacementRequest request = new PlacementRequest(
                PlacementModeType.BUILDING,
                null,
                buildingType,
                player.getWorld().getName(),
                Instant.now(),
                stagedTargetBlock
        );
        activeRequests.put(player.getUuid(), request);
        refreshPreview(player);
        return request;
    }

    @Override
    public Optional<PlacementRequest> activePlacement(UUID playerId) {
        return Optional.ofNullable(activeRequests.get(playerId));
    }

    @Override
    public boolean hasActivePlacement(UUID playerId) {
        return activeRequests.containsKey(playerId);
    }

    @Override
    public PlacementResult cancelPlacement(UUID playerId) {
        PlacementRequest removed = activeRequests.remove(playerId);
        previewService.clearPreview(playerId);
        if (removed == null) {
            return PlacementResult.failure("No active placement.");
        }
        recentPlacementActivity.put(playerId, Instant.now());
        return PlacementResult.success(removed.summary() + " cancelled.", null);
    }

    @Override
    public PlacementResult refreshPreview(Player player) {
        PlacementRequest request = activeRequests.get(player.getUuid());
        if (request == null) {
            return PlacementResult.failure("No active placement.");
        }
        Vector3i targetBlock = request.stagedTargetBlock() == null ? resolveTargetBlock(player) : request.stagedTargetBlock();
        if (targetBlock == null) {
            previewService.clearPreview(player.getUuid());
            return PlacementResult.failure("Look at a solid block within 12 blocks to preview placement.");
        }
        previewService.showPreview(player, request, targetBlock);
        return PlacementResult.success(request.summary() + " preview: " + targetBlock.getX() + ", " + targetBlock.getY() + ", " + targetBlock.getZ(), null);
    }

    @Override
    public PlacementResult confirmPlacement(Player player, Vector3i targetBlock) {
        PlacementRequest request = activeRequests.get(player.getUuid());
        if (request == null) {
            return PlacementResult.ignored();
        }
        if (targetBlock == null) {
            return PlacementResult.failure("Placement blocked: click a solid block.");
        }
        Instant now = Instant.now();
        PlayerGameState updatedState;
        String successMessage;
        if (request.modeType() == PlacementModeType.CASTLE) {
            updatedState = castlePlacementService.placeCastle(player.getUuid(), toLocation(player, targetBlock), now);
            successMessage = successMessage(request, updatedState);
        } else if (request.modeType() == PlacementModeType.BUILDING) {
            BuildingMutationResult mutationResult = buildingService.placeBuilding(
                    player.getUuid(),
                    request.buildingType(),
                    player.getWorld().getName(),
                    toPosition(targetBlock),
                    now
            );
            updatedState = mutationResult.state();
            if (!mutationResult.changed()) {
                return PlacementResult.failure(mutationResult.message());
            }
            if (updatedState != null) {
                buildingVisualService.refreshBuildings(player.getUuid(), updatedState);
            }
            successMessage = mutationResult.message();
        } else {
            updatedState = resourceNodeService.placeNode(
                    player.getUuid(),
                    request.resourceType(),
                    player.getWorld().getName(),
                    toPosition(targetBlock),
                    now
            );
            if (updatedState != null) {
                resourceNodeVisualService.refreshNodes(player.getUuid(), updatedState);
            }
            successMessage = successMessage(request, updatedState);
        }
        if (updatedState == null) {
            return PlacementResult.failure("Placement failed.");
        }
        previewService.clearPreview(player.getUuid());
        activeRequests.remove(player.getUuid());
        recentPlacementActivity.put(player.getUuid(), now);
        return PlacementResult.success(successMessage, updatedState);
    }

    @Override
    public PlacementResult confirmPlacementFromAim(Player player) {
        PlacementRequest request = activeRequests.get(player.getUuid());
        if (request == null) {
            return PlacementResult.failure("No active placement.");
        }
        Vector3i targetBlock = request.stagedTargetBlock() == null ? resolveTargetBlock(player) : request.stagedTargetBlock();
        if (targetBlock == null) {
            return PlacementResult.failure("Look at a solid block within 12 blocks before confirming placement.");
        }
        return confirmPlacement(player, targetBlock);
    }

    @Override
    public boolean shouldSuppressPrompts(UUID playerId, Instant now) {
        if (hasActivePlacement(playerId)) {
            return true;
        }
        Instant lastActivity = recentPlacementActivity.get(playerId);
        if (lastActivity == null) {
            return false;
        }
        if (lastActivity.plus(PROMPT_SUPPRESSION_WINDOW).isAfter(now)) {
            return true;
        }
        recentPlacementActivity.remove(playerId, lastActivity);
        return false;
    }

    private Vector3i resolveTargetBlock(Player player) {
        Ref<EntityStore> playerRef = player.getPlayerRef() == null ? null : player.getPlayerRef().getReference();
        if (playerRef == null || !playerRef.isValid()) {
            return null;
        }
        Store<EntityStore> store = playerRef.getStore();
        return TargetUtil.getTargetBlock(playerRef, TARGET_DISTANCE, store);
    }

    private CastleLocationData toLocation(Player player, Vector3i targetBlock) {
        Vector3d position = toPosition(targetBlock);
        return new CastleLocationData(player.getWorld().getName(), position.getX(), position.getY(), position.getZ());
    }

    private Vector3d toPosition(Vector3i targetBlock) {
        return new Vector3d(targetBlock.getX() + 0.5D, targetBlock.getY() + 1.0D, targetBlock.getZ() + 0.5D);
    }

    private String successMessage(PlacementRequest request, PlayerGameState updatedState) {
        if (request.modeType() == PlacementModeType.CASTLE) {
            CastleLocationData location = updatedState.castleLocation();
            return "Castle placed at " + (int) location.x() + ", " + (int) location.y() + ", " + (int) location.z() + ".";
        }
        if (request.modeType() == PlacementModeType.BUILDING) {
            return request.buildingType() == null
                    ? "Building construction started."
                    : request.buildingType().displayName() + " construction started.";
        }
        List<ResourceNodeData> nodes = resourceNodeService.listNodes(updatedState);
        ResourceNodeData latestNode = nodes.isEmpty() ? null : nodes.getLast();
        if (latestNode == null) {
            return request.summary() + " confirmed.";
        }
        return request.resourceType() + " node placed #" + nodes.size() + " " + latestNode.nodeId().toString().substring(0, 8) + ".";
    }
}
