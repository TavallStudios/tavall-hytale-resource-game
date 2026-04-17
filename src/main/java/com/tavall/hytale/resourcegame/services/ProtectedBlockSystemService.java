package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IProtectedBlockSystemService;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Installs protected-block ECS listeners after the base universe finishes booting.
 */
public final class ProtectedBlockSystemService implements IProtectedBlockSystemService, IDependencyInjectableConcrete {
    private static final Logger LOGGER = Logger.getLogger(ProtectedBlockSystemService.class.getName());
    private static final long STARTUP_DELAY_SECONDS = 20L;

    private final StructureProtectionService protectionService;
    private ScheduledFuture<?> startupTask;
    private boolean registered;

    public ProtectedBlockSystemService(StructureProtectionService protectionService) {
        this.protectionService = Objects.requireNonNull(protectionService, "protectionService");
    }

    @Override
    public synchronized void start() {
        if (registered || startupTask != null) {
            return;
        }
        startupTask = HytaleServer.SCHEDULED_EXECUTOR.schedule(
                this::registerSafely,
                STARTUP_DELAY_SECONDS,
                TimeUnit.SECONDS
        );
    }

    @Override
    public synchronized void shutdown() {
        if (startupTask != null) {
            startupTask.cancel(false);
            startupTask = null;
        }
        unregisterSafely();
    }

    private synchronized void registerSafely() {
        startupTask = null;
        if (registered) {
            return;
        }
        try {
            unregisterSafely();
            EntityStore.REGISTRY.registerSystem(new ProtectedBlockBreakSystem(protectionService));
            EntityStore.REGISTRY.registerSystem(new ProtectedBlockPlaceSystem(protectionService));
            registered = true;
            LOGGER.info("Protected block ECS systems registered.");
        } catch (Throwable throwable) {
            LOGGER.log(Level.WARNING, "Failed to register protected block ECS systems.", throwable);
        }
    }

    private void unregisterSafely() {
        try {
            EntityStore.REGISTRY.unregisterSystem(ProtectedBlockBreakSystem.class);
        } catch (Throwable ignored) {
        }
        try {
            EntityStore.REGISTRY.unregisterSystem(ProtectedBlockPlaceSystem.class);
        } catch (Throwable ignored) {
        }
        registered = false;
    }
}
