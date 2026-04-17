package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
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
    private static final long PAGE_CLOSE_BUFFER_MILLIS = 250L;
    private static final long EXIT_UI_DELAY_MILLIS = 750L;
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
        var interiorWorld = player.getWorld();
        if (interiorWorld == null) {
            LOGGER.at(Level.WARNING).log("Interior entry skipped because player %s has no current world.", playerId);
            return;
        }
        String interiorWorldName = interiorWorld.getName();
        InteriorLayout layout = layoutService.createLayoutForCastle(castleLocation);
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
            waitForInteriorReady(playerId, interiorWorld, entryPosition, updated, context, INTERIOR_READY_MAX_RETRIES);
        } catch (Throwable throwable) {
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
        }
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
            LOGGER.at(Level.INFO).log("Interior exit ignored for %s because no active interior session exists.", playerId);
            return;
        }
        LOGGER.at(Level.INFO).log("Beginning interior exit for %s (%s).", player.getDisplayName(), playerId);
        World transitionWorld = player.getWorld();
        closeCurrentPage(player);
        uiNavigator.clearTrackedPage(playerId);
        CastleLocationData returnLocation = interiorSession.returnLocation();
        PlayerGameState updated = state.withInteriorSession(null, Instant.now());
        session.updateGameState(updated);
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> executeInteriorExitOnWorld(transitionWorld, playerId, returnLocation, updated),
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
            PlayerGameState updated
    ) {
        if (transitionWorld == null) {
            LOGGER.at(Level.WARNING).log("Interior exit skipped for %s because the transition world is not available.", playerId);
            return;
        }
        transitionWorld.execute(() -> completeInteriorExit(transitionWorld, playerId, returnLocation, updated));
    }

    private void completeInteriorExit(
            World transitionWorld,
            UUID playerId,
            CastleLocationData returnLocation,
            PlayerGameState updated
    ) {
        World uiWorld = transitionWorld;
        try {
            Player livePlayer = resolveLivePlayer(playerId);
            if (livePlayer == null) {
                LOGGER.at(Level.WARNING).log("Interior exit movement skipped because player %s is no longer available.", playerId);
                return;
            }
            var returnWorld = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(returnLocation.worldName());
            if (returnWorld == null) {
                LOGGER.at(Level.WARNING).log(
                        "Interior exit return world %s is not available for %s; castle UI will still reopen.",
                        returnLocation.worldName(),
                        playerId
                );
                return;
            }
            uiWorld = returnWorld;
            Vector3d returnPosition = new Vector3d(returnLocation.x(), returnLocation.y(), returnLocation.z());
            safelyMovePlayerToReturnLocation(livePlayer, returnWorld, returnPosition);
        } catch (Throwable throwable) {
            Throwable rootCause = rootCause(throwable);
            LOGGER.at(Level.WARNING).withCause(throwable).log(
                    "Interior exit completion failed for %s; castle UI will still attempt to reopen. cause=%s: %s",
                    playerId,
                    rootCause.getClass().getName(),
                    safeMessage(rootCause)
            );
        } finally {
            scheduleCastleUiAfterExit(uiWorld, playerId, updated);
            scheduleInteriorRelease(playerId);
        }
    }

    private void scheduleCastleUiAfterExit(World world, UUID playerId, PlayerGameState updated) {
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> world.execute(() -> openCastleUiAfterExit(playerId, updated)),
                EXIT_UI_DELAY_MILLIS,
                TimeUnit.MILLISECONDS
        );
    }

    private void openCastleUiAfterExit(UUID playerId, PlayerGameState updated) {
        try {
            Player livePlayer = resolveLivePlayer(playerId);
            if (livePlayer == null) {
                LOGGER.at(Level.WARNING).log("Interior exit UI reopen skipped because player %s is no longer available.", playerId);
                return;
            }
            LOGGER.at(Level.INFO).log("Interior exit complete for %s; opening castle UI.", livePlayer.getDisplayName());
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
        }
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
                returnWorld.execute(() -> playerTeleportService.teleport(livePlayer, returnWorld, returnPosition));
            }
        } catch (Throwable throwable) {
            LOGGER.at(Level.WARNING).withCause(throwable).log(
                    "Interior exit movement failed for %s; castle UI will still reopen.",
                    livePlayer.getUuid()
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
        world.execute(() -> {
            safeEnsureStructure(world, layout, player.getUuid());
            // Entity-backed interior labels currently crash the local Hytale dev server when spawned during entry.
            // Keep the block interior and UI path stable while the hologram/NPC anchor implementation is hardened.
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
            LOGGER.at(Level.WARNING).withCause(throwable).log("Interior teleport scheduling failed for %s.", player.getUuid());
        }
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
        if (isInteriorReady(livePlayer, targetWorld, entryPosition)) {
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
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> waitForInteriorReady(playerId, targetWorld, entryPosition, updatedState, context, retriesRemaining - 1),
                INTERIOR_READY_RETRY_MILLIS,
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
