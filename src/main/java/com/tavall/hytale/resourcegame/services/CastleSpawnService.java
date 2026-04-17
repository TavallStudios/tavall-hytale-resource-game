package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.config.CastleAssetConfig;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleSiteVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleSpawnService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerSessionStore;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;

import java.util.Objects;
import java.util.UUID;

/**
 * Delegates castle surface refreshes to the block-first castle-site visual service.
 */
public final class CastleSpawnService implements ICastleSpawnService, IDependencyInjectableConcrete {
    private final CastleAssetConfig assetConfig;
    private final IPlayerSessionStore sessionStore;
    private final ICastleSiteVisualService castleSiteVisualService;

    public CastleSpawnService(
            CastleAssetConfig assetConfig,
            IPlayerSessionStore sessionStore,
            ICastleSiteVisualService castleSiteVisualService
    ) {
        this.assetConfig = Objects.requireNonNull(assetConfig, "assetConfig");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.castleSiteVisualService = Objects.requireNonNull(castleSiteVisualService, "castleSiteVisualService");
    }

    @Override
    public void ensureCastleSpawned(Player player, CastleLocationData locationData) {
        if (player == null || locationData == null) {
            return;
        }
        PlayerSession session = sessionStore.get(player.getUuid());
        if (session == null) {
            return;
        }
        castleSiteVisualService.ensureSite(player.getUuid(), ensureAssetType(session));
    }

    @Override
    public void replaceCastle(UUID playerId, CastleLocationData locationData) {
        if (playerId == null || locationData == null) {
            return;
        }
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return;
        }
        castleSiteVisualService.refreshSite(playerId, ensureAssetType(session));
    }

    private PlayerGameState ensureAssetType(PlayerSession session) {
        if (session.gameState().castleAssetType() != null && !session.gameState().castleAssetType().isBlank()) {
            return session.gameState();
        }
        PlayerGameState updatedState = session.gameState().withCastleAssetType(assetConfig.structureAssetType(), java.time.Instant.now());
        session.updateGameState(updatedState);
        return updatedState;
    }
}
