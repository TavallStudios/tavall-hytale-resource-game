package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInteriorInstanceService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInteriorWorldService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerGameStateService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerSessionStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerTeleportService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiNavigator;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.InteriorSessionData;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;
import com.tavall.hytale.resourcegame.interior.InteriorLayout;
import com.tavall.hytale.resourcegame.interior.InteriorLayoutService;
import com.tavall.hytale.resourcegame.interior.InteriorStructureService;
import com.tavall.hytale.resourcegame.tasks.AsyncTask;
import com.tavall.hytale.resourcegame.ui.UiPageType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Handles interior transitions within the same server instance.
 */
public final class InteriorWorldService implements IInteriorWorldService, IDependencyInjectableConcrete {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Vector3d INTERIOR_ORIGIN = new Vector3d(0.5, 120.0, 0.5);
    private static final long INTERIOR_UI_DELAY_MILLIS = 3000L;
    private static final long EXIT_UI_DELAY_MILLIS = 2000L;

    private final IPlayerSessionStore sessionStore;
    private final IPlayerGameStateService gameStateService;
    private final IInteriorInstanceService interiorInstanceService;
    private final InteriorLayoutService layoutService;
    private final InteriorStructureService structureService;
    private final InteriorTourMarkerService interiorTourMarkerService;
    private final IPlayerTeleportService playerTeleportService;
    private final PopulationDisplayGateway displayService;
    private final IUiNavigator uiNavigator;

    public InteriorWorldService(
            IPlayerSessionStore sessionStore,
            IPlayerGameStateService gameStateService,
            IInteriorInstanceService interiorInstanceService,
            InteriorLayoutService layoutService,
            InteriorStructureService structureService,
            InteriorTourMarkerService interiorTourMarkerService,
            IPlayerTeleportService playerTeleportService,
            PopulationDisplayGateway displayService,
            IUiNavigator uiNavigator
    ) {
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.gameStateService = Objects.requireNonNull(gameStateService, "gameStateService");
        this.interiorInstanceService = Objects.requireNonNull(interiorInstanceService, "interiorInstanceService");
        this.layoutService = Objects.requireNonNull(layoutService, "layoutService");
        this.structureService = Objects.requireNonNull(structureService, "structureService");
        this.interiorTourMarkerService = Objects.requireNonNull(interiorTourMarkerService, "interiorTourMarkerService");
        this.playerTeleportService = Objects.requireNonNull(playerTeleportService, "playerTeleportService");
        this.displayService = Objects.requireNonNull(displayService, "displayService");
        this.uiNavigator = Objects.requireNonNull(uiNavigator, "uiNavigator");
    }

    public void enterInterior(Player player) {
        UUID playerId = player.getUuid();
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return;
        }
        PlayerGameState state = session.gameState();
        CastleLocationData castleLocation = state.castleLocation();
        if (castleLocation == null) {
            return;
        }
        String interiorWorldName = interiorInstanceService.worldNameFor(playerId);
        InteriorLayout layout = layoutService.createLayout(INTERIOR_ORIGIN);
        Vector3d entryPosition = interiorEntryPosition(player, layout);
        Instant now = Instant.now();
        boolean firstInteriorTutorialPending = gameStateService.isInteriorTutorialPending(state);
        boolean firstInteriorTourPending = gameStateService.isInteriorTourPending(state);
        PlayerGameState tutorialState = state;
        if (firstInteriorTutorialPending) {
            tutorialState = gameStateService.markInteriorTutorialSeen(tutorialState, now);
        }
        if (firstInteriorTourPending) {
            tutorialState = gameStateService.markInteriorTourSeen(tutorialState, now);
        }
        InteriorSessionData interiorSession = new InteriorSessionData(
                interiorWorldName,
                castleLocation,
                now
        );
        PlayerGameState updated = tutorialState.withInteriorSession(interiorSession, now);
        session.updateGameState(updated);
        CompletableFuture<UiNavigationContext> transition = interiorInstanceService.resolveInteriorWorld(playerId)
                .thenApply(world -> prepareInteriorWorld(
                        world,
                        player,
                        updated,
                        layout,
                        entryPosition,
                        firstInteriorTutorialPending,
                        firstInteriorTourPending
                ));
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> transition.thenAccept(context -> uiNavigator.open(UiPageType.INTERIOR_MAIN, player, context, updated)),
                INTERIOR_UI_DELAY_MILLIS,
                TimeUnit.MILLISECONDS
        );
        gameStateService.cacheState(playerId, updated);
        AsyncTask.runAsync(() -> gameStateService.persistState(updated, now));
    }

    public void exitInterior(Player player) {
        UUID playerId = player.getUuid();
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return;
        }
        PlayerGameState state = session.gameState();
        InteriorSessionData interiorSession = state.interiorSession();
        if (interiorSession == null) {
            return;
        }
        CastleLocationData returnLocation = interiorSession.returnLocation();
        PlayerGameState updated = state.withInteriorSession(null, Instant.now());
        session.updateGameState(updated);
        interiorInstanceService.resolveInteriorWorld(playerId).thenAccept(ignored -> {
            var returnWorld = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(returnLocation.worldName());
            if (returnWorld == null) {
                LOGGER.at(Level.WARNING).log("Interior exit skipped because return world %s is not available.", returnLocation.worldName());
                return;
            }
            returnWorld.execute(() -> playerTeleportService.teleport(
                    player,
                    returnWorld,
                    new Vector3d(returnLocation.x(), returnLocation.y(), returnLocation.z())
            ));
            HytaleServer.SCHEDULED_EXECUTOR.schedule(
                    () -> uiNavigator.open(UiPageType.CASTLE_MAIN, player, new UiNavigationContext(playerId, player.getDisplayName()), updated),
                    EXIT_UI_DELAY_MILLIS,
                    TimeUnit.MILLISECONDS
            );
            HytaleServer.SCHEDULED_EXECUTOR.schedule(
                    () -> interiorInstanceService.releaseInteriorWorld(playerId),
                    EXIT_UI_DELAY_MILLIS,
                    TimeUnit.MILLISECONDS
            );
        });
        gameStateService.cacheState(playerId, updated);
        AsyncTask.runAsync(() -> gameStateService.persistState(updated, Instant.now()));
    }

    private UiNavigationContext prepareInteriorWorld(
            com.hypixel.hytale.server.core.universe.world.World world,
            Player player,
            PlayerGameState updated,
            InteriorLayout layout,
            Vector3d entryPosition,
            boolean firstInteriorTutorialPending,
            boolean firstInteriorTourPending
    ) {
        world.execute(() -> {
            structureService.ensureStructure(world, layout);
            interiorTourMarkerService.ensureTourMarkers(player.getUuid(), world, layout, firstInteriorTourPending);
            displayService.ensureDisplays(player.getUuid(), world, layout, updated.populationSummary());
            playerTeleportService.teleport(player, world, entryPosition);
        });
        String tutorialMessage = (firstInteriorTutorialPending || firstInteriorTourPending)
                ? "Step 1: follow the tour markers. Step 2: inspect the citizen and troop anchors. Step 3: leave through the exit lane when you are done."
                : "Interior tutorial complete: citizen and troop anchors stay here while the upgrade pipeline grows.";
        return new UiNavigationContext(player.getUuid(), player.getDisplayName(), tutorialMessage);
    }

    private Vector3d interiorEntryPosition(Player player, InteriorLayout layout) {
        Vector3f rotation = player.getTransformComponent().getRotation();
        Transform targetTransform = new Transform(layout.entryPoint(), rotation);
        return playerTeleportService.standingPosition(player, targetTransform.getPosition());
    }
}
