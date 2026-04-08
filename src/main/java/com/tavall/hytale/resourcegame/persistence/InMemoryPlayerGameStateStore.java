package com.tavall.hytale.resourcegame.persistence;

import com.tavall.hytale.resourcegame.domain.PlayerGameState;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Process-local fallback game-state store used when Postgres is unavailable.
 */
public final class InMemoryPlayerGameStateStore implements PlayerGameStateStore {
    private final AtomicLong idSequence;
    private final Map<Long, PlayerGameState> values;

    public InMemoryPlayerGameStateStore() {
        this.idSequence = new AtomicLong(1L);
        this.values = new ConcurrentHashMap<>();
    }

    @Override
    public Optional<PlayerGameState> findByProfileId(long profileId) throws SQLException {
        return Optional.ofNullable(values.get(profileId));
    }

    @Override
    public PlayerGameState upsert(PlayerGameState state, Instant now) throws SQLException {
        PlayerGameState existing = values.get(state.profileId());
        PlayerGameState persisted = new PlayerGameState(
                existing == null ? idSequence.getAndIncrement() : existing.id(),
                state.profileId(),
                state.castleId(),
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
}
