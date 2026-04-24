package com.tavall.hytale.resourcegame.domain;

import java.time.Instant;

/**
 * Snapshot of the kingdom clock for day/night logic.
 */
public final class KingdomClockState {
    private final Instant serverTime;
    private final boolean isDay;
    private final String timezone;

    public KingdomClockState(Instant serverTime, boolean isDay, String timezone) {
        this.serverTime = serverTime;
        this.isDay = isDay;
        this.timezone = timezone;
    }

    public Instant serverTime() {
        return serverTime;
    }

    public boolean isDay() {
        return isDay;
    }

    public String timezone() {
        return timezone;
    }
}
