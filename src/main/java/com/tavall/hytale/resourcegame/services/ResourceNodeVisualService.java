package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeVisualService;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.ResourceNodeData;
import com.tavall.hytale.resourcegame.domain.ResourceNodeSummary;
import com.tavall.hytale.resourcegame.world.ProtectedStructureType;
import com.tavall.hytale.resourcegame.world.ResourceNodeStructureService;
import com.tavall.hytale.resourcegame.world.ResourceNodeVisualRefs;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Builds block-first node visuals with overhead labels.
 */
public final class ResourceNodeVisualService implements IResourceNodeVisualService, IDependencyInjectableConcrete {
    private static final Logger LOGGER = Logger.getLogger(ResourceNodeVisualService.class.getName());

    private final IResourceNodeService resourceNodeService;
    private final ResourceNodeStructureService structureService;
    private final WorldLabelService worldLabelService;
    private final StructureProtectionService protectionService;
    private final Map<UUID, Map<UUID, ResourceNodeVisualRefs>> nodeRefs = new ConcurrentHashMap<>();

    public ResourceNodeVisualService(
            IResourceNodeService resourceNodeService,
            ResourceNodeStructureService structureService,
            WorldLabelService worldLabelService,
            StructureProtectionService protectionService
    ) {
        this.resourceNodeService = resourceNodeService;
        this.structureService = structureService;
        this.worldLabelService = worldLabelService;
        this.protectionService = protectionService;
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
        for (Map.Entry<UUID, ResourceNodeVisualRefs> entry : refsByNode.entrySet()) {
            ResourceNodeVisualRefs refs = entry.getValue();
            protectionService.clearStructure(structureKey(entry.getKey()));
            World world = Universe.get().getWorld(refs.worldName());
            if (world != null) {
                world.execute(() -> clearRefsOnWorld(world, refs));
                continue;
            }
            removeRefs(refs);
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
        Map<UUID, ResourceNodeVisualRefs> previousRefs = nodeRefs.remove(playerId);
        clearExistingNodes(previousRefs);
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
                ResourceNodeSummary summary = resourceNodeService.summary(state, node);
                protectionService.replaceStructure(
                        structureKey(node.nodeId()),
                        playerId,
                        ProtectedStructureType.RESOURCE_NODE,
                        world.getName(),
                        node.resourceType().name().toLowerCase(),
                        structureService.ensureNodeSite(world, node, summary)
                );
                List<Ref<EntityStore>> labelRefs = worldLabelService.spawnLabelStack(
                        world,
                        new Vector3d(node.x(), node.y() + 3.3D, node.z()),
                        anchorLabelLines(node, summary)
                );
                rebuiltRefs.put(
                        node.nodeId(),
                        new ResourceNodeVisualRefs(
                                world.getName(),
                                new Vector3d(node.x(), node.y(), node.z()),
                                labelRefs
                        )
                );
            });
        }
        nodeRefs.put(playerId, rebuiltRefs);
    }

    private void clearExistingNodes(Map<UUID, ResourceNodeVisualRefs> refsByNode) {
        if (refsByNode == null || refsByNode.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, ResourceNodeVisualRefs> entry : refsByNode.entrySet()) {
            ResourceNodeVisualRefs refs = entry.getValue();
            protectionService.clearStructure(structureKey(entry.getKey()));
            World world = Universe.get().getWorld(refs.worldName());
            if (world != null) {
                world.execute(() -> clearRefsOnWorld(world, refs));
                continue;
            }
            removeRefs(refs);
        }
    }

    private void clearRefsOnWorld(World world, ResourceNodeVisualRefs refs) {
        structureService.clearNodeSite(world, refs.worldPosition());
        removeRefs(refs);
    }

    private void removeRefs(ResourceNodeVisualRefs refs) {
        for (Ref<EntityStore> ref : refs.allRefs()) {
            if (ref != null && ref.isValid()) {
                ref.getStore().removeEntity(ref, RemoveReason.REMOVE);
            }
        }
    }

    private List<String> anchorLabelLines(ResourceNodeData node, ResourceNodeSummary summary) {
        long remainingLifetimeSeconds = node.remainingLifetimeSeconds(java.time.Instant.now());
        String ttlLine = remainingLifetimeSeconds < 0L
                ? "Pillage +" + summary.pillageReward()
                : "Pillage +" + summary.pillageReward() + " | ttl " + Math.max(1L, Math.round(remainingLifetimeSeconds / 60.0D)) + "m";
        return List.of(
                node.resourceType() + " Node | " + summary.currentStock() + "/" + summary.maxStock() + " | " + summary.stockStatus(),
                "Troops " + summary.assignedTroops() + " | Crew " + summary.assignedWorkers() + " | +" + summary.gainPerTick() + "/tick",
                ttlLine
        );
    }

    private String structureKey(UUID nodeId) {
        return "node:" + nodeId;
    }
}
