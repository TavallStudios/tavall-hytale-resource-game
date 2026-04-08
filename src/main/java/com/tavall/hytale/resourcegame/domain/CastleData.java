package com.tavall.hytale.resourcegame.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Stable identifier and ownership of a castle.
 */
public final class CastleData {
    private final UUID castleId;
    private final UUID ownerId;

    public CastleData(UUID castleId, UUID ownerId) {
        this.castleId = Objects.requireNonNull(castleId, "castleId");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
    }

    public UUID castleId() {
        return castleId;
    }

    public UUID ownerId() {
        return ownerId;
    }
}
