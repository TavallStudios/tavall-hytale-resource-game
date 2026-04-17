package com.tavall.hytale.resourcegame.clock;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.tavall.hytale.resourcegame.config.KingdomClockConfig;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IKingdomClockService;
import com.tavall.hytale.resourcegame.domain.KingdomClockState;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Kingdom clock service using real-world time.
 */
public final class KingdomClockService implements IKingdomClockService, IDependencyInjectableConcrete {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final long REFRESH_INTERVAL_SECONDS = 60L;
    private static final long INITIAL_REFRESH_DELAY_SECONDS = 10L;

    private final KingdomClockConfig config;
    private volatile ScheduledFuture<?> refreshTask;

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
        if (world == null) {
            return;
        }
        world.execute(() -> applyToWorldConfig(world));
    }

    private void applyToWorldConfig(World world) {
        KingdomClockState state = snapshot();
        Instant gameTime = state.isDay()
                ? WorldTimeResource.ZERO_YEAR.plusSeconds(12 * 60 * 60)
                : WorldTimeResource.ZERO_YEAR.plusSeconds(22 * 60 * 60);
        world.getWorldConfig().setGameTimePaused(true);
        world.getWorldConfig().setGameTime(gameTime);
        world.getWorldConfig().markChanged();
    }

    @Override
    public void applyToAllWorlds() {
        Universe.get().getWorlds().values().forEach(this::applyToWorld);
    }

    @Override
    public synchronized void start() {
        if (refreshTask != null && !refreshTask.isCancelled()) {
            return;
        }
        refreshTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                this::safeApplyToAllWorlds,
                INITIAL_REFRESH_DELAY_SECONDS,
                REFRESH_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    @Override
    public synchronized void shutdown() {
        if (refreshTask == null) {
            return;
        }
        refreshTask.cancel(false);
        refreshTask = null;
    }

    private void safeApplyToAllWorlds() {
        try {
            applyToAllWorlds();
        } catch (Throwable throwable) {
            LOGGER.at(Level.WARNING).withCause(throwable).log("Failed to refresh kingdom clock across worlds.");
        }
    }
}
