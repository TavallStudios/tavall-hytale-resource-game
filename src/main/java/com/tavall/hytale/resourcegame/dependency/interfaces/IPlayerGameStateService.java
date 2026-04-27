package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;
import com.tavall.hytale.resourcegame.domain.AccountProgression;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.DebugModeState;
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

    AccountProgression accountProgression(PlayerGameState state);

    DebugModeState debugModeState(PlayerGameState state);

    PlayerGameState setAccountLevel(PlayerGameState state, int level, Instant now);

    PlayerGameState addAccountExperience(PlayerGameState state, int experience, Instant now);

    PlayerGameState setDebugMode(PlayerGameState state, DebugModeState debugModeState, Instant now);
}
