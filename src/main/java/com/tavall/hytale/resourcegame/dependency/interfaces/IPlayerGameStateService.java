package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IPlayerGameStateService extends IDependencyInjectableInterface {
    Optional<PlayerGameState> readCached(UUID playerId);

    PlayerGameState loadOrCreate(long profileId, UUID playerId, CastleLocationData spawnLocation, Instant now);

    PlayerGameState persistState(PlayerGameState state, Instant now);

    void cacheState(UUID playerId, PlayerGameState state);

    boolean isInteriorTutorialPending(PlayerGameState state);

    boolean isInteriorTourPending(PlayerGameState state);

    boolean isUpgradeTutorialPending(PlayerGameState state);

    PlayerGameState markInteriorTutorialSeen(PlayerGameState state, Instant now);

    PlayerGameState markInteriorTourSeen(PlayerGameState state, Instant now);

    PlayerGameState markUpgradeTutorialSeen(PlayerGameState state, Instant now);

    PlayerGameState resetOnboardingProgress(PlayerGameState state, Instant now);

    int interiorInstanceIndex(PlayerGameState state);

    PlayerGameState bumpInteriorInstanceIndex(PlayerGameState state, Instant now);
}
