package com.tavall.hytale.resourcegame.support;

import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerGameStateService;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public final class RecordingPlayerGameStateService implements IPlayerGameStateService {
    private final AtomicReference<PlayerGameState> persistedState = new AtomicReference<>();

    @Override
    public Optional<PlayerGameState> readCached(UUID playerId) {
        return Optional.empty();
    }

    @Override
    public PlayerGameState loadOrCreate(long profileId, UUID playerId, CastleLocationData spawnLocation, Instant now) {
        throw new UnsupportedOperationException("Not used in this test double.");
    }

    @Override
    public PlayerGameState persistState(PlayerGameState state, Instant now) {
        persistedState.set(state);
        return state;
    }

    @Override
    public void cacheState(UUID playerId, PlayerGameState state) {
    }

    @Override
    public boolean isInteriorTutorialPending(PlayerGameState state) {
        return false;
    }

    @Override
    public boolean isInteriorTourPending(PlayerGameState state) {
        return false;
    }

    @Override
    public boolean isUpgradeTutorialPending(PlayerGameState state) {
        return false;
    }

    @Override
    public PlayerGameState markInteriorTutorialSeen(PlayerGameState state, Instant now) {
        return state;
    }

    @Override
    public PlayerGameState markInteriorTourSeen(PlayerGameState state, Instant now) {
        return state;
    }

    @Override
    public PlayerGameState markUpgradeTutorialSeen(PlayerGameState state, Instant now) {
        return state;
    }

    @Override
    public PlayerGameState resetOnboardingProgress(PlayerGameState state, Instant now) {
        return state;
    }

    @Override
    public int interiorInstanceIndex(PlayerGameState state) {
        return 0;
    }

    @Override
    public PlayerGameState bumpInteriorInstanceIndex(PlayerGameState state, Instant now) {
        return state;
    }

    public PlayerGameState persistedState() {
        return persistedState.get();
    }
}

