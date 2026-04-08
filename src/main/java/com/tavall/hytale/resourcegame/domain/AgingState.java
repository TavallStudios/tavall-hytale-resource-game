package com.tavall.hytale.resourcegame.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Tracks real-time aging state for the population.
 */
public final class AgingState {
    private final Instant lastTickAt;
    private final Duration totalAge;

    public AgingState(Instant lastTickAt, Duration totalAge) {
        this.lastTickAt = Objects.requireNonNull(lastTickAt, "lastTickAt");
        this.totalAge = Objects.requireNonNull(totalAge, "totalAge");
    }

    public Instant lastTickAt() {
        return lastTickAt;
    }

    public Duration totalAge() {
        return totalAge;
    }

    public AgingState tick(Instant now) {
        Duration delta = Duration.between(lastTickAt, now);
        if (delta.isNegative()) {
            return this;
        }
        return new AgingState(now, totalAge.plus(delta));
    }

    public static AgingState defaults(Instant now) {
        return new AgingState(now, Duration.ZERO);
    }
}
