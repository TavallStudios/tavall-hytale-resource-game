package com.tavall.hytale.resourcegame.services;

import com.tavall.hytale.resourcegame.cache.JacksonCacheCodec;
import com.tavall.hytale.resourcegame.cache.SemanticCacheFactory;
import com.tavall.hytale.resourcegame.config.CacheConfig;
import com.tavall.hytale.resourcegame.domain.BuildingType;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.CitizenJobType;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.PlayerProfile;
import com.tavall.hytale.resourcegame.domain.ResourceInventory;
import com.tavall.hytale.resourcegame.resources.ResourceType;
import com.tavall.hytale.resourcegame.support.InMemoryPlayerGameStateStore;
import com.tavall.hytale.resourcegame.support.NoopCastleBuildingService;
import com.tavall.hytale.resourcegame.support.RecordingCastleBuildingVisualService;
import com.tavall.hytale.resourcegame.support.RecordingCastleSiteVisualService;
import com.tavall.hytale.resourcegame.support.RecordingResourceNodeVisualService;
import com.tavall.hytale.resourcegame.support.RecordingUiNavigator;
import com.tavall.hytale.resourcegame.support.StubInteriorInstanceService;
import com.tavall.hytale.resourcegame.support.TestAwait;
import com.hypixel.hytale.math.vector.Vector3d;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CastleEconomySimulationServiceTest {
    @Test
    void runTickAddsResourcesAndPersistsJobCounts() {
        JsonMapperProvider mapperProvider = new JsonMapperProvider();
        InMemoryPlayerGameStateStore gameStateStore = new InMemoryPlayerGameStateStore();
        PlayerGameStateService gameStateService = new PlayerGameStateService(
                gameStateStore,
                new SemanticCacheFactory(new CacheConfig("", 6379, "", false)).build("economy-sim"),
                new JacksonCacheCodec<>(mapperProvider.mapper(), PlayerGameState.class, "economy-sim"),
                mapperProvider.mapper()
        );
        PlayerSessionStore sessionStore = new PlayerSessionStore();
        RecordingCastleSiteVisualService visualService = new RecordingCastleSiteVisualService();
        RecordingCastleBuildingVisualService buildingVisualService = new RecordingCastleBuildingVisualService();
        RecordingResourceNodeVisualService resourceNodeVisualService = new RecordingResourceNodeVisualService();
        RecordingUiNavigator uiNavigator = new RecordingUiNavigator();
        CastleEconomyPlanner planner = new CastleEconomyPlanner();
        ResourceNodeService resourceNodeService = new ResourceNodeService(sessionStore, gameStateService, mapperProvider.mapper());
        CastleEconomySimulationService simulationService = new CastleEconomySimulationService(
                sessionStore,
                gameStateService,
                new NoopCastleBuildingService(),
                buildingVisualService,
                visualService,
                planner,
                resourceNodeService,
                resourceNodeVisualService,
                uiNavigator
        );

        UUID playerId = UUID.randomUUID();
        Instant start = Instant.parse("2026-04-14T18:10:00Z");
        PlayerGameState initialState = gameStateService.loadOrCreate(
                91L,
                playerId,
                new CastleLocationData("overworld", 6.0, 72.0, 6.0),
                start
        );
        sessionStore.put(new PlayerSession(
                playerId,
                new PlayerProfile(91L, playerId, "EconomyBot", "UTC", "hash", start, start, start),
                initialState
        ));
        PlayerGameState withNode = resourceNodeService.placeNode(
                playerId,
                ResourceType.FOOD,
                "overworld",
                new Vector3d(initialState.castleLocation().x() + 8.0, initialState.castleLocation().y(), initialState.castleLocation().z() + 8.0),
                start
        );
        sessionStore.get(playerId).updateGameState(withNode);

        simulationService.runTick(start.plusSeconds(12));

        PlayerGameState updated = sessionStore.get(playerId).gameState();
        assertTrue(updated.resources().food() >= initialState.resources().food());
        assertTrue(updated.resources().wood() >= initialState.resources().wood());
        assertTrue(updated.resources().iron() >= initialState.resources().iron());
        assertTrue(updated.populationSummary().citizenMetaData().jobCounts().containsKey(CitizenJobType.GATHERER));
        assertEquals(updated.populationSummary().citizenCount(), planner.snapshot(updated).jobCounts().values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(updated.populationSummary().citizenCount(), visualService.lastState(playerId).populationSummary().citizenCount());
        assertEquals(updated.resources().food(), buildingVisualService.lastState(playerId).resources().food());
        assertEquals(updated.resources().food(), resourceNodeVisualService.lastState(playerId).resources().food());
        assertEquals(updated.resources().food(), uiNavigator.lastState(playerId).resources().food());

        TestAwait.until(
                () -> gameStateStore.snapshot(91L)
                        .map(snapshot -> snapshot.populationSummary().citizenMetaData().jobCounts().containsKey(CitizenJobType.GATHERER))
                        .orElse(false),
                Duration.ofSeconds(2),
                "economy tick should persist updated job counts"
        );
    }

    @Test
    void runTickCompletesBuildingsAndAppliesTheirBonusYield() {
        JsonMapperProvider mapperProvider = new JsonMapperProvider();
        InMemoryPlayerGameStateStore gameStateStore = new InMemoryPlayerGameStateStore();
        PlayerGameStateService gameStateService = new PlayerGameStateService(
                gameStateStore,
                new SemanticCacheFactory(new CacheConfig("", 6379, "", false)).build("economy-building-sim"),
                new JacksonCacheCodec<>(mapperProvider.mapper(), PlayerGameState.class, "economy-building-sim"),
                mapperProvider.mapper()
        );
        PlayerSessionStore sessionStore = new PlayerSessionStore();
        RecordingCastleSiteVisualService siteVisualService = new RecordingCastleSiteVisualService();
        RecordingCastleBuildingVisualService buildingVisualService = new RecordingCastleBuildingVisualService();
        RecordingResourceNodeVisualService resourceNodeVisualService = new RecordingResourceNodeVisualService();
        RecordingUiNavigator uiNavigator = new RecordingUiNavigator();
        CastleEconomyPlanner planner = new CastleEconomyPlanner();
        CastleBuildingService buildingService = new CastleBuildingService(
                sessionStore,
                gameStateService,
                new StubInteriorInstanceService(),
                new com.tavall.hytale.resourcegame.interior.InteriorLayoutService(),
                mapperProvider.mapper()
        );
        ResourceNodeService resourceNodeService = new ResourceNodeService(sessionStore, gameStateService, mapperProvider.mapper());
        CastleEconomySimulationService simulationService = new CastleEconomySimulationService(
                sessionStore,
                gameStateService,
                buildingService,
                buildingVisualService,
                siteVisualService,
                planner,
                resourceNodeService,
                resourceNodeVisualService,
                uiNavigator
        );

        UUID playerId = UUID.randomUUID();
        Instant start = Instant.parse("2026-04-15T00:00:00Z");
        PlayerGameState seededState = gameStateService.loadOrCreate(
                111L,
                playerId,
                new CastleLocationData("overworld", 6.0, 72.0, 6.0),
                start
        ).withResources(new ResourceInventory(200, 200, 200), start);
        sessionStore.put(new PlayerSession(
                playerId,
                new PlayerProfile(111L, playerId, "EconomyBuilderBot", "UTC", "hash", start, start, start),
                seededState
        ));

        PlayerGameState placedState = buildingService.placeBuilding(
                playerId,
                BuildingType.FARMSTEAD,
                "overworld",
                new Vector3d(14.0, 73.0, 14.0),
                start
        ).state();
        int expectedFoodAfterTick = placedState.resources().food() + planner.snapshot(placedState).gainFor(ResourceType.FOOD) + 2;

        simulationService.runTick(start.plusSeconds(CastleEconomySimulationService.TICK_INTERVAL_SECONDS * 2));

        PlayerGameState updated = sessionStore.get(playerId).gameState();
        assertEquals(expectedFoodAfterTick, updated.resources().food());
        assertEquals(1, buildingService.listBuildings(updated).getFirst().currentLevel());
        assertTrue(buildingVisualService.refreshCount(playerId) >= 1);
        assertEquals(updated.resources().food(), uiNavigator.lastState(playerId).resources().food());
        assertEquals(updated.resources().food(), siteVisualService.lastState(playerId).resources().food());
    }
}
