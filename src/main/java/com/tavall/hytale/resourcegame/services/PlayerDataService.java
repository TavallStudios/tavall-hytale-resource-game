package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.tavall.hytale.resourcegame.clock.KingdomClockService;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.PlayerProfile;
import com.tavall.hytale.resourcegame.tasks.AsyncTask;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Orchestrates player initialization and persistence.
 */
public final class PlayerDataService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final double INITIAL_CASTLE_Z_OFFSET = 4.0;

    private final PlayerProfileService profileService;
    private final PlayerGameStateService gameStateService;
    private final PlayerSessionStore sessionStore;
    private final CastleSpawnService castleSpawnService;
    private final IpHashService ipHashService;
    private final KingdomClockService clockService;

    public PlayerDataService(
            PlayerProfileService profileService,
            PlayerGameStateService gameStateService,
            PlayerSessionStore sessionStore,
            CastleSpawnService castleSpawnService,
            IpHashService ipHashService,
            KingdomClockService clockService
    ) {
        this.profileService = Objects.requireNonNull(profileService, "profileService");
        this.gameStateService = Objects.requireNonNull(gameStateService, "gameStateService");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.castleSpawnService = Objects.requireNonNull(castleSpawnService, "castleSpawnService");
        this.ipHashService = Objects.requireNonNull(ipHashService, "ipHashService");
        this.clockService = Objects.requireNonNull(clockService, "clockService");
    }

    public void handlePlayerReady(PlayerReadyEvent event) {
        ensureSession(event.getPlayer());
    }

    public void handlePlayerDisconnect(PlayerDisconnectEvent event) {
        UUID playerId = event.getPlayerRef().getUuid();
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return;
        }
        PlayerProfile profile = session.profile();
        PlayerGameState state = session.gameState();
        AsyncTask.runAsync(() -> {
            Instant now = Instant.now();
            profileService.persist(profile, now);
            gameStateService.persistState(state, now);
        });
        sessionStore.remove(playerId);
    }

    public CompletableFuture<PlayerSession> ensureSession(Player player) {
        PlayerSession existing = sessionStore.get(player.getUuid());
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }

        CastleLocationData spawnLocation = resolveSpawnLocation(player);
        return AsyncTask.supplyAsync(() -> initializeSession(player, spawnLocation))
                .thenApply(session -> {
                    if (session == null) {
                        return null;
                    }
                    player.getWorld().execute(() -> {
                        sessionStore.put(session);
                        castleSpawnService.ensureCastleSpawned(player, session.gameState().castleLocation());
                        clockService.applyToWorld(player.getWorld());
                    });
                    LOGGER.at(Level.INFO).log("Session initialized for %s (%s).", player.getDisplayName(), player.getUuid());
                    return session;
                });
    }

    private PlayerSession initializeSession(Player player, CastleLocationData spawnLocation) {
        UUID playerId = player.getUuid();
        Instant now = Instant.now();
        String timezone = clockService.snapshot().timezone();
        String ipHash = ipHashService.hash(null);
        PlayerProfile profile = profileService.loadOrCreate(playerId, player.getDisplayName(), timezone, ipHash, now);
        PlayerGameState state = gameStateService.loadOrCreate(profile.id(), playerId, spawnLocation, now);
        return new PlayerSession(playerId, profile, state);
    }

    private CastleLocationData resolveSpawnLocation(Player player) {
        Vector3d spawnPosition = player.getTransformComponent().getPosition();
        return new CastleLocationData(
                player.getWorld().getName(),
                spawnPosition.getX(),
                spawnPosition.getY(),
                spawnPosition.getZ() + INITIAL_CASTLE_Z_OFFSET
        );
    }
}
