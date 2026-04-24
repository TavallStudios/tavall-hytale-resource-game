package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
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
import com.tavall.hytale.resourcegame.tasks.WorldTasks;
import com.tavall.hytale.resourcegame.ui.UiPageType;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Handles interior transitions within the same server instance.
 */
public final class InteriorWorldService implements IInteriorWorldService, IDependencyInjectableConcrete {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Duration TRANSITION_TIMEOUT = Duration.ofSeconds(20L);
    private static final long PAGE_CLOSE_BUFFER_MILLIS = 250L;
    private static final long EXIT_UI_DELAY_MILLIS = 750L;
    private static final long INTERIOR_READY_RETRY_MILLIS = 250L;
    private static final int INTERIOR_READY_MAX_RETRIES = 48;
    private static final int EXIT_READY_MAX_RETRIES = 48;

    private final AtomicLong transitionSequence = new AtomicLong();
    private final ConcurrentHashMap<UUID, TransitionToken> transitionStates = new ConcurrentHashMap<>();
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
        if (!beginTransition(playerId, TransitionKind.ENTER)) {
            LOGGER.at(Level.INFO).log("Interior entry ignored for %s because a transition is already in progress.", playerId);
            return;
        }
        TransitionToken transitionToken = transitionStates.get(playerId);
        withLivePlayer(playerId, (livePlayer) -> {
            if (!isActiveTransition(playerId, transitionToken)) {
                return;
            }
            closeCurrentPage(livePlayer);
            uiNavigator.clearTrackedPage(playerId);
            if (livePlayer.getWorld() == null) {
                LOGGER.at(Level.WARNING).log("Interior entry skipped because player %s is no longer available.", playerId);
                completeTransition(playerId, transitionToken);
                return;
            }
            LOGGER.at(Level.INFO).log("Beginning interior entry for %s (%s).", livePlayer.getDisplayName(), playerId);
            beginInteriorEntry(livePlayer, session, state, castleLocation, transitionToken);
        });
    }

    private void beginInteriorEntry(
            Player player,
            PlayerSession session,
            PlayerGameState state,
            CastleLocationData castleLocation,
            TransitionToken transitionToken
    ) {
        UUID playerId = player.getUuid();
        int interiorIndex = gameStateService.interiorInstanceIndex(state);
        InteriorLayout layout = layoutService.createLayoutForCastle(castleLocation, interiorIndex);
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
        PlayerGameState finalTutorialState = tutorialState;
        CompletableFuture<World> interiorWorldFuture = interiorInstanceService.resolveInteriorWorld(playerId);
        interiorWorldFuture.whenComplete((interiorWorld, throwable) -> {
            if (!isActiveTransition(playerId, transitionToken)) {
                return;
            }
            if (throwable != null) {
                Throwable rootCause = rootCause(throwable);
                LOGGER.at(Level.SEVERE).withCause(rootCause).log(
                        "Interior world resolve failed for %s (%s).",
                        player.getDisplayName(),
                        playerId
                );
                sendPlayerMessage(playerId, Message.raw("Interior transfer failed: " + safeMessage(rootCause)).color("red"));
                completeTransition(playerId, transitionToken);
                return;
            }
            if (interiorWorld == null) {
                LOGGER.at(Level.WARNING).log("Interior entry failed because no interior world was resolved for %s.", playerId);
                sendPlayerMessage(playerId, Message.raw("Interior transfer failed: no interior world available.").color("red"));
                completeTransition(playerId, transitionToken);
                return;
            }

            InteriorSessionData interiorSession = new InteriorSessionData(
                    interiorWorld.getName(),
                    castleLocation,
                    now
            );
            PlayerGameState updated = finalTutorialState.withInteriorSession(interiorSession, now);
            PlayerSession updatedSession = sessionStore.get(playerId);
            if (updatedSession != null) {
                updatedSession.updateGameState(updated);
            } else {
                session.updateGameState(updated);
            }
            gameStateService.cacheState(playerId, updated);
            AsyncTask.runAsync(() -> gameStateService.persistState(updated, now));

            preloadInteriorChunks(interiorWorld, layout).whenComplete((ignored, preloadThrowable) -> {
                if (!isActiveTransition(playerId, transitionToken)) {
                    return;
                }
                if (preloadThrowable != null) {
                    Throwable rootCause = rootCause(preloadThrowable);
                    LOGGER.at(Level.SEVERE).withCause(rootCause).log(
                            "Interior chunk preload failed for %s (%s).",
                            player.getDisplayName(),
                            playerId
                    );
                    sendPlayerMessage(playerId, Message.raw("Interior transfer failed: " + safeMessage(rootCause)).color("red"));
                    completeTransition(playerId, transitionToken);
                    return;
                }
                try {
                    UiNavigationContext context = prepareInteriorWorld(
                            interiorWorld,
                            player,
                            updated,
                            layout,
                            entryPosition,
                            firstInteriorTutorialPending,
                            firstInteriorTourPending
                    );
                    waitForInteriorReady(
                            playerId,
                            interiorWorld,
                            entryPosition,
                            updated,
                            context,
                            layout,
                            firstInteriorTourPending,
                            INTERIOR_READY_MAX_RETRIES,
                            transitionToken
                    );
                } catch (Throwable exception) {
                    Throwable rootCause = rootCause(exception);
                    LOGGER.at(Level.SEVERE).withCause(rootCause).log(
                            "Interior entry failed for %s (%s).",
                            player.getDisplayName(),
                            playerId
                    );
                    sendPlayerMessage(playerId, Message.raw("Interior transfer failed: " + safeMessage(rootCause)).color("red"));
                    completeTransition(playerId, transitionToken);
                }
            });
        });
    }

    @Override
    public void rebuildInterior(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUuid();
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return;
        }
        PlayerGameState state = session.gameState();
        CastleLocationData castleLocation = state.castleLocation();
        if (castleLocation == null) {
            player.sendMessage(Message.raw("Interior rebuild skipped: castle location missing.").color("red"));
            return;
        }

        int interiorIndex = gameStateService.interiorInstanceIndex(state);
        InteriorLayout targetLayout = layoutService.createLayoutForCastle(castleLocation, interiorIndex);
        InteriorLayout previousLayout = state.interiorSession() != null && state.interiorSession().returnLocation() != null
                ? layoutService.createLayoutForCastle(state.interiorSession().returnLocation(), interiorIndex)
                : targetLayout;
        Vector3d entryPosition = interiorEntryPosition(player, targetLayout);
        boolean tourPending = gameStateService.isInteriorTourPending(state);

        interiorInstanceService.resolveInteriorWorld(playerId).whenComplete((world, throwable) -> {
            if (throwable != null || world == null) {
                Throwable rootCause = rootCause(throwable == null ? new IllegalStateException("missing interior world") : throwable);
                LOGGER.at(Level.WARNING).withCause(rootCause).log("Interior rebuild failed for %s.", playerId);
                sendPlayerMessage(playerId, Message.raw("Interior rebuild failed: " + safeMessage(rootCause)).color("red"));
                return;
            }
            preloadInteriorChunks(world, targetLayout).whenComplete((ignored, preloadThrowable) -> {
                if (preloadThrowable != null) {
                    Throwable rootCause = rootCause(preloadThrowable);
                    LOGGER.at(Level.WARNING).withCause(rootCause).log("Interior rebuild chunk preload failed for %s.", playerId);
                    sendPlayerMessage(playerId, Message.raw("Interior rebuild failed: " + safeMessage(rootCause)).color("red"));
                    return;
                }
                WorldTasks.executeSafe(world, "InteriorWorldService.rebuildInterior", () -> {
                    try {
                        structureService.clearStructure(world, previousLayout);
                    } catch (Throwable ignoredError) {
                    }
                    safeEnsureStructure(world, targetLayout, playerId);
                    interiorTourMarkerService.clearTourMarkers(playerId);
                    displayService.clearDisplays(playerId);
                    safeEnsureTourMarkers(world, targetLayout, playerId, tourPending);
                    safeEnsurePopulationDisplays(world, targetLayout, playerId, state);
                    buildingVisualService.refreshBuildings(playerId, state);
                });
            });
            withLivePlayer(playerId, (livePlayer) -> {
                if (livePlayer.getWorld() != null && livePlayer.getWorld().getName().equals(world.getName())) {
                    safeTeleportToInterior(livePlayer, world, entryPosition);
                    livePlayer.sendMessage(Message.raw("Interior rebuilt.").color("green"));
                    uiNavigator.open(UiPageType.INTERIOR_MAIN, livePlayer, new UiNavigationContext(playerId, livePlayer.getDisplayName()), state);
                }
            });
        });
    }

    @Override
    public void moveInterior(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUuid();
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return;
        }
        PlayerGameState state = session.gameState();
        CastleLocationData castleLocation = state.castleLocation();
        if (castleLocation == null) {
            player.sendMessage(Message.raw("Interior move skipped: castle location missing.").color("red"));
            return;
        }

        int previousIndex = gameStateService.interiorInstanceIndex(state);
        InteriorLayout previousLayout = layoutService.createLayoutForCastle(castleLocation, previousIndex);

        Instant now = Instant.now();
        PlayerGameState updatedState = gameStateService.bumpInteriorInstanceIndex(state, now);
        session.updateGameState(updatedState);
        gameStateService.cacheState(playerId, updatedState);
        AsyncTask.runAsync(() -> gameStateService.persistState(updatedState, now));

        int nextIndex = gameStateService.interiorInstanceIndex(updatedState);
        InteriorLayout targetLayout = layoutService.createLayoutForCastle(castleLocation, nextIndex);
        Vector3d entryPosition = interiorEntryPosition(player, targetLayout);
        boolean tourPending = gameStateService.isInteriorTourPending(updatedState);

        interiorInstanceService.resolveInteriorWorld(playerId).whenComplete((world, throwable) -> {
            if (throwable != null || world == null) {
                Throwable rootCause = rootCause(throwable == null ? new IllegalStateException("missing interior world") : throwable);
                LOGGER.at(Level.WARNING).withCause(rootCause).log("Interior move failed for %s.", playerId);
                sendPlayerMessage(playerId, Message.raw("Interior move failed: " + safeMessage(rootCause)).color("red"));
                return;
            }
            preloadInteriorChunks(world, targetLayout).whenComplete((ignored, preloadThrowable) -> {
                if (preloadThrowable != null) {
                    Throwable rootCause = rootCause(preloadThrowable);
                    LOGGER.at(Level.WARNING).withCause(rootCause).log("Interior move chunk preload failed for %s.", playerId);
                    sendPlayerMessage(playerId, Message.raw("Interior move failed: " + safeMessage(rootCause)).color("red"));
                    return;
                }
                WorldTasks.executeSafe(world, "InteriorWorldService.moveInterior", () -> {
                    try {
                        structureService.clearStructure(world, previousLayout);
                    } catch (Throwable ignoredError) {
                    }
                    safeEnsureStructure(world, targetLayout, playerId);
                    interiorTourMarkerService.clearTourMarkers(playerId);
                    displayService.clearDisplays(playerId);
                    safeEnsureTourMarkers(world, targetLayout, playerId, tourPending);
                    safeEnsurePopulationDisplays(world, targetLayout, playerId, updatedState);
                    buildingVisualService.refreshBuildings(playerId, updatedState);
                });
            });

            withLivePlayer(playerId, (livePlayer) -> {
                if (livePlayer.getWorld() != null && livePlayer.getWorld().getName().equals(world.getName())) {
                    safeTeleportToInterior(livePlayer, world, entryPosition);
                    livePlayer.sendMessage(Message.raw("Interior moved.").color("green"));
                    uiNavigator.open(UiPageType.INTERIOR_MAIN, livePlayer, new UiNavigationContext(playerId, livePlayer.getDisplayName()), updatedState);
                    return;
                }
                livePlayer.sendMessage(Message.raw("Interior moved. Enter /kd interior to visit the new instance.").color("green"));
            });
        });
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
            LOGGER.at(Level.INFO).log("Interior exit ignored for %s because no active interior session exists.", playerId);
            return;
        }
        if (!beginTransition(playerId, TransitionKind.EXIT)) {
            LOGGER.at(Level.INFO).log("Interior exit ignored for %s because a transition is already in progress.", playerId);
            return;
        }
        TransitionToken transitionToken = transitionStates.get(playerId);
        LOGGER.at(Level.INFO).log("Beginning interior exit for %s (%s).", player.getDisplayName(), playerId);
        World transitionWorld = player.getWorld();
        closeCurrentPage(player);
        uiNavigator.clearTrackedPage(playerId);
        if (transitionWorld != null) {
            WorldTasks.executeSafe(transitionWorld, "InteriorWorldService.exitInterior.clearAnchors", () -> {
                interiorTourMarkerService.clearTourMarkers(playerId);
                displayService.clearDisplays(playerId);
            });
        }
        CastleLocationData returnLocation = interiorSession.returnLocation();
        PlayerGameState updated = state.withInteriorSession(null, Instant.now());
        session.updateGameState(updated);
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> executeInteriorExitOnWorld(transitionWorld, playerId, returnLocation, updated, transitionToken),
                PAGE_CLOSE_BUFFER_MILLIS,
                TimeUnit.MILLISECONDS
        );
        gameStateService.cacheState(playerId, updated);
        AsyncTask.runAsync(() -> gameStateService.persistState(updated, Instant.now()));
    }

    private void executeInteriorExitOnWorld(
            World transitionWorld,
            UUID playerId,
            CastleLocationData returnLocation,
            PlayerGameState updated,
            TransitionToken transitionToken
    ) {
        if (!isActiveTransition(playerId, transitionToken)) {
            return;
        }
        if (transitionWorld == null) {
            LOGGER.at(Level.WARNING).log("Interior exit skipped for %s because the transition world is not available.", playerId);
            completeTransition(playerId, transitionToken);
            return;
        }
        WorldTasks.executeSafe(
                transitionWorld,
                "InteriorWorldService.executeInteriorExitOnWorld",
                () -> completeInteriorExit(transitionWorld, playerId, returnLocation, updated, transitionToken)
        );
    }

    private void completeInteriorExit(
            World transitionWorld,
            UUID playerId,
            CastleLocationData returnLocation,
            PlayerGameState updated,
            TransitionToken transitionToken
    ) {
        if (!isActiveTransition(playerId, transitionToken)) {
            return;
        }
        World uiWorld = transitionWorld;
        try {
            var returnWorld = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(returnLocation.worldName());
            if (returnWorld == null) {
                LOGGER.at(Level.WARNING).log(
                        "Interior exit return world %s is not available for %s; castle UI will still reopen.",
                        returnLocation.worldName(),
                        playerId
                );
                completeTransition(playerId, transitionToken);
                return;
            }
            uiWorld = returnWorld;
            Vector3d returnPosition = new Vector3d(returnLocation.x(), returnLocation.y(), returnLocation.z());
            withLivePlayer(playerId, (livePlayer) -> safelyMovePlayerToReturnLocation(livePlayer, returnWorld, returnPosition));
        } catch (Throwable throwable) {
            Throwable rootCause = rootCause(throwable);
            LOGGER.at(Level.WARNING).withCause(throwable).log(
                    "Interior exit completion failed for %s; castle UI will still attempt to reopen. cause=%s: %s",
                    playerId,
                    rootCause.getClass().getName(),
                    safeMessage(rootCause)
            );
        } finally {
            scheduleCastleUiAfterExit(uiWorld == null ? null : uiWorld.getName(), playerId, updated, transitionToken);
            scheduleInteriorRelease(playerId);
        }
    }

    private void scheduleCastleUiAfterExit(
            String expectedWorldName,
            UUID playerId,
            PlayerGameState updated,
            TransitionToken transitionToken
    ) {
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> {
                    if (!isActiveTransition(playerId, transitionToken)) {
                        return;
                    }
                    waitForCastleUiAfterExit(playerId, expectedWorldName, updated, EXIT_READY_MAX_RETRIES, transitionToken);
                },
                EXIT_UI_DELAY_MILLIS,
                TimeUnit.MILLISECONDS
        );
    }

    private void waitForCastleUiAfterExit(
            UUID playerId,
            String expectedWorldName,
            PlayerGameState updated,
            int retriesRemaining,
            TransitionToken transitionToken
    ) {
        if (!isActiveTransition(playerId, transitionToken)) {
            return;
        }
        if (expectedWorldName == null || expectedWorldName.isBlank()) {
            if (retriesRemaining <= 0) {
                LOGGER.at(Level.WARNING).log(
                        "Interior exit UI reopen timed out for %s waiting for world %s.",
                        playerId,
                        expectedWorldName
                );
                completeTransition(playerId, transitionToken);
                return;
            }
            retryCastleUiAfterExit(playerId, expectedWorldName, updated, retriesRemaining - 1, transitionToken);
            return;
        }
        World expectedWorld = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(expectedWorldName);
        if (expectedWorld == null) {
            if (retriesRemaining <= 0) {
                LOGGER.at(Level.WARNING).log(
                        "Interior exit UI reopen timed out for %s because world %s is unavailable.",
                        playerId,
                        expectedWorldName
                );
                completeTransition(playerId, transitionToken);
                return;
            }
            retryCastleUiAfterExit(playerId, expectedWorldName, updated, retriesRemaining - 1, transitionToken);
            return;
        }

        WorldTasks.executeSafe(expectedWorld, "InteriorWorldService.waitForCastleUiAfterExit", () -> {
            if (!isActiveTransition(playerId, transitionToken)) {
                return;
            }
            Ref<EntityStore> playerRef = expectedWorld.getEntityRef(playerId);
            if (playerRef == null || !playerRef.isValid()) {
                if (retriesRemaining <= 0) {
                    LOGGER.at(Level.WARNING).log(
                            "Interior exit UI reopen timed out for %s after returning to world %s.",
                            playerId,
                            expectedWorldName
                    );
                    completeTransition(playerId, transitionToken);
                    return;
                }
                retryCastleUiAfterExit(playerId, expectedWorldName, updated, retriesRemaining - 1, transitionToken);
                return;
            }
            Store<EntityStore> store = playerRef.getStore();
            Player livePlayer = store.getComponent(playerRef, Player.getComponentType());
            if (livePlayer == null || livePlayer.getWorld() == null
                    || (expectedWorldName != null && !expectedWorldName.equals(livePlayer.getWorld().getName()))) {
                if (retriesRemaining <= 0) {
                    LOGGER.at(Level.WARNING).log(
                            "Interior exit UI reopen timed out for %s after returning to world %s.",
                            playerId,
                            expectedWorldName
                    );
                    completeTransition(playerId, transitionToken);
                    return;
                }
                retryCastleUiAfterExit(playerId, expectedWorldName, updated, retriesRemaining - 1, transitionToken);
                return;
            }
            try {
                LOGGER.at(Level.INFO).log("Interior exit complete for %s; opening castle UI.", livePlayer.getDisplayName());
                completeTransition(playerId, transitionToken);
                uiNavigator.open(
                        UiPageType.CASTLE_MAIN,
                        livePlayer,
                        new UiNavigationContext(playerId, livePlayer.getDisplayName()),
                        updated
                );
            } catch (Throwable throwable) {
                Throwable rootCause = rootCause(throwable);
                LOGGER.at(Level.SEVERE).withCause(throwable).log(
                        "Interior exit UI reopen failed for %s. cause=%s: %s",
                        playerId,
                        rootCause.getClass().getName(),
                        safeMessage(rootCause)
                );
                completeTransition(playerId, transitionToken);
            }
        });
    }

    private void retryCastleUiAfterExit(
            UUID playerId,
            String expectedWorldName,
            PlayerGameState updated,
            int retriesRemaining,
            TransitionToken transitionToken
    ) {
        if (!isActiveTransition(playerId, transitionToken)) {
            return;
        }
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> waitForCastleUiAfterExit(playerId, expectedWorldName, updated, retriesRemaining, transitionToken),
                INTERIOR_READY_RETRY_MILLIS,
                TimeUnit.MILLISECONDS
        );
    }

    private void scheduleInteriorRelease(UUID playerId) {
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> interiorInstanceService.releaseInteriorWorld(playerId),
                EXIT_UI_DELAY_MILLIS,
                TimeUnit.MILLISECONDS
        );
    }

    private void safelyMovePlayerToReturnLocation(
            Player livePlayer,
            com.hypixel.hytale.server.core.universe.world.World returnWorld,
            Vector3d returnPosition
    ) {
        try {
            if (livePlayer.getWorld() != null && livePlayer.getWorld().getName().equals(returnWorld.getName())) {
                playerTeleportService.moveWithoutTeleportAck(livePlayer, returnPosition);
            } else {
                WorldTasks.executeSafe(
                        returnWorld,
                        "InteriorWorldService.safelyMovePlayerToReturnLocation",
                        () -> playerTeleportService.teleport(livePlayer, returnWorld, returnPosition)
                );
            }
        } catch (Throwable throwable) {
            Throwable rootCause = rootCause(throwable);
            LOGGER.at(Level.WARNING).withCause(rootCause).log(
                    "Interior exit movement failed for %s into %s; castle UI will still reopen. cause=%s: %s",
                    livePlayer.getUuid(),
                    returnWorld.getName(),
                    rootCause.getClass().getName(),
                    safeMessage(rootCause)
            );
        }
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
        WorldTasks.executeSafe(world, "InteriorWorldService.prepareInteriorWorld", () -> {
            safeEnsureStructure(world, layout, player.getUuid());
            safeTeleportToInterior(player, world, entryPosition);
        });
        String tutorialMessage = (firstInteriorTutorialPending || firstInteriorTourPending)
                ? "Step 1: follow the tour markers. Step 2: inspect the citizen and troop anchors. Step 3: leave through the exit lane when you are done."
                : "Interior tutorial complete: citizen and troop anchors stay here while the upgrade pipeline grows.";
        return new UiNavigationContext(player.getUuid(), player.getDisplayName(), tutorialMessage);
    }

    private void safeEnsureStructure(
            com.hypixel.hytale.server.core.universe.world.World world,
            InteriorLayout layout,
            UUID playerId
    ) {
        try {
            structureService.ensureStructure(world, layout);
        } catch (Throwable throwable) {
            LOGGER.at(Level.WARNING).withCause(throwable).log("Interior structure setup failed for %s.", playerId);
        }
    }

    private void safeEnsureTourMarkers(
            com.hypixel.hytale.server.core.universe.world.World world,
            InteriorLayout layout,
            UUID playerId,
            boolean firstInteriorTourPending
    ) {
        try {
            interiorTourMarkerService.ensureTourMarkers(playerId, world, layout, firstInteriorTourPending);
        } catch (Throwable throwable) {
            LOGGER.at(Level.WARNING).withCause(throwable).log("Interior tour marker setup failed for %s.", playerId);
        }
    }

    private void safeEnsurePopulationDisplays(
            com.hypixel.hytale.server.core.universe.world.World world,
            InteriorLayout layout,
            UUID playerId,
            PlayerGameState updated
    ) {
        try {
            displayService.ensureDisplays(playerId, world, layout, updated.populationSummary());
        } catch (Throwable throwable) {
            LOGGER.at(Level.WARNING).withCause(throwable).log("Interior population display setup failed for %s.", playerId);
        }
    }

    private void safeTeleportToInterior(
            Player player,
            com.hypixel.hytale.server.core.universe.world.World world,
            Vector3d entryPosition
    ) {
        try {
            if (player.getWorld() != null && player.getWorld().getName().equals(world.getName())) {
                playerTeleportService.moveWithoutTeleportAck(player, entryPosition);
                return;
            }
            playerTeleportService.teleport(player, world, entryPosition);
        } catch (Throwable throwable) {
            Throwable rootCause = rootCause(throwable);
            LOGGER.at(Level.WARNING).withCause(rootCause).log(
                    "Interior teleport scheduling failed for %s into %s. cause=%s: %s",
                    player.getUuid(),
                    world.getName(),
                    rootCause.getClass().getName(),
                    safeMessage(rootCause)
            );
        }
    }

    private void waitForInteriorReady(
            UUID playerId,
            com.hypixel.hytale.server.core.universe.world.World targetWorld,
            Vector3d entryPosition,
            PlayerGameState updatedState,
            UiNavigationContext context,
            InteriorLayout layout,
            boolean firstInteriorTourPending,
            int retriesRemaining,
            TransitionToken transitionToken
    ) {
        if (!isActiveTransition(playerId, transitionToken)) {
            return;
        }
        if (targetWorld == null) {
            if (retriesRemaining <= 0) {
                LOGGER.at(Level.WARNING).log("Interior entry timed out for %s because the target world is not available.", playerId);
                completeTransition(playerId, transitionToken);
                return;
            }
            HytaleServer.SCHEDULED_EXECUTOR.schedule(
                    () -> waitForInteriorReady(playerId, targetWorld, entryPosition, updatedState, context, layout, firstInteriorTourPending, retriesRemaining - 1, transitionToken),
                    INTERIOR_READY_RETRY_MILLIS,
                    TimeUnit.MILLISECONDS
            );
            return;
        }

        WorldTasks.executeSafe(targetWorld, "InteriorWorldService.waitForInteriorReady", () -> {
            if (!isActiveTransition(playerId, transitionToken)) {
                return;
            }
            Ref<EntityStore> playerRef = targetWorld.getEntityRef(playerId);
            if (playerRef == null || !playerRef.isValid()) {
                if (retriesRemaining <= 0) {
                    LOGGER.at(Level.WARNING).log("Interior entry timed out for %s before the live player reference became available.", playerId);
                    completeTransition(playerId, transitionToken);
                    return;
                }
                HytaleServer.SCHEDULED_EXECUTOR.schedule(
                        () -> waitForInteriorReady(playerId, targetWorld, entryPosition, updatedState, context, layout, firstInteriorTourPending, retriesRemaining - 1, transitionToken),
                        INTERIOR_READY_RETRY_MILLIS,
                        TimeUnit.MILLISECONDS
                );
                return;
            }
            Store<EntityStore> store = playerRef.getStore();
            Player livePlayer = store.getComponent(playerRef, Player.getComponentType());
            if (livePlayer == null) {
                if (retriesRemaining <= 0) {
                    LOGGER.at(Level.WARNING).log("Interior entry timed out for %s before the live player reference became available.", playerId);
                    completeTransition(playerId, transitionToken);
                    return;
                }
                HytaleServer.SCHEDULED_EXECUTOR.schedule(
                        () -> waitForInteriorReady(playerId, targetWorld, entryPosition, updatedState, context, layout, firstInteriorTourPending, retriesRemaining - 1, transitionToken),
                        INTERIOR_READY_RETRY_MILLIS,
                        TimeUnit.MILLISECONDS
                );
                return;
            }
            if (isInteriorReady(livePlayer, targetWorld, entryPosition)) {
                scheduleInteriorAnchors(targetWorld, layout, playerId, updatedState, firstInteriorTourPending);
                buildingVisualService.refreshBuildings(playerId, updatedState);
                livePlayer.sendMessage(Message.raw("Interior ready. Building placement and interaction are now available.").color("green"));
                completeTransition(playerId, transitionToken);
                uiNavigator.open(UiPageType.INTERIOR_MAIN, livePlayer, context, updatedState);
                return;
            }
            if (retriesRemaining <= 0) {
                LOGGER.at(Level.WARNING).log("Interior entry timed out for %s in world %s.", playerId, targetWorld.getName());
                livePlayer.sendMessage(Message.raw("Interior transfer timed out. Try /kd interior again.").color("red"));
                completeTransition(playerId, transitionToken);
                return;
            }
            HytaleServer.SCHEDULED_EXECUTOR.schedule(
                    () -> waitForInteriorReady(playerId, targetWorld, entryPosition, updatedState, context, layout, firstInteriorTourPending, retriesRemaining - 1, transitionToken),
                    INTERIOR_READY_RETRY_MILLIS,
                    TimeUnit.MILLISECONDS
            );
        });
    }

    private void scheduleInteriorAnchors(
            com.hypixel.hytale.server.core.universe.world.World world,
            InteriorLayout layout,
            UUID playerId,
            PlayerGameState updated,
            boolean firstInteriorTourPending
    ) {
        if (world == null || layout == null || playerId == null || updated == null) {
            return;
        }
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> WorldTasks.executeSafe(world, "InteriorWorldService.scheduleInteriorAnchors", () -> {
                    if (!isInteriorSessionActive(playerId, world.getName())) {
                        return;
                    }
                    safeEnsureTourMarkers(world, layout, playerId, firstInteriorTourPending);
                    safeEnsurePopulationDisplays(world, layout, playerId, updated);
                }),
                400L,
                TimeUnit.MILLISECONDS
        );
    }

    private boolean isInteriorReady(Player livePlayer, com.hypixel.hytale.server.core.universe.world.World targetWorld, Vector3d entryPosition) {
        return livePlayer.getWorld() != null && targetWorld.getName().equals(livePlayer.getWorld().getName());
    }

    private Vector3d interiorEntryPosition(Player player, InteriorLayout layout) {
        Vector3f rotation = player.getTransformComponent().getRotation();
        Vector3d standingEntryPoint = new Vector3d(
                layout.entryPoint().getX(),
                layout.entryPoint().getY() + 1.0D,
                layout.entryPoint().getZ()
        );
        Transform targetTransform = new Transform(standingEntryPoint, rotation);
        return playerTeleportService.standingPosition(player, targetTransform.getPosition());
    }

    private Ref<EntityStore> resolveLivePlayerRef(UUID playerId) {
        var universe = com.hypixel.hytale.server.core.universe.Universe.get();
        if (universe == null) {
            return null;
        }
        for (var playerRef : universe.getPlayers()) {
            if (!playerRef.getUuid().equals(playerId)) {
                continue;
            }
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) {
                return null;
            }
            return ref;
        }
        return null;
    }

    private void sendPlayerMessage(UUID playerId, Message message) {
        withLivePlayer(playerId, livePlayer -> livePlayer.sendMessage(message));
    }

    private void withLivePlayer(UUID playerId, Consumer<Player> action) {
        if (playerId == null || action == null) {
            return;
        }
        Ref<EntityStore> ref = resolveLivePlayerRef(playerId);
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        com.hypixel.hytale.server.core.universe.world.World world = ((EntityStore) store.getExternalData()).getWorld();
        if (world == null) {
            return;
        }
        WorldTasks.executeSafe(world, "InteriorWorldService.withLivePlayer", () -> {
            if (!ref.isValid()) {
                return;
            }
            Player livePlayer = store.getComponent(ref, Player.getComponentType());
            if (livePlayer != null) {
                action.accept(livePlayer);
            }
        });
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
        if (!ref.isValid()) {
            return;
        }
        Player livePlayer = store.getComponent(ref, Player.getComponentType());
        if (livePlayer == null) {
            return;
        }
        livePlayer.getPageManager().setPage(ref, store, Page.None);
    }

    private boolean isInteriorSessionActive(UUID playerId, String worldName) {
        if (playerId == null || worldName == null || worldName.isBlank()) {
            return false;
        }
        PlayerSession session = sessionStore.get(playerId);
        if (session == null || session.gameState() == null || session.gameState().interiorSession() == null) {
            return false;
        }
        if (!worldName.equals(session.gameState().interiorSession().interiorWorldName())) {
            return false;
        }
        Ref<EntityStore> ref = resolveLivePlayerRef(playerId);
        if (ref == null || !ref.isValid()) {
            return false;
        }
        World currentWorld = ((EntityStore) ref.getStore().getExternalData()).getWorld();
        return currentWorld != null && worldName.equals(currentWorld.getName());
    }

    private CompletableFuture<Void> preloadInteriorChunks(World world, InteriorLayout layout) {
        if (world == null || layout == null) {
            return CompletableFuture.completedFuture(null);
        }
        Set<Long> chunkIndexes = new LinkedHashSet<>();
        registerChunk(chunkIndexes, layout.origin());
        registerChunk(chunkIndexes, layout.entryPoint());
        registerChunk(chunkIndexes, layout.exitPoint());
        registerChunk(chunkIndexes, layout.workerPlatformAnchor());
        registerChunk(chunkIndexes, layout.workerPortalAnchor());
        CompletableFuture<?>[] futures = chunkIndexes.stream()
                .map(world::getChunkAsync)
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }

    private void registerChunk(Set<Long> chunkIndexes, Vector3d position) {
        if (chunkIndexes == null || position == null) {
            return;
        }
        chunkIndexes.add(ChunkUtil.indexChunkFromBlock(
                (int) Math.floor(position.getX()),
                (int) Math.floor(position.getZ())
        ));
    }

    private boolean beginTransition(UUID playerId, TransitionKind kind) {
        if (playerId == null || kind == null) {
            return false;
        }
        while (true) {
            TransitionToken existing = transitionStates.get(playerId);
            if (existing == null) {
                TransitionToken token = new TransitionToken(transitionSequence.incrementAndGet(), kind, Instant.now());
                return transitionStates.putIfAbsent(playerId, token) == null;
            }
            if (!isTransitionExpired(existing)) {
                return false;
            }
            if (transitionStates.remove(playerId, existing)) {
                LOGGER.at(Level.WARNING).log(
                        "Cleared stale interior %s transition for %s after %s ms.",
                        existing.kind(),
                        playerId,
                        Duration.between(existing.startedAt(), Instant.now()).toMillis()
                );
            }
        }
    }

    private boolean isActiveTransition(UUID playerId, TransitionToken token) {
        return playerId != null && token != null && token.equals(transitionStates.get(playerId));
    }

    private void completeTransition(UUID playerId, TransitionToken token) {
        if (playerId == null || token == null) {
            return;
        }
        transitionStates.remove(playerId, token);
    }

    private boolean isTransitionExpired(TransitionToken token) {
        if (token == null || token.startedAt() == null) {
            return false;
        }
        return Duration.between(token.startedAt(), Instant.now()).compareTo(TRANSITION_TIMEOUT) > 0;
    }

    private enum TransitionKind {
        ENTER,
        EXIT
    }

    private record TransitionToken(long sequence, TransitionKind kind, Instant startedAt) {
    }
}
