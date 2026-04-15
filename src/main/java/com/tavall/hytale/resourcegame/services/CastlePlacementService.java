package com.tavall.hytale.resourcegame.services;

import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastlePlacementService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleSiteVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleSpawnService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerGameStateService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerSessionStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeVisualService;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.tasks.AsyncTask;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Relocates a player's castle and refreshes all dependent world visuals.
 */
public final class CastlePlacementService implements ICastlePlacementService, IDependencyInjectableConcrete {
    private final IPlayerSessionStore sessionStore;
    private final IPlayerGameStateService gameStateService;
    private final ICastleSpawnService castleSpawnService;
    private final ICastleSiteVisualService castleSiteVisualService;
    private final IResourceNodeVisualService resourceNodeVisualService;

    public CastlePlacementService(
            IPlayerSessionStore sessionStore,
            IPlayerGameStateService gameStateService,
            ICastleSpawnService castleSpawnService,
            ICastleSiteVisualService castleSiteVisualService,
            IResourceNodeVisualService resourceNodeVisualService
    ) {
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.gameStateService = Objects.requireNonNull(gameStateService, "gameStateService");
        this.castleSpawnService = Objects.requireNonNull(castleSpawnService, "castleSpawnService");
        this.castleSiteVisualService = Objects.requireNonNull(castleSiteVisualService, "castleSiteVisualService");
        this.resourceNodeVisualService = Objects.requireNonNull(resourceNodeVisualService, "resourceNodeVisualService");
    }

    @Override
    public PlayerGameState placeCastle(UUID playerId, CastleLocationData castleLocation, Instant now) {
        PlayerSession session = sessionStore.get(playerId);
        if (session == null || castleLocation == null) {
            return null;
        }
        PlayerGameState updatedState = session.gameState().withCastleLocation(
                castleLocation,
                session.gameState().castleId() == null ? UUID.randomUUID() : session.gameState().castleId(),
                now
        );
        session.updateGameState(updatedState);
        gameStateService.cacheState(playerId, updatedState);
        castleSpawnService.replaceCastle(playerId, castleLocation);
        castleSiteVisualService.refreshSite(playerId, updatedState);
        resourceNodeVisualService.refreshNodes(playerId, updatedState);
        AsyncTask.runAsync(() -> gameStateService.persistState(updatedState, now));
        return updatedState;
    }
}
