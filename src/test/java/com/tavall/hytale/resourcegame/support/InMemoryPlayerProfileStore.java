package com.tavall.hytale.resourcegame.support;

import com.tavall.hytale.resourcegame.domain.PlayerProfile;
import com.tavall.hytale.resourcegame.persistence.PlayerProfileStore;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class InMemoryPlayerProfileStore implements PlayerProfileStore {
    private final AtomicLong idSequence;
    private final AtomicInteger findCalls;
    private final AtomicInteger upsertCalls;
    private final Map<UUID, PlayerProfile> values;

    public InMemoryPlayerProfileStore() {
        this.idSequence = new AtomicLong(1L);
        this.findCalls = new AtomicInteger();
        this.upsertCalls = new AtomicInteger();
        this.values = new ConcurrentHashMap<>();
    }

    @Override
    public Optional<PlayerProfile> findByUuid(UUID uuid) throws SQLException {
        findCalls.incrementAndGet();
        return Optional.ofNullable(values.get(uuid));
    }

    @Override
    public PlayerProfile upsert(UUID uuid, String name, String timezone, String ipHash, Instant now) throws SQLException {
        upsertCalls.incrementAndGet();
        PlayerProfile existing = values.get(uuid);
        PlayerProfile profile = new PlayerProfile(
                existing == null ? idSequence.getAndIncrement() : existing.id(),
                uuid,
                name,
                timezone,
                ipHash,
                existing == null ? now : existing.createdAt(),
                now,
                now
        );
        values.put(uuid, profile);
        return profile;
    }

    public int findCalls() {
        return findCalls.get();
    }

    public int upsertCalls() {
        return upsertCalls.get();
    }
}
