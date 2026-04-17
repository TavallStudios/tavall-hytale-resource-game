package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.tavall.hytale.resourcegame.config.PopulationDisplayConfig;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeVisualService;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.ResourceNodeData;
import com.tavall.hytale.resourcegame.domain.ResourceNodeSummary;
import com.tavall.hytale.resourcegame.world.ResourceNodeStructureService;
import com.tavall.hytale.resourcegame.world.ResourceNodeVisualRefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.time.Instant;

/**
 * Builds readable world-space visuals for placed gather nodes.
 */
public final class ResourceNodeVisualService implements IResourceNodeVisualService, IDependencyInjectableConcrete {
    private static final Logger LOGGER = Logger.getLogger(ResourceNodeVisualService.class.getName());
    private static final int MAX_WORKER_MARKERS = 6;
    private static final int MAX_ROUTE_MARKERS = 4;

    private final PopulationDisplayConfig displayConfig;
    private final IResourceNodeService resourceNodeService;
    private final ResourceNodeStructureService structureService;
    private final ResourceNodeRoutePlanner routePlanner;
    private final NpcVisualSpawner npcVisualSpawner;
    private final Map<UUID, Map<UUID, ResourceNodeVisualRefs>> nodeRefs = new ConcurrentHashMap<>();

    public ResourceNodeVisualService(
            PopulationDisplayConfig displayConfig,
            IResourceNodeService resourceNodeService,
            ResourceNodeStructureService structureService,
            ResourceNodeRoutePlanner routePlanner,
            NpcVisualSpawner npcVisualSpawner
    ) {
        this.displayConfig = displayConfig;
        this.resourceNodeService = resourceNodeService;
        this.structureService = structureService;
        this.routePlanner = routePlanner;
        this.npcVisualSpawner = npcVisualSpawner;
    }

    @Override
    public void ensureNodes(UUID playerId, PlayerGameState state) {
        rebuildNodes(playerId, state);
    }

    @Override
    public void refreshNodes(UUID playerId, PlayerGameState state) {
        rebuildNodes(playerId, state);
    }

    @Override
    public void clearNodes(UUID playerId) {
        Map<UUID, ResourceNodeVisualRefs> refsByNode = nodeRefs.remove(playerId);
        if (refsByNode == null) {
            return;
        }
        for (ResourceNodeVisualRefs refs : refsByNode.values()) {
            for (Ref<EntityStore> ref : refs.allRefs()) {
                if (ref != null && ref.isValid()) {
                    ref.getStore().removeEntity(ref, RemoveReason.REMOVE);
                }
            }
        }
    }

    @Override
    public Optional<UUID> findNodeId(UUID playerId, Ref<EntityStore> targetRef) {
        if (playerId == null || targetRef == null || !targetRef.isValid()) {
            return Optional.empty();
        }
        Map<UUID, ResourceNodeVisualRefs> refsByNode = nodeRefs.get(playerId);
        if (refsByNode == null) {
            return Optional.empty();
        }
        return refsByNode.entrySet().stream()
                .filter(entry -> entry.getValue().matches(targetRef))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private void rebuildNodes(UUID playerId, PlayerGameState state) {
        clearNodes(playerId);
        if (playerId == null || state == null) {
            return;
        }
        List<ResourceNodeData> nodes = resourceNodeService.listNodes(state);
        if (nodes.isEmpty()) {
            return;
        }
        Map<UUID, ResourceNodeVisualRefs> rebuiltRefs = new ConcurrentHashMap<>();
        for (ResourceNodeData node : nodes) {
            World world = Universe.get().getWorld(node.worldName());
            if (world == null) {
                continue;
            }
            world.execute(() -> {
                Store<EntityStore> store = world.getEntityStore().getStore();
                int roleIndex = new NpcRoleResolver().resolveRoleIndex(displayConfig.npcRoleName());
                if (roleIndex < 0) {
                    LOGGER.warning(() -> "Unable to build resource node visuals because NPC role '" + displayConfig.npcRoleName() + "' was not found.");
                    return;
                }
                Instant refreshTime = Instant.now();
                ResourceNodeSummary summary = resourceNodeService.summary(state, node);
                structureService.ensureNodeSite(world, node, summary);
                Ref<EntityStore> anchorRef = spawnNamed(store, roleIndex, new Vector3d(node.x(), node.y(), node.z()), anchorLabel(node, summary), anchorScale(summary));
                List<Ref<EntityStore>> workerRefs = spawnWorkers(store, roleIndex, node, summary.assignedTroops() + summary.assignedWorkers(), workerScale(summary));
                Ref<EntityStore> routeAnchorRef = spawnRouteAnchor(store, roleIndex, state, node, summary);
                List<Ref<EntityStore>> routeRefs = spawnRouteMarkers(store, roleIndex, state, summary, refreshTime);
                rebuiltRefs.put(node.nodeId(), new ResourceNodeVisualRefs(anchorRef, routeAnchorRef, workerRefs, routeRefs));
            });
        }
        nodeRefs.put(playerId, rebuiltRefs);
    }

    private Ref<EntityStore> spawnNamed(Store<EntityStore> store, int roleIndex, Vector3d position, String label, float scale) {
        return npcVisualSpawner.spawnNamed(store, roleIndex, position, label, scale);
    }

    private List<Ref<EntityStore>> spawnWorkers(Store<EntityStore> store, int roleIndex, ResourceNodeData node, int assignedPopulation, float scale) {
        List<Vector3d> positions = workerPositions(node);
        int visibleCount = Math.min(MAX_WORKER_MARKERS, Math.max(0, (int) Math.ceil(assignedPopulation / 2.0)));
        return npcVisualSpawner.spawnGroup(store, roleIndex, positions, visibleCount, scale);
    }

    private Ref<EntityStore> spawnRouteAnchor(
            Store<EntityStore> store,
            int roleIndex,
            PlayerGameState state,
            ResourceNodeData node,
            ResourceNodeSummary summary
    ) {
        if (state.castleLocation() == null || summary.visibleRouteCount() <= 0) {
            return null;
        }
        Vector3d castlePosition = new Vector3d(state.castleLocation().x(), state.castleLocation().y() + 1.0, state.castleLocation().z());
        Vector3d nodePosition = new Vector3d(node.x(), node.y(), node.z());
        Vector3d midpoint = midpoint(castlePosition, nodePosition);
        return spawnNamed(
                store,
                roleIndex,
                midpoint,
                "Supply Lane | Convoys " + summary.visibleRouteCount() + " | " + summary.stockStatus(),
                routeScale(summary)
        );
    }

    private List<Ref<EntityStore>> spawnRouteMarkers(
            Store<EntityStore> store,
            int roleIndex,
            PlayerGameState state,
            ResourceNodeSummary summary,
            Instant now
    ) {
        if (state.castleLocation() == null || summary.visibleRouteCount() <= 0) {
            return List.of();
        }
        List<Vector3d> positions = routePlanner.routePositions(state.castleLocation(), summary, now, MAX_ROUTE_MARKERS);
        return npcVisualSpawner.spawnGroup(store, roleIndex, positions, positions.size(), routeScale(summary));
    }

    private List<Vector3d> workerPositions(ResourceNodeData node) {
        double x = node.x();
        double y = node.y();
        double z = node.z();
        return List.of(
                new Vector3d(x + 1.2, y, z),
                new Vector3d(x - 1.2, y, z),
                new Vector3d(x, y, z + 1.2),
                new Vector3d(x, y, z - 1.2),
                new Vector3d(x + 1.0, y, z + 1.0),
                new Vector3d(x - 1.0, y, z - 1.0)
        );
    }

    private Vector3d midpoint(Vector3d start, Vector3d end) {
        return new Vector3d(
                (start.getX() + end.getX()) / 2.0,
                (start.getY() + end.getY()) / 2.0,
                (start.getZ() + end.getZ()) / 2.0
        );
    }

    private String anchorLabel(ResourceNodeData node, ResourceNodeSummary summary) {
        return node.resourceType()
                + " Node | Troops " + summary.assignedTroops()
                + " | Workers " + summary.assignedWorkers()
                + " | Stock " + summary.currentStock() + "/" + summary.maxStock()
                + " (" + summary.stockPercent() + "%)"
                + " " + summary.stockStatus()
                + " | Pillage +" + summary.pillageReward()
                + " | +" + summary.gainPerTick() + "/tick";
    }

    private float anchorScale(ResourceNodeSummary summary) {
        return clampScale(0.95F + (summary.stockPercent() / 100.0F) + ((summary.assignedTroops() + summary.assignedWorkers()) / 10.0F), 0.95F, 2.15F);
    }

    private float workerScale(ResourceNodeSummary summary) {
        return clampScale(0.6F + ((summary.assignedTroops() + summary.assignedWorkers()) / 12.0F), 0.6F, 1.35F);
    }

    private float routeScale(ResourceNodeSummary summary) {
        return clampScale(0.5F + (summary.visibleRouteCount() / 4.0F), 0.5F, 1.15F);
    }

    private float clampScale(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
