package com.tavall.hytale.resourcegame.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Context for castle interactions.
 */
public final class CastleInteractionContext {
    private final UUID playerId;
    private final UUID castleId;
    private final boolean withinRange;
    private final boolean lookingAtCastle;

    public CastleInteractionContext(UUID playerId, UUID castleId, boolean withinRange, boolean lookingAtCastle) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.castleId = Objects.requireNonNull(castleId, "castleId");
        this.withinRange = withinRange;
        this.lookingAtCastle = lookingAtCastle;
    }

    public UUID playerId() {
        return playerId;
    }

    public UUID castleId() {
        return castleId;
    }

    public boolean withinRange() {
        return withinRange;
    }

    public boolean lookingAtCastle() {
        return lookingAtCastle;
    }
}
