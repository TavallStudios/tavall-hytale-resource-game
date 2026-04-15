package com.tavall.hytale.resourcegame.services;

import com.tavall.hytale.resourcegame.cache.JacksonCacheCodec;
import com.tavall.hytale.resourcegame.cache.SemanticCacheFactory;
import com.tavall.hytale.resourcegame.config.CacheConfig;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.CitizenJobType;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.PlayerProfile;
import com.tavall.hytale.resourcegame.support.InMemoryPlayerGameStateStore;
import com.tavall.hytale.resourcegame.support.RecordingCastleSiteVisualService;
import com.tavall.hytale.resourcegame.support.TestAwait;
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
        CastleEconomyPlanner planner = new CastleEconomyPlanner();
        CastleEconomySimulationService simulationService = new CastleEconomySimulationService(
                sessionStore,
                gameStateService,
                visualService,
                planner
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

        simulationService.runTick(start.plusSeconds(12));

        PlayerGameState updated = sessionStore.get(playerId).gameState();
        assertTrue(updated.resources().food() >= initialState.resources().food());
        assertTrue(updated.resources().wood() >= initialState.resources().wood());
        assertTrue(updated.resources().iron() >= initialState.resources().iron());
        assertTrue(updated.populationSummary().citizenMetaData().jobCounts().containsKey(CitizenJobType.GATHERER));
        assertEquals(updated.populationSummary().citizenCount(), planner.snapshot(updated).jobCounts().values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(updated.populationSummary().citizenCount(), visualService.lastState(playerId).populationSummary().citizenCount());

        TestAwait.until(
                () -> gameStateStore.snapshot(91L)
                        .map(snapshot -> snapshot.populationSummary().citizenMetaData().jobCounts().containsKey(CitizenJobType.GATHERER))
                        .orElse(false),
                Duration.ofSeconds(2),
                "economy tick should persist updated job counts"
        );
    }
}
