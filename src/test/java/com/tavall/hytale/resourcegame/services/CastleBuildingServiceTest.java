package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.math.vector.Vector3d;
import com.tavall.hytale.resourcegame.cache.JacksonCacheCodec;
import com.tavall.hytale.resourcegame.cache.SemanticCacheFactory;
import com.tavall.hytale.resourcegame.config.CacheConfig;
import com.tavall.hytale.resourcegame.domain.BuildingMutationResult;
import com.tavall.hytale.resourcegame.domain.BuildingType;
import com.tavall.hytale.resourcegame.domain.CastleBuildingData;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.PlayerProfile;
import com.tavall.hytale.resourcegame.domain.ResourceInventory;
import com.tavall.hytale.resourcegame.population.PromotionCost;
import com.tavall.hytale.resourcegame.support.InMemoryPlayerGameStateStore;
import com.tavall.hytale.resourcegame.support.StubInteriorInstanceService;
import com.tavall.hytale.resourcegame.support.TestAwait;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CastleBuildingServiceTest {
    @Test
    void placeBuildingPersistsConstructionAndCompletesAfterTicks() {
        JsonMapperProvider mapperProvider = new JsonMapperProvider();
        InMemoryPlayerGameStateStore gameStateStore = new InMemoryPlayerGameStateStore();
        PlayerGameStateService gameStateService = new PlayerGameStateService(
                gameStateStore,
                new SemanticCacheFactory(new CacheConfig("", 6379, "", false)).build("building-state"),
                new JacksonCacheCodec<>(mapperProvider.mapper(), PlayerGameState.class, "building-state"),
                mapperProvider.mapper()
        );
        PlayerSessionStore sessionStore = new PlayerSessionStore();
        CastleBuildingService buildingService = new CastleBuildingService(
                sessionStore,
                gameStateService,
                new StubInteriorInstanceService(),
                new com.tavall.hytale.resourcegame.interior.InteriorLayoutService(),
                mapperProvider.mapper()
        );

        UUID playerId = UUID.randomUUID();
        Instant start = Instant.parse("2026-04-15T22:00:00Z");
        PlayerGameState initialState = gameStateService.loadOrCreate(
                201L,
                playerId,
                new CastleLocationData("overworld", 10.0, 72.0, 10.0),
                start
        );
        sessionStore.put(new PlayerSession(
                playerId,
                new PlayerProfile(201L, playerId, "BuilderBot", "UTC", "hash", start, start, start),
                initialState
        ));

        Vector3d buildPosition = new Vector3d(18.0, 73.0, 18.0);
        BuildingMutationResult placement = buildingService.placeBuilding(playerId, BuildingType.FARMSTEAD, "overworld", buildPosition, start);

        assertTrue(placement.changed());
        assertEquals("Farmstead construction started.", placement.message());
        assertNotNull(placement.state());
        CastleBuildingData placed = buildingService.listBuildings(placement.state()).getFirst();
        assertTrue(placed.isUnderConstruction());
        assertEquals(0, placed.currentLevel());
        assertEquals(1, placed.targetLevel());
        assertEquals(12, placement.state().resources().food());
        assertEquals(13, placement.state().resources().wood());
        assertEquals(10, placement.state().resources().iron());

        PlayerGameState midTickState = buildingService.applyTick(playerId, placement.state(), start.plusSeconds(12));
        CastleBuildingData midTickBuilding = buildingService.listBuildings(midTickState).getFirst();
        assertTrue(midTickBuilding.isUnderConstruction());

        PlayerGameState completedState = buildingService.applyTick(playerId, midTickState, start.plusSeconds(24));
        CastleBuildingData completedBuilding = buildingService.listBuildings(completedState).getFirst();
        assertFalse(completedBuilding.isUnderConstruction());
        assertEquals(1, completedBuilding.currentLevel());
        assertEquals(14, completedState.resources().food());
        assertEquals(13, completedState.resources().wood());
        assertEquals(10, completedState.resources().iron());
        assertTrue(completedState.metadataJson().contains(completedBuilding.buildingId().toString()));

        TestAwait.until(
                () -> gameStateStore.snapshot(201L)
                        .map(snapshot -> snapshot.metadataJson() != null && snapshot.metadataJson().contains(completedBuilding.buildingId().toString()))
                        .orElse(false),
                Duration.ofSeconds(2),
                "building metadata should persist after placement"
        );
    }

    @Test
    void workshopAndBarracksAffectFutureBuildSpeedAndPromotionCost() {
        JsonMapperProvider mapperProvider = new JsonMapperProvider();
        com.tavall.hytale.resourcegame.interior.InteriorLayoutService layoutService = new com.tavall.hytale.resourcegame.interior.InteriorLayoutService();
        InMemoryPlayerGameStateStore gameStateStore = new InMemoryPlayerGameStateStore();
        PlayerGameStateService gameStateService = new PlayerGameStateService(
                gameStateStore,
                new SemanticCacheFactory(new CacheConfig("", 6379, "", false)).build("building-bonus-state"),
                new JacksonCacheCodec<>(mapperProvider.mapper(), PlayerGameState.class, "building-bonus-state"),
                mapperProvider.mapper()
        );
        PlayerSessionStore sessionStore = new PlayerSessionStore();
        StubInteriorInstanceService interiorInstanceService = new StubInteriorInstanceService();
        CastleBuildingService buildingService = new CastleBuildingService(
                sessionStore,
                gameStateService,
                interiorInstanceService,
                layoutService,
                mapperProvider.mapper()
        );

        UUID playerId = UUID.randomUUID();
        Instant start = Instant.parse("2026-04-15T22:15:00Z");
        PlayerGameState seededState = gameStateService.loadOrCreate(
                205L,
                playerId,
                new CastleLocationData("overworld", 12.0, 72.0, 12.0),
                start
        ).withResources(new ResourceInventory(200, 200, 200), start);
        sessionStore.put(new PlayerSession(
                playerId,
                new PlayerProfile(205L, playerId, "BonusBot", "UTC", "hash", start, start, start),
                seededState
        ));

        String interiorWorld = seededState.castleLocation().worldName();
        Vector3d interiorOrigin = layoutService.originForCastle(seededState.castleLocation());
        BuildingMutationResult workshopPlacement = buildingService.placeBuilding(
                playerId,
                BuildingType.WORKSHOP,
                interiorWorld,
                new Vector3d(interiorOrigin.getX() + 5.0D, interiorOrigin.getY() + 1.0D, interiorOrigin.getZ()),
                start
        );
        assertTrue(workshopPlacement.changed());
        CastleBuildingData workshop = buildingService.listBuildings(workshopPlacement.state()).getFirst();
        PlayerGameState afterWorkshop = buildingService.forceComplete(playerId, workshop.buildingId(), start.plusSeconds(30)).state();

        BuildingMutationResult barracksPlacement = buildingService.placeBuilding(
                playerId,
                BuildingType.BARRACKS,
                interiorWorld,
                new Vector3d(interiorOrigin.getX() - 5.0D, interiorOrigin.getY() + 1.0D, interiorOrigin.getZ()),
                start.plusSeconds(31)
        );
        assertTrue(barracksPlacement.changed());
        CastleBuildingData barracks = buildingService.resolveBuilding(barracksPlacement.state(), BuildingType.BARRACKS.shortKey()).orElseThrow();
        PlayerGameState afterBarracks = buildingService.forceComplete(playerId, barracks.buildingId(), start.plusSeconds(62)).state();

        assertEquals(0.85D, buildingService.constructionSpeedMultiplier(afterBarracks), 0.0001D);
        PromotionCost adjustedCost = buildingService.adjustedPromotionCost(afterBarracks, PromotionCost.defaultCost());
        assertEquals(3, adjustedCost.foodCost());
        assertEquals(2, adjustedCost.woodCost());
        assertEquals(1, adjustedCost.ironCost());

        BuildingMutationResult farmsteadPlacement = buildingService.placeBuilding(
                playerId,
                BuildingType.FARMSTEAD,
                "overworld",
                new Vector3d(20.0, 73.0, 20.0),
                start.plusSeconds(63)
        );
        assertTrue(farmsteadPlacement.changed());
        CastleBuildingData farmstead = buildingService.resolveBuilding(farmsteadPlacement.state(), BuildingType.FARMSTEAD.shortKey()).orElseThrow();
        long buildSeconds = farmstead.constructionEndsAt().getEpochSecond() - farmstead.constructionStartedAt().getEpochSecond();
        assertEquals(21L, buildSeconds);
    }

    @Test
    void rejectsSurfacePlacementsThatBlockCastleCore() {
        JsonMapperProvider mapperProvider = new JsonMapperProvider();
        InMemoryPlayerGameStateStore gameStateStore = new InMemoryPlayerGameStateStore();
        PlayerGameStateService gameStateService = new PlayerGameStateService(
                gameStateStore,
                new SemanticCacheFactory(new CacheConfig("", 6379, "", false)).build("building-validate-state"),
                new JacksonCacheCodec<>(mapperProvider.mapper(), PlayerGameState.class, "building-validate-state"),
                mapperProvider.mapper()
        );
        PlayerSessionStore sessionStore = new PlayerSessionStore();
        CastleBuildingService buildingService = new CastleBuildingService(
                sessionStore,
                gameStateService,
                new StubInteriorInstanceService(),
                new com.tavall.hytale.resourcegame.interior.InteriorLayoutService(),
                mapperProvider.mapper()
        );

        UUID playerId = UUID.randomUUID();
        Instant start = Instant.parse("2026-04-15T22:25:00Z");
        PlayerGameState initialState = gameStateService.loadOrCreate(
                209L,
                playerId,
                new CastleLocationData("overworld", 10.0, 72.0, 10.0),
                start
        );
        sessionStore.put(new PlayerSession(
                playerId,
                new PlayerProfile(209L, playerId, "ValidateBot", "UTC", "hash", start, start, start),
                initialState
        ));

        BuildingMutationResult blocked = buildingService.placeBuilding(
                playerId,
                BuildingType.FARMSTEAD,
                "overworld",
                new Vector3d(11.0, 73.0, 11.0),
                start
        );

        assertFalse(blocked.changed());
        assertEquals("Place Farmstead between 5 and 18 blocks from the castle center.", blocked.message());
        assertTrue(buildingService.listBuildings(sessionStore.get(playerId).gameState()).isEmpty());
    }
}
