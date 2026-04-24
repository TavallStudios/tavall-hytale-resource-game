package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleSpawnService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IIpHashService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInteriorInstanceService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IKingdomClockService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleBuildingVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerDataService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerGameStateService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerProfileService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerSessionStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiNavigator;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.PlayerProfile;
import com.tavall.hytale.resourcegame.tasks.AsyncTask;
import com.tavall.hytale.resourcegame.tasks.WorldTasks;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Orchestrates player initialization and persistence.
 */
public final class PlayerDataService implements IPlayerDataService, IDependencyInjectableConcrete {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final IPlayerProfileService profileService;
    private final IPlayerGameStateService gameStateService;
    private final IPlayerSessionStore sessionStore;
    private final ICastleSpawnService castleSpawnService;
    private final IInteriorInstanceService interiorInstanceService;
    private final IIpHashService ipHashService;
    private final IKingdomClockService clockService;
    private final IResourceNodeVisualService resourceNodeVisualService;
    private final ICastleBuildingVisualService buildingVisualService;
    private final PopulationDisplayGateway populationDisplayGateway;
    private final InteriorTourMarkerService interiorTourMarkerService;
    private final IUiNavigator uiNavigator;

    public PlayerDataService(
            IPlayerProfileService profileService,
            IPlayerGameStateService gameStateService,
            IPlayerSessionStore sessionStore,
            ICastleSpawnService castleSpawnService,
            IInteriorInstanceService interiorInstanceService,
            IIpHashService ipHashService,
            IKingdomClockService clockService,
            IResourceNodeVisualService resourceNodeVisualService,
            ICastleBuildingVisualService buildingVisualService,
            PopulationDisplayGateway populationDisplayGateway,
            InteriorTourMarkerService interiorTourMarkerService,
            IUiNavigator uiNavigator
    ) {
        this.profileService = Objects.requireNonNull(profileService, "profileService");
        this.gameStateService = Objects.requireNonNull(gameStateService, "gameStateService");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.castleSpawnService = Objects.requireNonNull(castleSpawnService, "castleSpawnService");
        this.interiorInstanceService = Objects.requireNonNull(interiorInstanceService, "interiorInstanceService");
        this.ipHashService = Objects.requireNonNull(ipHashService, "ipHashService");
        this.clockService = Objects.requireNonNull(clockService, "clockService");
        this.resourceNodeVisualService = Objects.requireNonNull(resourceNodeVisualService, "resourceNodeVisualService");
        this.buildingVisualService = Objects.requireNonNull(buildingVisualService, "buildingVisualService");
        this.populationDisplayGateway = Objects.requireNonNull(populationDisplayGateway, "populationDisplayGateway");
        this.interiorTourMarkerService = Objects.requireNonNull(interiorTourMarkerService, "interiorTourMarkerService");
        this.uiNavigator = Objects.requireNonNull(uiNavigator, "uiNavigator");
    }

    public void handlePlayerReady(PlayerReadyEvent event) {
        ensureSession(event.getPlayer()).whenComplete((session, throwable) -> {
            if (throwable != null) {
                LOGGER.at(Level.SEVERE).withCause(throwable).log(
                        "Failed to initialize session for %s (%s).",
                        event.getPlayer().getDisplayName(),
                        event.getPlayer().getUuid()
                );
            }
        });
    }

    public void handlePlayerDisconnect(PlayerDisconnectEvent event) {
        try {
            if (event == null || event.getPlayerRef() == null) {
                return;
            }
            UUID playerId = event.getPlayerRef().getUuid();
            Ref<EntityStore> ref = event.getPlayerRef().getReference();
            LOGGER.at(Level.INFO).log(
                    "Disconnect cleanup begin for %s on thread %s. refValid=%s",
                    playerId,
                    Thread.currentThread().getName(),
                    ref != null && ref.isValid()
            );
            uiNavigator.clearTrackedPage(playerId);
            handlePlayerDisconnect(playerId);
        } catch (Throwable throwable) {
            LOGGER.at(Level.SEVERE).withCause(throwable).log("Player disconnect handler failed.");
        }
    }

    void handlePlayerDisconnect(UUID playerId) {
        try {
            if (playerId == null) {
                return;
            }
            PlayerSession session = sessionStore.get(playerId);
            if (session == null) {
                return;
            }
            sessionStore.remove(playerId);
            uiNavigator.clearTrackedPage(playerId);
            PlayerProfile profile = session.profile();
            Instant now = Instant.now();
            PlayerGameState persistedState = session.gameState();
            if (persistedState != null && persistedState.interiorSession() != null) {
                persistedState = persistedState.withInteriorSession(null, now);
            }
            populationDisplayGateway.clearDisplays(playerId);
            interiorTourMarkerService.clearTourMarkers(playerId);
            PlayerGameState finalPersistedState = persistedState;
            AsyncTask.runAsync(() -> {
                profileService.persist(profile, now);
                gameStateService.persistState(finalPersistedState, now);
            });
            interiorInstanceService.releaseInteriorWorld(playerId);
            LOGGER.at(Level.INFO).log("Disconnect cleanup complete for %s.", playerId);
        } catch (Throwable throwable) {
            LOGGER.at(Level.SEVERE).withCause(throwable).log("Player disconnect cleanup failed for %s.", playerId);
        }
    }

    public CompletableFuture<PlayerSession> ensureSession(Player player) {
        PlayerSession existing = sessionStore.get(player.getUuid());
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }
        UUID playerId = player.getUuid();
        String displayName = player.getDisplayName();

        return resolveSpawnLocation(player)
                .thenCompose(spawnLocation -> AsyncTask.supplyAsync(() -> initializeSession(playerId, displayName, spawnLocation)))
                .thenCompose(session -> {
                    if (session == null) {
                        return CompletableFuture.completedFuture(null);
                    }
                    CompletableFuture<PlayerSession> readyFuture = new CompletableFuture<>();
                    if (player.getWorld() == null) {
                        sessionStore.put(session);
                        LOGGER.at(Level.WARNING).log(
                                "Session initialized for %s (%s) without a live world reference.",
                                displayName,
                                playerId
                        );
                        readyFuture.complete(session);
                        return readyFuture;
                    }
                    WorldTasks.executeSafe(player.getWorld(), "PlayerDataService.ensureSession", () -> {
                        try {
                            sessionStore.put(session);
                            castleSpawnService.ensureCastleSpawned(player, session.gameState().castleLocation());
                            resourceNodeVisualService.ensureNodes(playerId, session.gameState());
                            buildingVisualService.ensureBuildings(playerId, session.gameState());
                            clockService.applyToWorld(player.getWorld());
                            LOGGER.at(Level.INFO).log("Session initialized for %s (%s).", displayName, playerId);
                            readyFuture.complete(session);
                        } catch (Throwable throwable) {
                            readyFuture.completeExceptionally(throwable);
                        }
                    });
                    return readyFuture;
                });
    }

    private PlayerSession initializeSession(UUID playerId, String displayName, CastleLocationData spawnLocation) {
        Instant now = Instant.now();
        String timezone = clockService.snapshot().timezone();
        String ipHash = ipHashService.hash(null);
        PlayerProfile profile = profileService.loadOrCreate(playerId, displayName, timezone, ipHash, now);
        PlayerGameState state = gameStateService.loadOrCreate(profile.id(), playerId, spawnLocation, now);
        return new PlayerSession(playerId, profile, state);
    }

    private CompletableFuture<CastleLocationData> resolveSpawnLocation(Player player) {
        if (player == null || player.getWorld() == null) {
            return CompletableFuture.completedFuture(new CastleLocationData("default", 0.0D, 80.0D, 0.0D));
        }

        CompletableFuture<CastleLocationData> future = new CompletableFuture<>();
        WorldTasks.executeSafe(player.getWorld(), "PlayerDataService.resolveSpawnLocation", () -> {
            try {
                TransformComponent transform = player.getTransformComponent();
                Vector3d position = transform == null ? null : transform.getPosition();
                if (position == null) {
                    future.complete(new CastleLocationData(player.getWorld().getName(), 0.0D, 80.0D, 0.0D));
                    return;
                }

                double x = position.getX();
                double y = position.getY();
                double z = position.getZ();
                if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || y < 5.0D) {
                    future.complete(new CastleLocationData(player.getWorld().getName(), 0.0D, 80.0D, 0.0D));
                    return;
                }

                future.complete(new CastleLocationData(player.getWorld().getName(), x, y + 1.0D, z));
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

}
