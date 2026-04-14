package com.tavall.hytale.resourcegame;

import com.tavall.hytale.resourcegame.cache.JacksonCacheCodec;
import com.tavall.hytale.resourcegame.cache.SemanticCacheFactory;
import com.tavall.hytale.resourcegame.config.CacheConfig;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.PlayerProfile;
import com.tavall.hytale.resourcegame.population.PromotionCost;
import com.tavall.hytale.resourcegame.resources.ResourceType;
import com.tavall.hytale.resourcegame.services.JsonMapperProvider;
import com.tavall.hytale.resourcegame.services.PlayerGameStateService;
import com.tavall.hytale.resourcegame.services.PlayerProfileService;
import com.tavall.hytale.resourcegame.services.PlayerSession;
import com.tavall.hytale.resourcegame.services.PlayerSessionStore;
import com.tavall.hytale.resourcegame.services.PopulationService;
import com.tavall.hytale.resourcegame.services.ResourceService;
import com.tavall.hytale.resourcegame.support.RecordingCastleSiteVisualService;
import com.tavall.hytale.resourcegame.support.InMemoryPlayerGameStateStore;
import com.tavall.hytale.resourcegame.support.InMemoryPlayerProfileStore;
import com.tavall.hytale.resourcegame.support.RecordingPopulationDisplayGateway;
import com.tavall.hytale.resourcegame.support.TestAwait;
import org.junit.jupiter.api.Test;
import org.tavall.abstractcache.semantic.SemanticCache;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public final class VerticalSliceServiceTest {
    @Test
    void loadOrCreateUsesCacheAndBuildsStarterState() {
        JsonMapperProvider mapperProvider = new JsonMapperProvider();
        SemanticCacheFactory cacheFactory = new SemanticCacheFactory(new CacheConfig("", 6379, "", false));
        SemanticCache profileCache = cacheFactory.build("profile-test");
        SemanticCache stateCache = cacheFactory.build("state-test");
        InMemoryPlayerProfileStore profileStore = new InMemoryPlayerProfileStore();
        InMemoryPlayerGameStateStore gameStateStore = new InMemoryPlayerGameStateStore();

        PlayerProfileService profileService = new PlayerProfileService(
                profileStore,
                profileCache,
                new JacksonCacheCodec<>(mapperProvider.mapper(), PlayerProfile.class, "profile-test")
        );
        PlayerGameStateService gameStateService = new PlayerGameStateService(
                gameStateStore,
                stateCache,
                new JacksonCacheCodec<>(mapperProvider.mapper(), PlayerGameState.class, "state-test"),
                mapperProvider.mapper()
        );

        UUID playerId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-08T10:00:00Z");
        CastleLocationData spawn = new CastleLocationData("overworld", 12.0, 64.0, 18.0);

        PlayerProfile firstProfile = profileService.loadOrCreate(playerId, "SliceBot", "America/Los_Angeles", "hashed-ip", now);
        PlayerGameState firstState = gameStateService.loadOrCreate(firstProfile.id(), playerId, spawn, now);
        PlayerProfile secondProfile = profileService.loadOrCreate(playerId, "SliceBot", "America/Los_Angeles", "hashed-ip", now.plusSeconds(30));
        PlayerGameState secondState = gameStateService.loadOrCreate(firstProfile.id(), playerId, spawn, now.plusSeconds(30));

        assertEquals(firstProfile.id(), secondProfile.id());
        assertEquals(firstState.castleId(), secondState.castleId());
        assertEquals("overworld", firstState.castleLocation().worldName());
        assertEquals(12, firstState.populationSummary().citizenCount());
        assertEquals(0, firstState.populationSummary().troopCount());
        assertEquals(40, firstState.resources().food());
        assertEquals(25, firstState.resources().wood());
        assertEquals(10, firstState.resources().iron());
        assertTrue(profileStore.findCalls() <= 1, "profile store should not be hit after cache warm");
        assertTrue(gameStateStore.findCalls() <= 1, "game state store should not be hit after cache warm");
    }

    @Test
    void populationAndResourcesFlowPersistsAndUpdatesDisplayState() {
        JsonMapperProvider mapperProvider = new JsonMapperProvider();
        SemanticCacheFactory cacheFactory = new SemanticCacheFactory(new CacheConfig("", 6379, "", false));
        InMemoryPlayerGameStateStore gameStateStore = new InMemoryPlayerGameStateStore();
        PlayerGameStateService gameStateService = new PlayerGameStateService(
                gameStateStore,
                cacheFactory.build("flow-state"),
                new JacksonCacheCodec<>(mapperProvider.mapper(), PlayerGameState.class, "flow-state"),
                mapperProvider.mapper()
        );

        PlayerSessionStore sessionStore = new PlayerSessionStore();
        RecordingCastleSiteVisualService castleSiteVisualService = new RecordingCastleSiteVisualService();
        ResourceService resourceService = new ResourceService(sessionStore, gameStateService, castleSiteVisualService);
        RecordingPopulationDisplayGateway displayGateway = new RecordingPopulationDisplayGateway();
        PopulationService populationService = new PopulationService(
                sessionStore,
                gameStateService,
                resourceService,
                castleSiteVisualService,
                displayGateway,
                PromotionCost.defaultCost()
        );

        UUID playerId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-08T11:00:00Z");
        PlayerGameState initialState = gameStateService.loadOrCreate(
                44L,
                playerId,
                new CastleLocationData("overworld", 4.0, 70.0, 4.0),
                now
        );
        PlayerProfile profile = new PlayerProfile(44L, playerId, "FlowBot", "America/Los_Angeles", "ip", now, now, now);
        sessionStore.put(new PlayerSession(playerId, profile, initialState));

        resourceService.addResource(playerId, ResourceType.FOOD, 20);
        resourceService.setResource(playerId, ResourceType.WOOD, 80);
        resourceService.setResource(playerId, ResourceType.IRON, 30);

        assertTrue(populationService.promoteCitizen(playerId));
        assertTrue(populationService.promoteCitizen(playerId));
        assertTrue(populationService.demoteTroop(playerId));
        PlayerGameState finalState = populationService.addCitizens(playerId, 3);

        assertNotNull(finalState);
        assertEquals(14, finalState.populationSummary().citizenCount());
        assertEquals(1, finalState.populationSummary().troopCount());
        assertEquals(52, finalState.resources().food());
        assertEquals(76, finalState.resources().wood());
        assertEquals(28, finalState.resources().iron());
        assertEquals(14, displayGateway.lastSummary(playerId).citizenCount());
        assertEquals(1, displayGateway.lastSummary(playerId).troopCount());
        assertEquals(14, castleSiteVisualService.lastState(playerId).populationSummary().citizenCount());
        assertTrue(castleSiteVisualService.refreshCount(playerId) >= 4, "castle site visuals should refresh on mutations");

        TestAwait.until(
                () -> gameStateStore.snapshot(44L)
                        .map(snapshot -> snapshot.populationSummary().citizenCount() == 14
                                && snapshot.populationSummary().troopCount() == 1
                                && snapshot.resources().food() == 52)
                        .orElse(false),
                Duration.ofSeconds(2),
                "async state persistence did not complete"
        );
    }

    @Test
    void upgradeActionStatesExplainBlockedAndReadyCases() {
        JsonMapperProvider mapperProvider = new JsonMapperProvider();
        SemanticCacheFactory cacheFactory = new SemanticCacheFactory(new CacheConfig("", 6379, "", false));
        InMemoryPlayerGameStateStore gameStateStore = new InMemoryPlayerGameStateStore();
        PlayerGameStateService gameStateService = new PlayerGameStateService(
                gameStateStore,
                cacheFactory.build("ui-state"),
                new JacksonCacheCodec<>(mapperProvider.mapper(), PlayerGameState.class, "ui-state"),
                mapperProvider.mapper()
        );

        PlayerSessionStore sessionStore = new PlayerSessionStore();
        RecordingCastleSiteVisualService castleSiteVisualService = new RecordingCastleSiteVisualService();
        ResourceService resourceService = new ResourceService(sessionStore, gameStateService, castleSiteVisualService);
        PopulationService populationService = new PopulationService(
                sessionStore,
                gameStateService,
                resourceService,
                castleSiteVisualService,
                new RecordingPopulationDisplayGateway(),
                PromotionCost.defaultCost()
        );

        UUID playerId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-08T11:30:00Z");
        PlayerGameState initialState = gameStateService.loadOrCreate(
                77L,
                playerId,
                new CastleLocationData("overworld", 1.0, 65.0, 1.0),
                now
        );
        PlayerProfile profile = new PlayerProfile(77L, playerId, "UiBot", "UTC", "ip", now, now, now);
        sessionStore.put(new PlayerSession(playerId, profile, initialState));

        PlayerGameState noCitizens = populationService.setCitizens(playerId, 0);
        assertFalse(populationService.promoteActionState(noCitizens).allowed());
        assertEquals("Blocked: need at least 1 citizen.", populationService.promoteActionState(noCitizens).message());
        assertFalse(populationService.demoteActionState(noCitizens).allowed());
        assertEquals("Blocked: need at least 1 troop.", populationService.demoteActionState(noCitizens).message());

        resourceService.setResource(playerId, ResourceType.FOOD, 4);
        resourceService.setResource(playerId, ResourceType.WOOD, 0);
        resourceService.setResource(playerId, ResourceType.IRON, 1);
        PlayerGameState woodBlocked = populationService.setCitizens(playerId, 2);
        assertFalse(populationService.promoteActionState(woodBlocked).allowed());
        assertEquals("Blocked: need 2 Wood.", populationService.promoteActionState(woodBlocked).message());

        resourceService.setResource(playerId, ResourceType.WOOD, 2);
        PlayerGameState readyState = sessionStore.get(playerId).gameState();
        assertTrue(populationService.promoteActionState(readyState).allowed());
        assertEquals("Ready: promote 1 citizen into 1 troop.", populationService.promoteActionState(readyState).message());
        assertEquals("Cost per promotion: 4 Food, 2 Wood, 1 Iron.", populationService.promotionCostSummary());
    }
}
