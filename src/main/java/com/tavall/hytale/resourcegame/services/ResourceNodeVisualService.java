package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
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
import com.tavall.hytale.resourcegame.world.VectorMath;
import com.tavall.hytale.resourcegame.world.ResourceNodeStructureService;
import com.tavall.hytale.resourcegame.world.ResourceNodeVisualRefs;
import it.unimi.dsi.fastutil.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

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
    private final Map<UUID, Map<UUID, ResourceNodeVisualRefs>> nodeRefs = new ConcurrentHashMap<>();

    public ResourceNodeVisualService(
            PopulationDisplayConfig displayConfig,
            IResourceNodeService resourceNodeService,
            ResourceNodeStructureService structureService
    ) {
        this.displayConfig = displayConfig;
        this.resourceNodeService = resourceNodeService;
        this.structureService = structureService;
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
                int roleIndex = NPCPlugin.get().getIndex(displayConfig.npcRoleName());
                if (roleIndex < 0) {
                    LOGGER.warning(() -> "Unable to build resource node visuals because NPC role '" + displayConfig.npcRoleName() + "' was not found.");
                    return;
                }
                structureService.ensureNodeSite(world, new Vector3d(node.x(), node.y(), node.z()));
                ResourceNodeSummary summary = resourceNodeService.summary(state, node);
                Ref<EntityStore> anchorRef = spawnNamed(store, roleIndex, new Vector3d(node.x(), node.y(), node.z()), anchorLabel(node, summary));
                List<Ref<EntityStore>> workerRefs = spawnWorkers(store, roleIndex, node, summary.assignedTroops());
                Ref<EntityStore> routeAnchorRef = spawnRouteAnchor(store, roleIndex, state, node, summary);
                List<Ref<EntityStore>> routeRefs = spawnRouteMarkers(store, roleIndex, state, node, summary.visibleRouteCount());
                rebuiltRefs.put(node.nodeId(), new ResourceNodeVisualRefs(anchorRef, routeAnchorRef, workerRefs, routeRefs));
            });
        }
        nodeRefs.put(playerId, rebuiltRefs);
    }

    private Ref<EntityStore> spawnNamed(Store<EntityStore> store, int roleIndex, Vector3d position, String label) {
        Pair<Ref<EntityStore>, ?> pair = NPCPlugin.get().spawnEntity(store, roleIndex, position, Vector3f.ZERO, null, null);
        Ref<EntityStore> ref = pair.first();
        if (ref != null && ref.isValid()) {
            store.putComponent(ref, DisplayNameComponent.getComponentType(), new DisplayNameComponent(Message.raw(label)));
        }
        return ref;
    }

    private List<Ref<EntityStore>> spawnWorkers(Store<EntityStore> store, int roleIndex, ResourceNodeData node, int assignedTroops) {
        List<Vector3d> positions = workerPositions(node);
        int visibleCount = Math.min(MAX_WORKER_MARKERS, Math.max(0, (int) Math.ceil(assignedTroops / 2.0)));
        List<Ref<EntityStore>> refs = new ArrayList<>();
        for (int index = 0; index < visibleCount && index < positions.size(); index++) {
            Pair<Ref<EntityStore>, ?> pair = NPCPlugin.get().spawnEntity(store, roleIndex, positions.get(index), Vector3f.ZERO, null, null);
            Ref<EntityStore> ref = pair.first();
            if (ref != null && ref.isValid()) {
                refs.add(ref);
            }
        }
        return List.copyOf(refs);
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
        Vector3d midpoint = interpolate(castlePosition, nodePosition, 0.5);
        return spawnNamed(
                store,
                roleIndex,
                midpoint,
                "Supply Lane | Convoys " + summary.visibleRouteCount() + " | Stock " + summary.stockPercent() + "%"
        );
    }

    private List<Ref<EntityStore>> spawnRouteMarkers(
            Store<EntityStore> store,
            int roleIndex,
            PlayerGameState state,
            ResourceNodeData node,
            int routeCount
    ) {
        if (state.castleLocation() == null || routeCount <= 0) {
            return List.of();
        }
        Vector3d castlePosition = new Vector3d(state.castleLocation().x(), state.castleLocation().y() + 1.0, state.castleLocation().z());
        Vector3d nodePosition = new Vector3d(node.x(), node.y(), node.z());
        int visibleCount = Math.min(MAX_ROUTE_MARKERS, routeCount);
        List<Ref<EntityStore>> refs = new ArrayList<>();
        for (int index = 0; index < visibleCount; index++) {
            double progress = (index + 1.0) / (visibleCount + 1.0);
            Vector3d routePosition = interpolate(castlePosition, nodePosition, progress);
            Pair<Ref<EntityStore>, ?> pair = NPCPlugin.get().spawnEntity(store, roleIndex, routePosition, Vector3f.ZERO, null, null);
            Ref<EntityStore> ref = pair.first();
            if (ref != null && ref.isValid()) {
                refs.add(ref);
            }
        }
        return List.copyOf(refs);
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

    private String anchorLabel(ResourceNodeData node, ResourceNodeSummary summary) {
        return node.resourceType()
                + " Node | Sent " + summary.assignedTroops()
                + " | Stock " + summary.currentStock() + "/" + summary.maxStock()
                + " (" + summary.stockPercent() + "%)"
                + " | +" + summary.gainPerTick() + "/tick";
    }

    private Vector3d interpolate(Vector3d start, Vector3d end, double progress) {
        Vector3d direction = new Vector3d(end.getX() - start.getX(), end.getY() - start.getY(), end.getZ() - start.getZ());
        Vector3d normalizedDirection = VectorMath.normalize(direction);
        double distance = Math.sqrt(
                direction.getX() * direction.getX()
                        + direction.getY() * direction.getY()
                        + direction.getZ() * direction.getZ()
        );
        return new Vector3d(
                start.getX() + normalizedDirection.getX() * distance * progress,
                start.getY() + normalizedDirection.getY() * distance * progress,
                start.getZ() + normalizedDirection.getZ() * distance * progress
        );
    }
}
