package com.tavall.hytale.resourcegame.clock;

import com.tavall.hytale.resourcegame.config.KingdomClockConfig;
import com.tavall.hytale.resourcegame.domain.KingdomClockState;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Kingdom clock service using real-world time.
 */
public final class KingdomClockService {
    private final KingdomClockConfig config;

    public KingdomClockService(KingdomClockConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public KingdomClockState snapshot() {
        Instant now = Instant.now();
        ZoneId zoneId = ZoneId.of(config.timezone());
        ZonedDateTime zoned = ZonedDateTime.ofInstant(now, zoneId);
        int hour = zoned.getHour();
        boolean isDay = hour >= config.dayStartHour() && hour < config.dayEndHour();
        return new KingdomClockState(now, isDay, config.timezone());
    }

    public void applyToWorld(World world) {
        KingdomClockState state = snapshot();
        Instant gameTime = state.isDay()
                ? WorldTimeResource.ZERO_YEAR.plusSeconds(12 * 60 * 60)
                : WorldTimeResource.ZERO_YEAR.plusSeconds(22 * 60 * 60);
        world.getWorldConfig().setGameTimePaused(true);
        world.getWorldConfig().setGameTime(gameTime);
    }
}
