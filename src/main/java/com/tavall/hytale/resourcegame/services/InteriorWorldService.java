package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleBuildingVisualService;
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
    private static final long PAGE_CLOSE_BUFFER_MILLIS = 1500L;
    private static final long EXIT_UI_DELAY_MILLIS = 2000L;
    private static final long INTERIOR_READY_RETRY_MILLIS = 250L;
    private static final int INTERIOR_READY_MAX_RETRIES = 48;

    private final IPlayerSessionStore sessionStore;
    private final IPlayerGameStateService gameStateService;
    private final IInteriorInstanceService interiorInstanceService;
    private final InteriorLayoutService layoutService;
    private final InteriorStructureService structureService;
    private final InteriorTourMarkerService interiorTourMarkerService;
    private final IPlayerTeleportService playerTeleportService;
    private final PopulationDisplayGateway displayService;
    private final ICastleBuildingVisualService buildingVisualService;
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
            ICastleBuildingVisualService buildingVisualService,
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
        this.buildingVisualService = Objects.requireNonNull(buildingVisualService, "buildingVisualService");
        this.uiNavigator = Objects.requireNonNull(uiNavigator, "uiNavigator");
    }

    public void enterInterior(Player player) {
        UUID playerId = player.getUuid();
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            LOGGER.at(Level.WARNING).log("Interior entry ignored for %s because no session is available.", playerId);
            return;
        }
        PlayerGameState state = session.gameState();
        CastleLocationData castleLocation = state.castleLocation();
        if (castleLocation == null) {
            LOGGER.at(Level.WARNING).log("Interior entry ignored for %s because the castle location is missing.", playerId);
            return;
        }
        closeCurrentPage(player);
        uiNavigator.clearTrackedPage(playerId);
        Player livePlayer = resolveLivePlayer(playerId);
        if (livePlayer == null || livePlayer.getWorld() == null) {
            LOGGER.at(Level.WARNING).log("Interior entry skipped because player %s is no longer available.", playerId);
            return;
        }
        LOGGER.at(Level.INFO).log("Beginning interior entry for %s (%s).", livePlayer.getDisplayName(), playerId);
        beginInteriorEntry(livePlayer, session, state, castleLocation);
    }

    private void beginInteriorEntry(Player player, PlayerSession session, PlayerGameState state, CastleLocationData castleLocation) {
        UUID playerId = player.getUuid();
        String interiorWorldName = interiorInstanceService.worldNameFor(playerId);
        InteriorLayout layout = layoutService.createLayoutForPlayer(playerId);
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
        interiorInstanceService.resolveInteriorWorld(playerId)
                .thenAccept(world -> {
                    UiNavigationContext context = prepareInteriorWorld(
                            world,
                            player,
                            updated,
                            layout,
                            entryPosition,
                            firstInteriorTutorialPending,
                            firstInteriorTourPending
                    );
                    waitForInteriorReady(playerId, world, entryPosition, updated, context, INTERIOR_READY_MAX_RETRIES);
                })
                .exceptionally(throwable -> {
                    Throwable rootCause = rootCause(throwable);
                    LOGGER.at(Level.SEVERE).withCause(rootCause).log(
                            "Interior entry failed for %s (%s).",
                            player.getDisplayName(),
                            playerId
                    );
                    Player livePlayer = resolveLivePlayer(playerId);
                    if (livePlayer != null) {
                        livePlayer.sendMessage(Message.raw("Interior transfer failed: " + safeMessage(rootCause)).color("red"));
                    }
                    return null;
                });
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
        closeCurrentPage(player);
        uiNavigator.clearTrackedPage(playerId);
        CastleLocationData returnLocation = interiorSession.returnLocation();
        PlayerGameState updated = state.withInteriorSession(null, Instant.now());
        session.updateGameState(updated);
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> interiorInstanceService.resolveInteriorWorld(playerId).thenAccept(ignored -> {
                    Player livePlayer = resolveLivePlayer(playerId);
                    if (livePlayer == null) {
                        LOGGER.at(Level.WARNING).log("Interior exit skipped because player %s is no longer available.", playerId);
                        return;
                    }
                    var returnWorld = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(returnLocation.worldName());
                    if (returnWorld == null) {
                        LOGGER.at(Level.WARNING).log("Interior exit skipped because return world %s is not available.", returnLocation.worldName());
                        return;
                    }
                    returnWorld.execute(() -> playerTeleportService.teleport(
                            livePlayer,
                            returnWorld,
                            new Vector3d(returnLocation.x(), returnLocation.y(), returnLocation.z())
                    ));
                    HytaleServer.SCHEDULED_EXECUTOR.schedule(
                            () -> uiNavigator.open(UiPageType.CASTLE_MAIN, livePlayer, new UiNavigationContext(playerId, livePlayer.getDisplayName()), updated),
                            EXIT_UI_DELAY_MILLIS,
                            TimeUnit.MILLISECONDS
                    );
                    HytaleServer.SCHEDULED_EXECUTOR.schedule(
                            () -> interiorInstanceService.releaseInteriorWorld(playerId),
                            EXIT_UI_DELAY_MILLIS,
                            TimeUnit.MILLISECONDS
                    );
                }),
                PAGE_CLOSE_BUFFER_MILLIS,
                TimeUnit.MILLISECONDS
        );
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

    private void waitForInteriorReady(
            UUID playerId,
            com.hypixel.hytale.server.core.universe.world.World targetWorld,
            Vector3d entryPosition,
            PlayerGameState updatedState,
            UiNavigationContext context,
            int retriesRemaining
    ) {
        Player livePlayer = resolveLivePlayer(playerId);
        if (livePlayer == null) {
            if (retriesRemaining <= 0) {
                LOGGER.at(Level.WARNING).log("Interior entry timed out for %s before the live player reference became available.", playerId);
                return;
            }
            HytaleServer.SCHEDULED_EXECUTOR.schedule(
                    () -> waitForInteriorReady(playerId, targetWorld, entryPosition, updatedState, context, retriesRemaining - 1),
                    INTERIOR_READY_RETRY_MILLIS,
                    TimeUnit.MILLISECONDS
            );
            return;
        }
        if (livePlayer.getWorld() != null && targetWorld.getName().equals(livePlayer.getWorld().getName())) {
            buildingVisualService.refreshBuildings(playerId, updatedState);
            livePlayer.sendMessage(Message.raw("Interior ready. Building placement and interaction are now available.").color("green"));
            uiNavigator.open(UiPageType.INTERIOR_MAIN, livePlayer, context, updatedState);
            return;
        }
        if (retriesRemaining <= 0) {
            LOGGER.at(Level.WARNING).log("Interior entry timed out for %s in world %s.", playerId, targetWorld.getName());
            livePlayer.sendMessage(Message.raw("Interior transfer timed out. Try /kd interior again.").color("red"));
            return;
        }
        playerTeleportService.teleport(livePlayer, targetWorld, entryPosition);
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> waitForInteriorReady(playerId, targetWorld, entryPosition, updatedState, context, retriesRemaining - 1),
                INTERIOR_READY_RETRY_MILLIS,
                TimeUnit.MILLISECONDS
        );
    }

    private Vector3d interiorEntryPosition(Player player, InteriorLayout layout) {
        Vector3f rotation = player.getTransformComponent().getRotation();
        Transform targetTransform = new Transform(layout.entryPoint(), rotation);
        return playerTeleportService.standingPosition(player, targetTransform.getPosition());
    }

    private Player resolveLivePlayer(UUID playerId) {
        for (var playerRef : com.hypixel.hytale.server.core.universe.Universe.get().getPlayers()) {
            if (!playerRef.getUuid().equals(playerId)) {
                continue;
            }
            var ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) {
                return null;
            }
            return ref.getStore().getComponent(ref, Player.getComponentType());
        }
        return null;
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private String safeMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "unknown error";
        }
        return throwable.getMessage();
    }

    private void closeCurrentPage(Player player) {
        if (player == null || player.getPlayerRef() == null) {
            return;
        }
        var ref = player.getPlayerRef().getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        var store = ref.getStore();
        Player livePlayer = store.getComponent(ref, Player.getComponentType());
        if (livePlayer == null) {
            return;
        }
        livePlayer.getPageManager().setPage(ref, store, Page.None);
    }
}
