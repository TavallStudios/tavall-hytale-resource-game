package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.math.vector.Vector3d;
import com.tavall.hytale.resourcegame.cache.JacksonCacheCodec;
import com.tavall.hytale.resourcegame.cache.SemanticCacheFactory;
import com.tavall.hytale.resourcegame.config.CacheConfig;
import com.tavall.hytale.resourcegame.domain.BuildingType;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.PlayerProfile;
import com.tavall.hytale.resourcegame.domain.ResourceInventory;
import com.tavall.hytale.resourcegame.support.InMemoryPlayerGameStateStore;
import com.tavall.hytale.resourcegame.support.StubInteriorInstanceService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class BuildingPlacementPlannerTest {
    @Test
    void returnsDeterministicSurfaceAndInteriorAnchorsWhenBuildingsDoNotExist() {
        com.tavall.hytale.resourcegame.interior.InteriorLayoutService layoutService = new com.tavall.hytale.resourcegame.interior.InteriorLayoutService();
        PlayerSessionStore sessionStore = new PlayerSessionStore();
        PlayerGameStateService gameStateService = gameStateService("building-placement-planner-state-a");
        StubInteriorInstanceService interiorInstanceService = new StubInteriorInstanceService();
        CastleBuildingService buildingService = new CastleBuildingService(
                sessionStore,
                gameStateService,
                interiorInstanceService,
                layoutService,
                new JsonMapperProvider().mapper()
        );
        BuildingPlacementPlanner planner = new BuildingPlacementPlanner(
                buildingService,
                interiorInstanceService,
                gameStateService,
                layoutService
        );

        UUID playerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        PlayerGameState state = seedSession(sessionStore, gameStateService, playerId, Instant.parse("2026-04-15T23:00:00Z"));

        assertEquals(interiorInstanceService.worldNameFor(playerId), planner.recommendedWorldName(playerId, state, BuildingType.FARMSTEAD));
        Vector3d farmsteadAnchor = layoutService.createLayoutForCastle(state.castleLocation()).buildingAnchor(BuildingType.FARMSTEAD);
        assertVector(planner.recommendedPosition(playerId, state, BuildingType.FARMSTEAD), farmsteadAnchor.getX(), farmsteadAnchor.getY(), farmsteadAnchor.getZ());

        assertEquals(interiorInstanceService.worldNameFor(playerId), planner.recommendedWorldName(playerId, state, BuildingType.BARRACKS));
        Vector3d barracksAnchor = layoutService.createLayoutForCastle(state.castleLocation()).buildingAnchor(BuildingType.BARRACKS);
        assertVector(planner.recommendedPosition(playerId, state, BuildingType.BARRACKS), barracksAnchor.getX(), barracksAnchor.getY(), barracksAnchor.getZ());
    }

    @Test
    void returnsExistingBuildingLocationWhenBuildingAlreadyPlaced() {
        com.tavall.hytale.resourcegame.interior.InteriorLayoutService layoutService = new com.tavall.hytale.resourcegame.interior.InteriorLayoutService();
        PlayerSessionStore sessionStore = new PlayerSessionStore();
        PlayerGameStateService gameStateService = gameStateService("building-placement-planner-state-b");
        StubInteriorInstanceService interiorInstanceService = new StubInteriorInstanceService();
        CastleBuildingService buildingService = new CastleBuildingService(
                sessionStore,
                gameStateService,
                interiorInstanceService,
                layoutService,
                new JsonMapperProvider().mapper()
        );
        BuildingPlacementPlanner planner = new BuildingPlacementPlanner(
                buildingService,
                interiorInstanceService,
                gameStateService,
                layoutService
        );

        UUID playerId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Instant now = Instant.parse("2026-04-15T23:10:00Z");
        PlayerGameState state = seedSession(sessionStore, gameStateService, playerId, now)
                .withResources(new ResourceInventory(200, 200, 200), now);
        state = gameStateService.setAccountLevel(state, 50, now);
        sessionStore.get(playerId).updateGameState(state);
        Vector3d farmsteadAnchor = layoutService.createLayoutForCastle(state.castleLocation()).buildingAnchor(BuildingType.FARMSTEAD);

        PlayerGameState placedState = buildingService.placeBuilding(
                playerId,
                BuildingType.FARMSTEAD,
                interiorInstanceService.worldNameFor(playerId),
                farmsteadAnchor,
                now
        ).state();

        assertEquals(interiorInstanceService.worldNameFor(playerId), planner.recommendedWorldName(playerId, placedState, BuildingType.FARMSTEAD));
        assertVector(planner.recommendedPosition(playerId, placedState, BuildingType.FARMSTEAD), farmsteadAnchor.getX(), farmsteadAnchor.getY(), farmsteadAnchor.getZ());
    }

    private PlayerGameStateService gameStateService(String cacheNamespace) {
        JsonMapperProvider mapperProvider = new JsonMapperProvider();
        return new PlayerGameStateService(
                new InMemoryPlayerGameStateStore(),
                new SemanticCacheFactory(new CacheConfig("", 6379, "", false)).build(cacheNamespace),
                new JacksonCacheCodec<>(mapperProvider.mapper(), PlayerGameState.class, cacheNamespace),
                mapperProvider.mapper()
        );
    }

    private PlayerGameState seedSession(
            PlayerSessionStore sessionStore,
            PlayerGameStateService gameStateService,
            UUID playerId,
            Instant now
    ) {
        PlayerGameState initialState = gameStateService.loadOrCreate(
                310L,
                playerId,
                new CastleLocationData("overworld", 10.0D, 72.0D, 10.0D),
                now
        );
        sessionStore.put(new PlayerSession(
                playerId,
                new PlayerProfile(310L, playerId, "PlannerBot", "UTC", "hash", now, now, now),
                initialState
        ));
        return initialState;
    }

    private void assertVector(Vector3d actual, double expectedX, double expectedY, double expectedZ) {
        assertEquals(expectedX, actual.getX(), 0.0001D);
        assertEquals(expectedY, actual.getY(), 0.0001D);
        assertEquals(expectedZ, actual.getZ(), 0.0001D);
    }
}
