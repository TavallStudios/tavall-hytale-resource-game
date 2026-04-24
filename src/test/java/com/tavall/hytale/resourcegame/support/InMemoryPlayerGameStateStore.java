package com.tavall.hytale.resourcegame.support;

import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.persistence.PlayerGameStateStore;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class InMemoryPlayerGameStateStore implements PlayerGameStateStore {
    private final AtomicLong idSequence;
    private final AtomicInteger findCalls;
    private final AtomicInteger upsertCalls;
    private final Map<Long, PlayerGameState> values;

    public InMemoryPlayerGameStateStore() {
        this.idSequence = new AtomicLong(1L);
        this.findCalls = new AtomicInteger();
        this.upsertCalls = new AtomicInteger();
        this.values = new ConcurrentHashMap<>();
    }

    @Override
    public Optional<PlayerGameState> findByProfileId(long profileId) throws SQLException {
        findCalls.incrementAndGet();
        return Optional.ofNullable(values.get(profileId));
    }

    @Override
    public PlayerGameState upsert(PlayerGameState state, Instant now) throws SQLException {
        upsertCalls.incrementAndGet();
        PlayerGameState existing = values.get(state.profileId());
        PlayerGameState persisted = new PlayerGameState(
                existing == null ? idSequence.getAndIncrement() : existing.id(),
                state.profileId(),
                state.castleId(),
                state.castleAssetType(),
                state.castleLocation(),
                state.populationSummary(),
                state.resources(),
                state.interiorSession(),
                state.metadataJson(),
                existing == null ? now : existing.createdAt(),
                now
        );
        values.put(state.profileId(), persisted);
        return persisted;
    }

    public Optional<PlayerGameState> snapshot(long profileId) {
        return Optional.ofNullable(values.get(profileId));
    }

    public int findCalls() {
        return findCalls.get();
    }

    public int upsertCalls() {
        return upsertCalls.get();
    }
}
