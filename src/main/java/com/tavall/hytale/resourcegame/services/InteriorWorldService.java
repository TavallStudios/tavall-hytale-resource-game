package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.InteriorSessionData;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;
import com.tavall.hytale.resourcegame.interior.InteriorLayout;
import com.tavall.hytale.resourcegame.interior.InteriorLayoutService;
import com.tavall.hytale.resourcegame.interior.InteriorStructureService;
import com.tavall.hytale.resourcegame.tasks.AsyncTask;
import com.tavall.hytale.resourcegame.ui.UiNavigator;
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
public final class InteriorWorldService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Vector3d INTERIOR_ORIGIN = new Vector3d(0.5, 120.0, 0.5);
    private static final long INTERIOR_UI_DELAY_MILLIS = 3000L;
    private static final long EXIT_UI_DELAY_MILLIS = 2000L;

    private final PlayerSessionStore sessionStore;
    private final PlayerGameStateService gameStateService;
    private final InteriorInstanceService interiorInstanceService;
    private final InteriorLayoutService layoutService;
    private final InteriorStructureService structureService;
    private final PlayerTeleportService playerTeleportService;
    private final PopulationDisplayGateway displayService;
    private final UiNavigator uiNavigator;

    public InteriorWorldService(
            PlayerSessionStore sessionStore,
            PlayerGameStateService gameStateService,
            InteriorInstanceService interiorInstanceService,
            InteriorLayoutService layoutService,
            InteriorStructureService structureService,
            PlayerTeleportService playerTeleportService,
            PopulationDisplayGateway displayService,
            UiNavigator uiNavigator
    ) {
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.gameStateService = Objects.requireNonNull(gameStateService, "gameStateService");
        this.interiorInstanceService = Objects.requireNonNull(interiorInstanceService, "interiorInstanceService");
        this.layoutService = Objects.requireNonNull(layoutService, "layoutService");
        this.structureService = Objects.requireNonNull(structureService, "structureService");
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
        InteriorSessionData interiorSession = new InteriorSessionData(
                interiorWorldName,
                castleLocation,
                Instant.now()
        );
        PlayerGameState updated = state.withInteriorSession(interiorSession, Instant.now());
        session.updateGameState(updated);
        CompletableFuture<UiNavigationContext> transition = interiorInstanceService.resolveInteriorWorld(playerId)
                .thenApply(world -> prepareInteriorWorld(world, player, updated, layout, entryPosition));
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> transition.thenAccept(context -> uiNavigator.open(UiPageType.INTERIOR_MAIN, player, context, updated)),
                INTERIOR_UI_DELAY_MILLIS,
                TimeUnit.MILLISECONDS
        );
        gameStateService.cacheState(playerId, updated);
        AsyncTask.runAsync(() -> gameStateService.persistState(updated, Instant.now()));
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
        });
        gameStateService.cacheState(playerId, updated);
        AsyncTask.runAsync(() -> gameStateService.persistState(updated, Instant.now()));
    }

    private UiNavigationContext prepareInteriorWorld(
            com.hypixel.hytale.server.core.universe.world.World world,
            Player player,
            PlayerGameState updated,
            InteriorLayout layout,
            Vector3d entryPosition
    ) {
        world.execute(() -> {
            structureService.ensureStructure(world, layout);
            displayService.ensureDisplays(player.getUuid(), world, layout, updated.populationSummary());
            playerTeleportService.teleport(player, world, entryPosition);
        });
        return new UiNavigationContext(player.getUuid(), player.getDisplayName());
    }

    private Vector3d interiorEntryPosition(Player player, InteriorLayout layout) {
        Vector3f rotation = player.getTransformComponent().getRotation();
        Transform targetTransform = new Transform(layout.entryPoint(), rotation);
        return playerTeleportService.standingPosition(player, targetTransform.getPosition());
    }
}
