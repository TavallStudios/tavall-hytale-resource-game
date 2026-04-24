package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.math.vector.Vector3d;
import com.tavall.hytale.resourcegame.cache.JacksonCacheCodec;
import com.tavall.hytale.resourcegame.cache.SemanticCacheFactory;
import com.tavall.hytale.resourcegame.config.CacheConfig;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.GameStateMetadata;
import com.tavall.hytale.resourcegame.domain.OnboardingProgress;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.PlayerProfile;
import com.tavall.hytale.resourcegame.domain.ResourceNodeData;
import com.tavall.hytale.resourcegame.domain.ResourceNodePillageResult;
import com.tavall.hytale.resourcegame.resources.ResourceType;
import com.tavall.hytale.resourcegame.support.InMemoryPlayerGameStateStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ResourceNodeServiceTest {
    @Test
    void placedNodesPersistAndClampAssignedTroopsToAvailablePool() {
        JsonMapperProvider mapperProvider = new JsonMapperProvider();
        InMemoryPlayerGameStateStore gameStateStore = new InMemoryPlayerGameStateStore();
        PlayerGameStateService gameStateService = new PlayerGameStateService(
                gameStateStore,
                new SemanticCacheFactory(new CacheConfig("", 6379, "", false)).build("node-state"),
                new JacksonCacheCodec<>(mapperProvider.mapper(), PlayerGameState.class, "node-state"),
                mapperProvider.mapper()
        );
        PlayerSessionStore sessionStore = new PlayerSessionStore();
        ResourceNodeService resourceNodeService = new ResourceNodeService(sessionStore, gameStateService, mapperProvider.mapper(), new CastleEconomyPlanner());

        UUID playerId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-15T08:00:00Z");
        PlayerGameState initialState = gameStateService.loadOrCreate(15L, playerId, new CastleLocationData("default", 0.0, 72.0, 0.0), now);
        PlayerGameState troopState = initialState.withPopulation(initialState.populationSummary().withTroopCount(6), now);
        sessionStore.put(new PlayerSession(playerId, new PlayerProfile(15L, playerId, "NodeBot", "UTC", "hash", now, now, now), troopState));

        PlayerGameState afterPlacement = resourceNodeService.placeNode(playerId, ResourceType.FOOD, "default", new Vector3d(8.0, 72.0, 8.0), now);
        ResourceNodeData node = resourceNodeService.listNodes(afterPlacement).getFirst();
        PlayerGameState afterAssign = resourceNodeService.assignTroops(playerId, node.nodeId(), 9, now.plusSeconds(1));

        assertEquals(1, resourceNodeService.listNodes(afterAssign).size());
        ResourceNodeData assignedNode = resourceNodeService.findNode(afterAssign, node.nodeId()).orElseThrow();
        assertEquals(6, assignedNode.assignedTroops());
        assertEquals(180, assignedNode.maxStock());
        assertEquals(180, assignedNode.currentStock());
        assertEquals(0, resourceNodeService.availableTroops(afterAssign));
    }

    @Test
    void tickAddsResourceGainFromAssignedTroops() {
        JsonMapperProvider mapperProvider = new JsonMapperProvider();
        InMemoryPlayerGameStateStore gameStateStore = new InMemoryPlayerGameStateStore();
        PlayerGameStateService gameStateService = new PlayerGameStateService(
                gameStateStore,
                new SemanticCacheFactory(new CacheConfig("", 6379, "", false)).build("node-tick"),
                new JacksonCacheCodec<>(mapperProvider.mapper(), PlayerGameState.class, "node-tick"),
                mapperProvider.mapper()
        );
        PlayerSessionStore sessionStore = new PlayerSessionStore();
        ResourceNodeService resourceNodeService = new ResourceNodeService(sessionStore, gameStateService, mapperProvider.mapper(), new CastleEconomyPlanner());

        UUID playerId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-15T08:05:00Z");
        PlayerGameState initialState = gameStateService.loadOrCreate(19L, playerId, new CastleLocationData("default", 0.0, 72.0, 0.0), now);
        PlayerGameState troopState = initialState.withPopulation(initialState.populationSummary().withTroopCount(4), now);
        sessionStore.put(new PlayerSession(playerId, new PlayerProfile(19L, playerId, "NodeTickBot", "UTC", "hash", now, now, now), troopState));

        PlayerGameState withNode = resourceNodeService.placeNode(playerId, ResourceType.IRON, "default", new Vector3d(10.0, 72.0, 10.0), now);
        ResourceNodeData node = resourceNodeService.listNodes(withNode).getFirst();
        PlayerGameState assignedState = resourceNodeService.assignTroops(playerId, node.nodeId(), 3, now.plusSeconds(1));
        PlayerGameState ticked = resourceNodeService.applyTick(assignedState, now.plusSeconds(12));

        ResourceNodeData tickedNode = resourceNodeService.findNode(ticked, node.nodeId()).orElseThrow();
        assertEquals(assignedState.resources().iron() + 9, ticked.resources().iron());
        assertEquals(111, tickedNode.currentStock());
        assertTrue(ticked.metadataJson().contains(node.nodeId().toString()));
    }

    @Test
    void nodeMutationsPreserveInteriorInstanceIndex() {
        JsonMapperProvider mapperProvider = new JsonMapperProvider();
        InMemoryPlayerGameStateStore gameStateStore = new InMemoryPlayerGameStateStore();
        PlayerGameStateService gameStateService = new PlayerGameStateService(
                gameStateStore,
                new SemanticCacheFactory(new CacheConfig("", 6379, "", false)).build("node-interior-index"),
                new JacksonCacheCodec<>(mapperProvider.mapper(), PlayerGameState.class, "node-interior-index"),
                mapperProvider.mapper()
        );
        PlayerSessionStore sessionStore = new PlayerSessionStore();
        ResourceNodeService resourceNodeService = new ResourceNodeService(sessionStore, gameStateService, mapperProvider.mapper(), new CastleEconomyPlanner());

        UUID playerId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-16T06:10:00Z");
        PlayerGameState initialState = gameStateService.loadOrCreate(31L, playerId, new CastleLocationData("default", 0.0, 72.0, 0.0), now);
        PlayerGameState indexed = gameStateService.bumpInteriorInstanceIndex(initialState, now.plusSeconds(1));
        sessionStore.put(new PlayerSession(playerId, new PlayerProfile(31L, playerId, "NodeIndexBot", "UTC", "hash", now, now, now), indexed));

        assertEquals(1, gameStateService.interiorInstanceIndex(indexed));
        PlayerGameState afterPlacement = resourceNodeService.placeNode(playerId, ResourceType.FOOD, "default", new Vector3d(9.0, 72.0, 9.0), now.plusSeconds(2));
        assertEquals(1, gameStateService.interiorInstanceIndex(afterPlacement));
    }

    @Test
    void pillageAddsImmediateRewardAndDrainsNodeStock() {
        JsonMapperProvider mapperProvider = new JsonMapperProvider();
        InMemoryPlayerGameStateStore gameStateStore = new InMemoryPlayerGameStateStore();
        PlayerGameStateService gameStateService = new PlayerGameStateService(
                gameStateStore,
                new SemanticCacheFactory(new CacheConfig("", 6379, "", false)).build("node-pillage"),
                new JacksonCacheCodec<>(mapperProvider.mapper(), PlayerGameState.class, "node-pillage"),
                mapperProvider.mapper()
        );
        PlayerSessionStore sessionStore = new PlayerSessionStore();
        ResourceNodeService resourceNodeService = new ResourceNodeService(sessionStore, gameStateService, mapperProvider.mapper(), new CastleEconomyPlanner());

        UUID playerId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-15T08:07:00Z");
        PlayerGameState initialState = gameStateService.loadOrCreate(21L, playerId, new CastleLocationData("default", 0.0, 72.0, 0.0), now);
        PlayerGameState troopState = initialState.withPopulation(initialState.populationSummary().withTroopCount(2), now);
        sessionStore.put(new PlayerSession(playerId, new PlayerProfile(21L, playerId, "PillageBot", "UTC", "hash", now, now, now), troopState));

        PlayerGameState withNode = resourceNodeService.placeNode(playerId, ResourceType.WOOD, "default", new Vector3d(10.0, 72.0, 10.0), now);
        ResourceNodeData node = resourceNodeService.listNodes(withNode).getFirst();
        PlayerGameState assignedState = resourceNodeService.assignTroops(playerId, node.nodeId(), 2, now.plusSeconds(1));
        int woodBefore = assignedState.resources().wood();

        ResourceNodePillageResult result = resourceNodeService.pillageNode(playerId, node.nodeId(), now.plusSeconds(2));

        assertTrue(result.changed());
        assertEquals(28, result.reward());
        assertEquals(woodBefore + 28, result.state().resources().wood());
        assertEquals(122, resourceNodeService.findNode(result.state(), node.nodeId()).orElseThrow().currentStock());
    }

    @Test
    void depletedNodesAreRemovedWhenHarvestingConsumesRemainingStock() {
        JsonMapperProvider mapperProvider = new JsonMapperProvider();
        InMemoryPlayerGameStateStore gameStateStore = new InMemoryPlayerGameStateStore();
        PlayerGameStateService gameStateService = new PlayerGameStateService(
                gameStateStore,
                new SemanticCacheFactory(new CacheConfig("", 6379, "", false)).build("node-regen"),
                new JacksonCacheCodec<>(mapperProvider.mapper(), PlayerGameState.class, "node-regen"),
                mapperProvider.mapper()
        );
        PlayerSessionStore sessionStore = new PlayerSessionStore();
        ResourceNodeService resourceNodeService = new ResourceNodeService(sessionStore, gameStateService, mapperProvider.mapper(), new CastleEconomyPlanner());

        UUID playerId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-15T08:10:00Z");
        PlayerGameState initialState = gameStateService.loadOrCreate(22L, playerId, new CastleLocationData("default", 0.0, 72.0, 0.0), now);
        PlayerGameState troopState = initialState.withPopulation(initialState.populationSummary().withTroopCount(9), now);
        sessionStore.put(new PlayerSession(playerId, new PlayerProfile(22L, playerId, "NodeRegenBot", "UTC", "hash", now, now, now), troopState));

        PlayerGameState withNode = resourceNodeService.placeNode(playerId, ResourceType.FOOD, "default", new Vector3d(11.0, 72.0, 11.0), now);
        ResourceNodeData node = resourceNodeService.listNodes(withNode).getFirst();
        PlayerGameState reducedStock = resourceNodeService.setStock(playerId, node.nodeId(), 10, now.plusSeconds(1));
        PlayerGameState assignedState = resourceNodeService.assignTroops(playerId, node.nodeId(), 9, now.plusSeconds(2));
        PlayerGameState afterTick = resourceNodeService.applyTick(assignedState, now.plusSeconds(12));

        assertTrue(resourceNodeService.findNode(afterTick, node.nodeId()).isEmpty());
    }

    @Test
    void expiredNodesAreHiddenAndDroppedOnTick() throws Exception {
        JsonMapperProvider mapperProvider = new JsonMapperProvider();
        InMemoryPlayerGameStateStore gameStateStore = new InMemoryPlayerGameStateStore();
        PlayerGameStateService gameStateService = new PlayerGameStateService(
                gameStateStore,
                new SemanticCacheFactory(new CacheConfig("", 6379, "", false)).build("node-expiry"),
                new JacksonCacheCodec<>(mapperProvider.mapper(), PlayerGameState.class, "node-expiry"),
                mapperProvider.mapper()
        );
        PlayerSessionStore sessionStore = new PlayerSessionStore();
        ResourceNodeService resourceNodeService = new ResourceNodeService(sessionStore, gameStateService, mapperProvider.mapper(), new CastleEconomyPlanner());

        UUID playerId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-15T08:15:00Z");
        PlayerGameState initialState = gameStateService.loadOrCreate(25L, playerId, new CastleLocationData("default", 0.0, 72.0, 0.0), now);
        sessionStore.put(new PlayerSession(playerId, new PlayerProfile(25L, playerId, "ExpiryBot", "UTC", "hash", now, now, now), initialState));

        PlayerGameState withNode = resourceNodeService.placeNode(playerId, ResourceType.FOOD, "default", new Vector3d(12.0, 72.0, 12.0), now);
        ResourceNodeData node = resourceNodeService.listNodes(withNode).getFirst();
        ResourceNodeData expiredNode = node.withLifetime(now.minusSeconds(30));
        String expiredMetadata = mapperProvider.mapper().writeValueAsString(
                GameStateMetadata.fromPopulation(
                        withNode.populationSummary(),
                        OnboardingProgress.defaults(),
                        java.util.List.of(expiredNode),
                        java.util.List.of()
                )
        );
        PlayerGameState expiredState = withNode.withMetadataJson(expiredMetadata, now);

        assertTrue(resourceNodeService.listNodes(expiredState).isEmpty());
        PlayerGameState afterTick = resourceNodeService.applyTick(expiredState, now.plusSeconds(1));
        assertTrue(resourceNodeService.listNodes(afterTick).isEmpty());
    }
}
