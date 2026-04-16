package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.server.core.HytaleServer;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleBuildingService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleBuildingVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleEconomySimulationService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleSiteVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerGameStateService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerSessionStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiNavigator;
import com.tavall.hytale.resourcegame.domain.CastleEconomySnapshot;
import com.tavall.hytale.resourcegame.domain.CitizenMetaData;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.PopulationSummary;
import com.tavall.hytale.resourcegame.domain.ResourceInventory;
import com.tavall.hytale.resourcegame.resources.ResourceType;
import com.tavall.hytale.resourcegame.tasks.AsyncTask;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Runs the lightweight castle economy tick for active sessions.
 */
public final class CastleEconomySimulationService implements ICastleEconomySimulationService, IDependencyInjectableConcrete {
    private static final Logger LOGGER = Logger.getLogger(CastleEconomySimulationService.class.getName());
    public static final long TICK_INTERVAL_SECONDS = 12L;

    private final IPlayerSessionStore sessionStore;
    private final IPlayerGameStateService gameStateService;
    private final ICastleBuildingService buildingService;
    private final ICastleBuildingVisualService buildingVisualService;
    private final ICastleSiteVisualService castleSiteVisualService;
    private final CastleEconomyPlanner planner;
    private final IResourceNodeService resourceNodeService;
    private final IResourceNodeVisualService resourceNodeVisualService;
    private final IUiNavigator uiNavigator;
    private ScheduledFuture<?> tickTask;

    public CastleEconomySimulationService(
            IPlayerSessionStore sessionStore,
            IPlayerGameStateService gameStateService,
            ICastleBuildingService buildingService,
            ICastleBuildingVisualService buildingVisualService,
            ICastleSiteVisualService castleSiteVisualService,
            CastleEconomyPlanner planner,
            IResourceNodeService resourceNodeService,
            IResourceNodeVisualService resourceNodeVisualService,
            IUiNavigator uiNavigator
    ) {
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.gameStateService = Objects.requireNonNull(gameStateService, "gameStateService");
        this.buildingService = Objects.requireNonNull(buildingService, "buildingService");
        this.buildingVisualService = Objects.requireNonNull(buildingVisualService, "buildingVisualService");
        this.castleSiteVisualService = Objects.requireNonNull(castleSiteVisualService, "castleSiteVisualService");
        this.planner = Objects.requireNonNull(planner, "planner");
        this.resourceNodeService = Objects.requireNonNull(resourceNodeService, "resourceNodeService");
        this.resourceNodeVisualService = Objects.requireNonNull(resourceNodeVisualService, "resourceNodeVisualService");
        this.uiNavigator = Objects.requireNonNull(uiNavigator, "uiNavigator");
    }

    @Override
    public void start() {
        if (tickTask != null && !tickTask.isCancelled()) {
            return;
        }
        tickTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                () -> runTick(Instant.now()),
                TICK_INTERVAL_SECONDS,
                TICK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    @Override
    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel(false);
            tickTask = null;
        }
    }

    public void runTick(Instant now) {
        for (PlayerSession session : sessionStore.snapshot()) {
            applyTick(session, now);
        }
    }

    private void applyTick(PlayerSession session, Instant now) {
        PlayerGameState state = session.gameState();
        CastleEconomySnapshot snapshot = planner.snapshot(state);
        CitizenMetaData citizenMetaData = state.populationSummary().citizenMetaData().withJobCounts(snapshot.jobCounts());
        PopulationSummary updatedSummary = new PopulationSummary(
                state.populationSummary().citizenCount(),
                state.populationSummary().troopCount(),
                citizenMetaData,
                state.populationSummary().troopMetaData(),
                state.populationSummary().agingState().tick(now)
        );
        ResourceInventory updatedResources = state.resources()
                .withFood(state.resources().food() + snapshot.gainFor(ResourceType.FOOD))
                .withWood(state.resources().wood() + snapshot.gainFor(ResourceType.WOOD))
                .withIron(state.resources().iron() + snapshot.gainFor(ResourceType.IRON));
        PlayerGameState updatedState = state.withPopulation(updatedSummary, now).withResources(updatedResources, now);
        updatedState = resourceNodeService.applyTick(updatedState, now);
        updatedState = buildingService.applyTick(session.playerId(), updatedState, now);
        session.updateGameState(updatedState);
        gameStateService.cacheState(session.playerId(), updatedState);
        castleSiteVisualService.refreshSite(session.playerId(), updatedState);
        buildingVisualService.refreshBuildings(session.playerId(), updatedState);
        resourceNodeVisualService.refreshNodes(session.playerId(), updatedState);
        uiNavigator.refreshTrackedPage(session.playerId(), updatedState);
        PlayerGameState persistedState = updatedState;
        AsyncTask.runAsync(() -> gameStateService.persistState(persistedState, now));
        LOGGER.fine(() -> "Economy tick applied for " + session.playerId());
    }
}
