package com.tavall.hytale.resourcegame.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Core player identity record used for persistence and caching.
 */
public final class PlayerProfile {
    private final long id;
    private final UUID uuid;
    private final String name;
    private final String timezone;
    private final String ipHash;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant lastSeenAt;

    public PlayerProfile(
            long id,
            UUID uuid,
            String name,
            String timezone,
            String ipHash,
            Instant createdAt,
            Instant updatedAt,
            Instant lastSeenAt
    ) {
        this.id = id;
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.name = Objects.requireNonNull(name, "name");
        this.timezone = timezone;
        this.ipHash = ipHash;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastSeenAt = lastSeenAt;
    }

    public long id() {
        return id;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public String timezone() {
        return timezone;
    }

    public String ipHash() {
        return ipHash;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Instant lastSeenAt() {
        return lastSeenAt;
    }

    public PlayerProfile withLastSeen(Instant lastSeenAt, Instant updatedAt) {
        return new PlayerProfile(id, uuid, name, timezone, ipHash, createdAt, updatedAt, lastSeenAt);
    }
}
