package com.tavall.hytale.resourcegame.services;

import com.tavall.hytale.resourcegame.cache.JacksonCacheCodec;
import com.tavall.hytale.resourcegame.cache.SemanticCacheFactory;
import com.tavall.hytale.resourcegame.config.CacheConfig;
import com.tavall.hytale.resourcegame.domain.AgingState;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.CitizenJobType;
import com.tavall.hytale.resourcegame.domain.CitizenMetaData;
import com.tavall.hytale.resourcegame.domain.GameStateMetadata;
import com.tavall.hytale.resourcegame.domain.OnboardingProgress;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.PopulationSummary;
import com.tavall.hytale.resourcegame.domain.ResourceInventory;
import com.tavall.hytale.resourcegame.domain.TroopMetaData;
import com.tavall.hytale.resourcegame.support.InMemoryPlayerGameStateStore;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PlayerGameStateMetadataTest {
    @Test
    void loadOrCreateRehydratesMetadataAndOnboardingState() throws Exception {
        JsonMapperProvider mapperProvider = new JsonMapperProvider();
        InMemoryPlayerGameStateStore store = new InMemoryPlayerGameStateStore();
        PlayerGameStateService service = new PlayerGameStateService(
                store,
                new SemanticCacheFactory(new CacheConfig("", 6379, "", false)).build("metadata-rehydrate"),
                new JacksonCacheCodec<>(mapperProvider.mapper(), PlayerGameState.class, "metadata-rehydrate"),
                mapperProvider.mapper()
        );

        Instant now = Instant.parse("2026-04-12T21:00:00Z");
        CastleLocationData castleLocation = new CastleLocationData("overworld", 8.0, 70.0, 8.0);
        PopulationSummary summary = new PopulationSummary(
                18,
                3,
                new CitizenMetaData(0.82, 0.41, 0.77, Map.of(CitizenJobType.GATHERER, 5, CitizenJobType.BUILDER, 2)),
                new TroopMetaData(0.74, 0.63, 0.58),
                new AgingState(now, Duration.ofHours(12))
        );
        GameStateMetadata metadata = GameStateMetadata.fromPopulation(summary, new OnboardingProgress(false, true, false));
        PlayerGameState seeded = new PlayerGameState(
                0L,
                99L,
                UUID.randomUUID(),
                castleLocation,
                summary,
                new ResourceInventory(90, 45, 21),
                null,
                mapperProvider.mapper().writeValueAsString(metadata),
                now,
                now
        );
        store.upsert(seeded, now);

        PlayerGameState loaded = service.loadOrCreate(99L, UUID.randomUUID(), castleLocation, now);

        assertEquals(18, loaded.populationSummary().citizenCount());
        assertEquals(3, loaded.populationSummary().troopCount());
        assertEquals(0.82, loaded.populationSummary().citizenMetaData().productivityMedian());
        assertEquals(5, loaded.populationSummary().citizenMetaData().jobCounts().get(CitizenJobType.GATHERER));
        assertEquals(0.74, loaded.populationSummary().troopMetaData().combatMedian());
        assertEquals(Duration.ofHours(12), loaded.populationSummary().agingState().totalAge());
        assertFalse(service.isInteriorTutorialPending(loaded));
        assertFalse(service.isInteriorTourPending(loaded));
        assertTrue(service.isUpgradeTutorialPending(loaded));
    }

    @Test
    void tutorialMilestonesRewriteMetadataWithoutLosingPopulationState() {
        JsonMapperProvider mapperProvider = new JsonMapperProvider();
        PlayerGameStateService service = new PlayerGameStateService(
                new InMemoryPlayerGameStateStore(),
                new SemanticCacheFactory(new CacheConfig("", 6379, "", false)).build("metadata-milestones"),
                new JacksonCacheCodec<>(mapperProvider.mapper(), PlayerGameState.class, "metadata-milestones"),
                mapperProvider.mapper()
        );

        Instant now = Instant.parse("2026-04-12T21:05:00Z");
        PlayerGameState state = service.loadOrCreate(
                42L,
                UUID.randomUUID(),
                new CastleLocationData("overworld", 4.0, 65.0, 4.0),
                now
        );

        PlayerGameState afterInterior = service.markInteriorTutorialSeen(state, now.plusSeconds(5));
        PlayerGameState afterTour = service.markInteriorTourSeen(afterInterior, now.plusSeconds(8));
        PlayerGameState afterUpgrade = service.markUpgradeTutorialSeen(afterTour, now.plusSeconds(10));

        assertFalse(service.isInteriorTutorialPending(afterInterior));
        assertTrue(service.isInteriorTourPending(afterInterior));
        assertFalse(service.isInteriorTourPending(afterTour));
        assertTrue(service.isUpgradeTutorialPending(afterTour));
        assertFalse(service.isUpgradeTutorialPending(afterUpgrade));
        assertEquals(state.populationSummary().citizenCount(), afterUpgrade.populationSummary().citizenCount());
        assertEquals(state.populationSummary().troopCount(), afterUpgrade.populationSummary().troopCount());
    }
}
