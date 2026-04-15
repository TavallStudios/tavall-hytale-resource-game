package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.math.vector.Vector3d;
import com.tavall.hytale.resourcegame.cache.JacksonCacheCodec;
import com.tavall.hytale.resourcegame.cache.SemanticCacheFactory;
import com.tavall.hytale.resourcegame.config.CacheConfig;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.PlayerProfile;
import com.tavall.hytale.resourcegame.domain.ResourceNodeData;
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
        ResourceNodeService resourceNodeService = new ResourceNodeService(sessionStore, gameStateService, mapperProvider.mapper());

        UUID playerId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-15T08:00:00Z");
        PlayerGameState initialState = gameStateService.loadOrCreate(15L, playerId, new CastleLocationData("default", 0.0, 72.0, 0.0), now);
        PlayerGameState troopState = initialState.withPopulation(initialState.populationSummary().withTroopCount(6), now);
        sessionStore.put(new PlayerSession(playerId, new PlayerProfile(15L, playerId, "NodeBot", "UTC", "hash", now, now, now), troopState));

        PlayerGameState afterPlacement = resourceNodeService.placeNode(playerId, ResourceType.FOOD, "default", new Vector3d(8.0, 72.0, 8.0), now);
        ResourceNodeData node = resourceNodeService.listNodes(afterPlacement).getFirst();
        PlayerGameState afterAssign = resourceNodeService.assignTroops(playerId, node.nodeId(), 9, now.plusSeconds(1));

        assertEquals(1, resourceNodeService.listNodes(afterAssign).size());
        assertEquals(6, resourceNodeService.findNode(afterAssign, node.nodeId()).orElseThrow().assignedTroops());
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
        ResourceNodeService resourceNodeService = new ResourceNodeService(sessionStore, gameStateService, mapperProvider.mapper());

        UUID playerId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-15T08:05:00Z");
        PlayerGameState initialState = gameStateService.loadOrCreate(19L, playerId, new CastleLocationData("default", 0.0, 72.0, 0.0), now);
        PlayerGameState troopState = initialState.withPopulation(initialState.populationSummary().withTroopCount(4), now);
        sessionStore.put(new PlayerSession(playerId, new PlayerProfile(19L, playerId, "NodeTickBot", "UTC", "hash", now, now, now), troopState));

        PlayerGameState withNode = resourceNodeService.placeNode(playerId, ResourceType.IRON, "default", new Vector3d(10.0, 72.0, 10.0), now);
        ResourceNodeData node = resourceNodeService.listNodes(withNode).getFirst();
        PlayerGameState assignedState = resourceNodeService.assignTroops(playerId, node.nodeId(), 3, now.plusSeconds(1));
        PlayerGameState ticked = resourceNodeService.applyTick(assignedState, now.plusSeconds(12));

        assertEquals(assignedState.resources().iron() + 6, ticked.resources().iron());
        assertTrue(ticked.metadataJson().contains(node.nodeId().toString()));
    }
}
