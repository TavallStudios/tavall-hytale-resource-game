package com.tavall.hytale.resourcegame.services;

import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IFocusedWorldOverrideService;
import com.tavall.hytale.resourcegame.domain.FocusedWorldOverride;
import com.tavall.hytale.resourcegame.domain.FocusedWorldTarget;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores short-lived selection overrides so explicit align flows can drive the next interaction.
 */
public final class FocusedWorldOverrideService implements IFocusedWorldOverrideService, IDependencyInjectableConcrete {
    private static final Duration OVERRIDE_TTL = Duration.ofSeconds(12L);

    private final Map<UUID, FocusedWorldOverride> overrides = new ConcurrentHashMap<>();

    @Override
    public void markCastle(UUID playerId) {
        put(playerId, FocusedWorldTarget.castle(0.0D, 1.0D));
    }

    @Override
    public void markNode(UUID playerId, UUID nodeId) {
        Objects.requireNonNull(nodeId, "nodeId");
        put(playerId, FocusedWorldTarget.resourceNode(nodeId, "selected node", 0.0D, 1.0D));
    }

    @Override
    public void markBuilding(UUID playerId, UUID buildingId) {
        Objects.requireNonNull(buildingId, "buildingId");
        put(playerId, FocusedWorldTarget.building(buildingId, "selected building", 0.0D, 1.0D));
    }

    @Override
    public Optional<FocusedWorldTarget> peek(UUID playerId) {
        return resolve(playerId, false);
    }

    @Override
    public Optional<FocusedWorldTarget> consume(UUID playerId) {
        return resolve(playerId, true);
    }

    @Override
    public void clear(UUID playerId) {
        if (playerId != null) {
            overrides.remove(playerId);
        }
    }

    private void put(UUID playerId, FocusedWorldTarget target) {
        overrides.put(
                Objects.requireNonNull(playerId, "playerId"),
                new FocusedWorldOverride(Objects.requireNonNull(target, "target"), Instant.now().plus(OVERRIDE_TTL))
        );
    }

    private Optional<FocusedWorldTarget> resolve(UUID playerId, boolean consume) {
        if (playerId == null) {
            return Optional.empty();
        }
        FocusedWorldOverride override = overrides.get(playerId);
        if (override == null) {
            return Optional.empty();
        }
        if (override.expiresAt() == null || Instant.now().isAfter(override.expiresAt())) {
            overrides.remove(playerId);
            return Optional.empty();
        }
        if (consume) {
            overrides.remove(playerId);
        }
        return Optional.of(override.target());
    }
}
