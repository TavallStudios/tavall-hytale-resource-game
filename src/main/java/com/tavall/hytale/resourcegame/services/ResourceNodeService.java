package com.tavall.hytale.resourcegame.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hypixel.hytale.math.vector.Vector3d;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerGameStateService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerSessionStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeService;
import com.tavall.hytale.resourcegame.domain.GameStateMetadata;
import com.tavall.hytale.resourcegame.domain.OnboardingProgress;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.ResourceInventory;
import com.tavall.hytale.resourcegame.domain.ResourceNodeData;
import com.tavall.hytale.resourcegame.domain.ResourceNodeSummary;
import com.tavall.hytale.resourcegame.resources.ResourceType;
import com.tavall.hytale.resourcegame.tasks.AsyncTask;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Persists placed node state and troop assignments in player metadata.
 */
public final class ResourceNodeService implements IResourceNodeService, IDependencyInjectableConcrete {
    private static final Logger LOGGER = Logger.getLogger(ResourceNodeService.class.getName());
    private static final int DEFAULT_ROUTE_DIVISOR = 3;

    private final IPlayerSessionStore sessionStore;
    private final IPlayerGameStateService gameStateService;
    private final ObjectMapper objectMapper;

    public ResourceNodeService(
            IPlayerSessionStore sessionStore,
            IPlayerGameStateService gameStateService,
            ObjectMapper objectMapper
    ) {
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.gameStateService = Objects.requireNonNull(gameStateService, "gameStateService");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public List<ResourceNodeData> listNodes(PlayerGameState state) {
        return metadataOf(state, resolveNow(state)).resourceNodes().stream()
                .map(this::normalizeNode)
                .sorted(Comparator.comparing(ResourceNodeData::placedAt).thenComparing(ResourceNodeData::nodeId))
                .toList();
    }

    @Override
    public Optional<ResourceNodeData> resolveNode(PlayerGameState state, String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        List<ResourceNodeData> nodes = listNodes(state);
        try {
            int index = Integer.parseInt(token);
            if (index >= 1 && index <= nodes.size()) {
                return Optional.of(nodes.get(index - 1));
            }
        } catch (NumberFormatException ignored) {
        }
        String normalizedToken = token.toLowerCase(Locale.ROOT).replace("-", "");
        return nodes.stream()
                .filter(node -> node.nodeId().toString().replace("-", "").startsWith(normalizedToken))
                .findFirst();
    }

    @Override
    public Optional<ResourceNodeData> findNode(PlayerGameState state, UUID nodeId) {
        if (nodeId == null) {
            return Optional.empty();
        }
        return listNodes(state).stream().filter(node -> node.nodeId().equals(nodeId)).findFirst();
    }

    @Override
    public ResourceNodeSummary summary(PlayerGameState state, ResourceNodeData node) {
        ResourceNodeData normalizedNode = normalizeNode(node);
        int gainPerTick = Math.min(
                normalizedNode.currentStock(),
                normalizedNode.assignedTroops() * yieldPerTroop(normalizedNode.resourceType())
        );
        int stockPercent = normalizedNode.maxStock() == 0
                ? 0
                : (int) Math.round((normalizedNode.currentStock() * 100.0) / normalizedNode.maxStock());
        int routeCount = normalizedNode.assignedTroops() == 0
                ? 0
                : Math.max(1, (int) Math.ceil(normalizedNode.assignedTroops() / (double) DEFAULT_ROUTE_DIVISOR));
        return new ResourceNodeSummary(
                normalizedNode,
                availableTroops(state),
                normalizedNode.assignedTroops(),
                gainPerTick,
                normalizedNode.currentStock(),
                normalizedNode.maxStock(),
                normalizedNode.regenerationPerTick(),
                stockPercent,
                routeCount,
                stockStatus(stockPercent)
        );
    }

    @Override
    public int assignedTroops(PlayerGameState state) {
        return listNodes(state).stream().mapToInt(ResourceNodeData::assignedTroops).sum();
    }

    @Override
    public int availableTroops(PlayerGameState state) {
        return Math.max(0, state.populationSummary().troopCount() - assignedTroops(state));
    }

    @Override
    public PlayerGameState placeNode(UUID playerId, ResourceType resourceType, String worldName, Vector3d position, Instant now) {
        PlayerSession session = sessionStore.get(playerId);
        if (session == null || resourceType == null || worldName == null || position == null) {
            return null;
        }
        List<ResourceNodeData> nodes = new ArrayList<>(listNodes(session.gameState()));
        NodeStockProfile stockProfile = stockProfile(resourceType);
        nodes.add(new ResourceNodeData(
                UUID.randomUUID(),
                resourceType,
                worldName,
                position.getX(),
                position.getY(),
                position.getZ(),
                0,
                stockProfile.maxStock(),
                stockProfile.maxStock(),
                stockProfile.regenerationPerTick(),
                now
        ));
        return persistNodeState(session, nodes, now);
    }

    @Override
    public PlayerGameState removeNode(UUID playerId, UUID nodeId, Instant now) {
        PlayerSession session = sessionStore.get(playerId);
        if (session == null || nodeId == null) {
            return null;
        }
        List<ResourceNodeData> nodes = listNodes(session.gameState()).stream()
                .filter(node -> !node.nodeId().equals(nodeId))
                .toList();
        return persistNodeState(session, nodes, now);
    }

    @Override
    public PlayerGameState clearNodes(UUID playerId, Instant now) {
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return null;
        }
        return persistNodeState(session, List.of(), now);
    }

    @Override
    public PlayerGameState assignTroops(UUID playerId, UUID nodeId, int assignedTroops, Instant now) {
        return mutateNodeAssignment(playerId, nodeId, assignedTroops, false, now);
    }

    @Override
    public PlayerGameState addTroops(UUID playerId, UUID nodeId, int troopDelta, Instant now) {
        return mutateNodeAssignment(playerId, nodeId, troopDelta, true, now);
    }

    @Override
    public PlayerGameState setStock(UUID playerId, UUID nodeId, int currentStock, Instant now) {
        PlayerSession session = sessionStore.get(playerId);
        if (session == null || nodeId == null) {
            return null;
        }
        List<ResourceNodeData> nodes = new ArrayList<>(listNodes(session.gameState()));
        for (int index = 0; index < nodes.size(); index++) {
            ResourceNodeData node = nodes.get(index);
            if (!node.nodeId().equals(nodeId)) {
                continue;
            }
            nodes.set(index, node.withCurrentStock(currentStock));
            return persistNodeState(session, nodes, now);
        }
        return session.gameState();
    }

    @Override
    public PlayerGameState normalizeAssignments(PlayerGameState state, Instant now) {
        List<ResourceNodeData> nodes = listNodes(state);
        if (nodes.isEmpty()) {
            return state;
        }
        int remainingTroops = Math.max(0, state.populationSummary().troopCount());
        boolean changed = false;
        List<ResourceNodeData> normalizedNodes = new ArrayList<>();
        for (ResourceNodeData node : nodes) {
            int normalizedAssigned = Math.min(node.assignedTroops(), remainingTroops);
            if (normalizedAssigned != node.assignedTroops()) {
                changed = true;
            }
            normalizedNodes.add(node.withAssignedTroops(normalizedAssigned));
            remainingTroops -= normalizedAssigned;
        }
        if (!changed) {
            return state;
        }
        return rewriteNodes(state, normalizedNodes, now);
    }

    @Override
    public PlayerGameState applyTick(PlayerGameState state, Instant now) {
        PlayerGameState normalizedState = normalizeAssignments(state, now);
        ResourceInventory resources = normalizedState.resources();
        int food = resources.food();
        int wood = resources.wood();
        int iron = resources.iron();
        List<ResourceNodeData> updatedNodes = new ArrayList<>();
        for (ResourceNodeData node : listNodes(normalizedState)) {
            int potentialGain = node.assignedTroops() * yieldPerTroop(node.resourceType());
            int actualGain = Math.min(node.currentStock(), potentialGain);
            switch (node.resourceType()) {
                case FOOD -> food += actualGain;
                case WOOD -> wood += actualGain;
                case IRON -> iron += actualGain;
            }
            int replenishedStock = Math.min(
                    node.maxStock(),
                    Math.max(0, node.currentStock() - actualGain) + node.regenerationPerTick()
            );
            updatedNodes.add(node.withCurrentStock(replenishedStock));
        }
        return rewriteNodes(
                normalizedState.withResources(new ResourceInventory(food, wood, iron), now),
                updatedNodes,
                now
        );
    }

    public String summaryLine(PlayerGameState state, ResourceNodeData node, int index) {
        ResourceNodeSummary summary = summary(state, node);
        return "#" + index + " " + node.resourceType() + " " + shortId(node.nodeId())
                + " @ " + node.worldName()
                + " " + (int) node.x() + "," + (int) node.y() + "," + (int) node.z()
                + " | assigned " + summary.assignedTroops()
                + " | reserve " + summary.availableTroops()
                + " | stock " + summary.currentStock() + "/" + summary.maxStock()
                + " (" + summary.stockPercent() + "%)"
                + " " + summary.stockStatus()
                + " | regen " + summary.regenerationPerTick()
                + " | +" + summary.gainPerTick() + "/tick";
    }

    private PlayerGameState mutateNodeAssignment(UUID playerId, UUID nodeId, int troopValue, boolean deltaMode, Instant now) {
        PlayerSession session = sessionStore.get(playerId);
        if (session == null || nodeId == null) {
            return null;
        }
        List<ResourceNodeData> nodes = new ArrayList<>(listNodes(session.gameState()));
        for (int index = 0; index < nodes.size(); index++) {
            ResourceNodeData node = nodes.get(index);
            if (!node.nodeId().equals(nodeId)) {
                continue;
            }
            int desiredAssignment = deltaMode ? node.assignedTroops() + troopValue : troopValue;
            int withoutCurrentNode = assignedTroops(session.gameState()) - node.assignedTroops();
            int maxAssignable = Math.max(0, session.gameState().populationSummary().troopCount() - withoutCurrentNode);
            int cappedAssignment = Math.max(0, Math.min(desiredAssignment, maxAssignable));
            nodes.set(index, node.withAssignedTroops(cappedAssignment));
            return persistNodeState(session, nodes, now);
        }
        return session.gameState();
    }

    private PlayerGameState persistNodeState(PlayerSession session, List<ResourceNodeData> nodes, Instant now) {
        PlayerGameState updatedState = rewriteNodes(session.gameState(), nodes, now);
        session.updateGameState(updatedState);
        gameStateService.cacheState(session.playerId(), updatedState);
        AsyncTask.runAsync(() -> gameStateService.persistState(updatedState, now));
        LOGGER.info(() -> "Resource nodes updated for " + session.playerId() + ": " + nodes.size() + " nodes");
        return updatedState;
    }

    private PlayerGameState rewriteNodes(PlayerGameState state, List<ResourceNodeData> nodes, Instant now) {
        GameStateMetadata metadata = metadataOf(state, now);
        GameStateMetadata updatedMetadata = new GameStateMetadata(
                metadata.citizenMetaData(),
                metadata.troopMetaData(),
                metadata.agingState(),
                metadata.jobCounts(),
                metadata.onboardingProgress(),
                nodes
        );
        try {
            return state.withMetadataJson(objectMapper.writeValueAsString(updatedMetadata), now);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to encode resource node metadata", ex);
        }
    }

    private GameStateMetadata metadataOf(PlayerGameState state, Instant now) {
        if (state.metadataJson() == null || state.metadataJson().isBlank()) {
            return GameStateMetadata.fromPopulation(state.populationSummary(), OnboardingProgress.defaults(), List.of());
        }
        try {
            GameStateMetadata metadata = objectMapper.readValue(state.metadataJson(), GameStateMetadata.class);
            return new GameStateMetadata(
                    metadata.citizenMetaData(),
                    metadata.troopMetaData(),
                    metadata.agingState(),
                    metadata.jobCounts(),
                    metadata.onboardingProgress(),
                    metadata.resourceNodes()
            );
        } catch (Exception ex) {
            LOGGER.warning(() -> "Failed to decode resource node metadata. Falling back to empty nodes. " + ex.getMessage());
            return GameStateMetadata.fromPopulation(state.populationSummary(), OnboardingProgress.defaults(), List.of());
        }
    }

    private Instant resolveNow(PlayerGameState state) {
        return state.updatedAt() == null ? Instant.now() : state.updatedAt();
    }

    private int yieldPerTroop(ResourceType resourceType) {
        return switch (resourceType) {
            case FOOD -> 4;
            case WOOD -> 3;
            case IRON -> 2;
        };
    }

    private ResourceNodeData normalizeNode(ResourceNodeData node) {
        NodeStockProfile stockProfile = stockProfile(node.resourceType());
        int maxStock = node.maxStock() <= 0 ? stockProfile.maxStock() : node.maxStock();
        int regenerationPerTick = node.regenerationPerTick() <= 0 ? stockProfile.regenerationPerTick() : node.regenerationPerTick();
        int currentStock = node.currentStock() <= 0 && node.maxStock() <= 0
                ? maxStock
                : Math.min(maxStock, Math.max(0, node.currentStock()));
        return node.withStockProfile(currentStock, maxStock, regenerationPerTick);
    }

    private NodeStockProfile stockProfile(ResourceType resourceType) {
        return switch (resourceType) {
            case FOOD -> new NodeStockProfile(180, 10);
            case WOOD -> new NodeStockProfile(150, 7);
            case IRON -> new NodeStockProfile(120, 5);
        };
    }

    private String stockStatus(int stockPercent) {
        if (stockPercent <= 0) {
            return "Exhausted";
        }
        if (stockPercent < 25) {
            return "Low";
        }
        if (stockPercent < 70) {
            return "Stable";
        }
        return "Rich";
    }

    private String shortId(UUID nodeId) {
        String value = nodeId.toString();
        return value.substring(0, Math.min(8, value.length()));
    }
}
