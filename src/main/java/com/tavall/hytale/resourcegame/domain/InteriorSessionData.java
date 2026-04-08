package com.tavall.hytale.resourcegame.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Tracks a player's active interior session and return location.
 */
public final class InteriorSessionData {
    private final String interiorWorldName;
    private final CastleLocationData returnLocation;
    private final Instant enteredAt;

    public InteriorSessionData(String interiorWorldName, CastleLocationData returnLocation, Instant enteredAt) {
        this.interiorWorldName = Objects.requireNonNull(interiorWorldName, "interiorWorldName");
        this.returnLocation = Objects.requireNonNull(returnLocation, "returnLocation");
        this.enteredAt = Objects.requireNonNull(enteredAt, "enteredAt");
    }

    public String interiorWorldName() {
        return interiorWorldName;
    }

    public CastleLocationData returnLocation() {
        return returnLocation;
    }

    public Instant enteredAt() {
        return enteredAt;
    }
}
