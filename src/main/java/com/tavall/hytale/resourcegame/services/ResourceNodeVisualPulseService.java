package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.server.core.HytaleServer;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerSessionStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeVisualPulseService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeVisualService;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Refreshes node route visuals frequently enough to show convoy movement.
 */
public final class ResourceNodeVisualPulseService implements IResourceNodeVisualPulseService, IDependencyInjectableConcrete {
    private static final long PULSE_INTERVAL_SECONDS = 3L;

    private final IPlayerSessionStore sessionStore;
    private final IResourceNodeVisualService resourceNodeVisualService;
    private ScheduledFuture<?> pulseTask;

    public ResourceNodeVisualPulseService(
            IPlayerSessionStore sessionStore,
            IResourceNodeVisualService resourceNodeVisualService
    ) {
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.resourceNodeVisualService = Objects.requireNonNull(resourceNodeVisualService, "resourceNodeVisualService");
    }

    @Override
    public void start() {
        if (pulseTask != null && !pulseTask.isCancelled()) {
            return;
        }
        pulseTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                this::runPulse,
                PULSE_INTERVAL_SECONDS,
                PULSE_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    @Override
    public void shutdown() {
        if (pulseTask != null) {
            pulseTask.cancel(false);
            pulseTask = null;
        }
    }

    public void runPulse() {
        for (PlayerSession session : sessionStore.snapshot()) {
            if (session.gameState() == null) {
                continue;
            }
            resourceNodeVisualService.refreshNodes(session.playerId(), session.gameState());
        }
    }
}
