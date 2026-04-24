package com.tavall.hytale.resourcegame.domain;

import java.time.Instant;

/**
 * Represents a short-lived interaction override set by explicit selection commands.
 */
public final class FocusedWorldOverride {
    private final FocusedWorldTarget target;
    private final Instant expiresAt;

    public FocusedWorldOverride(FocusedWorldTarget target, Instant expiresAt) {
        this.target = target;
        this.expiresAt = expiresAt;
    }

    public FocusedWorldTarget target() {
        return target;
    }

    public Instant expiresAt() {
        return expiresAt;
    }
}
