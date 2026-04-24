package com.tavall.hytale.resourcegame.support;

import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerProfileService;
import com.tavall.hytale.resourcegame.domain.PlayerProfile;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public final class RecordingPlayerProfileService implements IPlayerProfileService {
    private final AtomicReference<PlayerProfile> persistedProfile = new AtomicReference<>();

    @Override
    public Optional<PlayerProfile> readCached(UUID playerId) {
        return Optional.empty();
    }

    @Override
    public PlayerProfile loadOrCreate(UUID playerId, String name, String timezone, String ipHash, Instant now) {
        return new PlayerProfile(
                1L,
                playerId,
                name == null ? "player" : name,
                timezone,
                ipHash,
                now,
                now,
                now
        );
    }

    @Override
    public void persist(PlayerProfile profile, Instant now) {
        persistedProfile.set(profile);
    }

    public PlayerProfile persistedProfile() {
        return persistedProfile.get();
    }
}

