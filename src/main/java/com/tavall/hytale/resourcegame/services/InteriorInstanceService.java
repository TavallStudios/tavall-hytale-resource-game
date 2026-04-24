package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.storage.provider.EmptyChunkStorageProvider;
import com.hypixel.hytale.server.core.universe.world.storage.resources.EmptyResourceStorageProvider;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.VoidWorldGenProvider;
import com.hypixel.hytale.server.core.util.io.FileUtil;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInteriorInstanceService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves dedicated same-process interior worlds for players.
 */
public final class InteriorInstanceService implements IInteriorInstanceService, IDependencyInjectableConcrete {
    private static final Logger LOGGER = Logger.getLogger(InteriorInstanceService.class.getName());
    private static final String SHARED_WORLD_NAME = "kingdom-interiors";
    private static final String LEGACY_INTERIOR_WORLD_NAME_PREFIX = "kingdom-interior-";
    private static final long WORLD_READY_RETRY_MILLIS = 50L;
    private static final int WORLD_READY_MAX_RETRIES = 100;

    private final Object worldCreationLock = new Object();
    private final Map<String, CompletableFuture<World>> worldFutures = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<World> resolveInteriorWorld(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        String worldName = worldNameFor(playerId);
        World existing = Universe.get().getWorld(worldName);
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }

        CompletableFuture<World> pendingFuture = worldFutures.get(worldName);
        if (pendingFuture != null) {
            return pendingFuture;
        }

        synchronized (worldCreationLock) {
            existing = Universe.get().getWorld(worldName);
            if (existing != null) {
                return CompletableFuture.completedFuture(existing);
            }
            pendingFuture = worldFutures.get(worldName);
            if (pendingFuture != null) {
                return pendingFuture;
            }
            Path worldPath = Universe.get().validateWorldPath(worldName);
            deleteWorldDirectoryIfPresent(worldPath);
            CompletableFuture<World> createdFuture = new CompletableFuture<>();
            worldFutures.put(worldName, createdFuture);
            Universe.get().makeWorld(worldName, worldPath, createWorldConfig())
                    .whenComplete((world, throwable) -> {
                        if (throwable != null) {
                            LOGGER.log(Level.WARNING, "Failed to warm interior world " + worldName + '.', throwable);
                            worldFutures.remove(worldName, createdFuture);
                            createdFuture.completeExceptionally(throwable);
                            return;
                        }
                        awaitWorldReady(worldName, world, createdFuture, WORLD_READY_MAX_RETRIES);
                    });
            return createdFuture;
        }
    }

    @Override
    public CompletableFuture<World> warmInteriorWorld() {
        return resolveInteriorWorld(new UUID(0L, 1L));
    }

    @Override
    public String worldNameFor(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return SHARED_WORLD_NAME;
    }

    @Override
    public void releaseInteriorWorld(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        LOGGER.fine(() -> "Keeping shared interior world active for " + playerId + '.');
    }

    @Override
    public void pruneTransientWorlds() {
        Universe.get().getWorlds().keySet().stream()
                .filter(this::isTransientWorldName)
                .toList()
                .forEach(this::deleteWorldByName);
        deleteLegacyInteriorDirectories();
    }

    private WorldConfig createWorldConfig() {
        WorldConfig worldConfig = new WorldConfig();
        worldConfig.setDisplayName("Kingdom Interior");
        worldConfig.setWorldGenProvider(new VoidWorldGenProvider());
        worldConfig.setChunkStorageProvider(new EmptyChunkStorageProvider());
        worldConfig.setResourceStorageProvider(new EmptyResourceStorageProvider());
        // Allow spawned NPC anchors; they are frozen/intangible anyway.
        worldConfig.setSpawningNPC(true);
        worldConfig.setIsSpawnMarkersEnabled(false);
        worldConfig.setIsAllNPCFrozen(true);
        worldConfig.setSavingPlayers(false);
        worldConfig.setSavingConfig(false);
        worldConfig.setCanSaveChunks(false);
        worldConfig.setDeleteOnRemove(true);
        worldConfig.setDeleteOnUniverseStart(true);
        return worldConfig;
    }

    private void awaitWorldReady(String worldName, World world, CompletableFuture<World> readyFuture, int retriesRemaining) {
        if (readyFuture == null || readyFuture.isDone()) {
            return;
        }
        if (world == null) {
            IllegalStateException exception = new IllegalStateException("missing interior world");
            worldFutures.remove(worldName, readyFuture);
            readyFuture.completeExceptionally(exception);
            return;
        }
        if (!isWorldUsable(world)) {
            retryWorldReady(worldName, world, readyFuture, retriesRemaining, null);
            return;
        }
        try {
            world.execute(() -> {
                if (readyFuture.isDone()) {
                    return;
                }
                readyFuture.complete(world);
                LOGGER.info(() -> "Kingdom interior world is ready: " + world.getName());
            });
        } catch (Throwable throwable) {
            retryWorldReady(worldName, world, readyFuture, retriesRemaining, throwable);
        }
    }

    private void retryWorldReady(
            String worldName,
            World world,
            CompletableFuture<World> readyFuture,
            int retriesRemaining,
            Throwable lastFailure
    ) {
        if (readyFuture == null || readyFuture.isDone()) {
            return;
        }
        if (retriesRemaining <= 0) {
            IllegalStateException exception = new IllegalStateException(
                    "interior world did not become ready: " + worldName,
                    lastFailure
            );
            worldFutures.remove(worldName, readyFuture);
            readyFuture.completeExceptionally(exception);
            return;
        }
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> awaitWorldReady(worldName, world, readyFuture, retriesRemaining - 1),
                WORLD_READY_RETRY_MILLIS,
                TimeUnit.MILLISECONDS
        );
    }

    private boolean isWorldUsable(World world) {
        return world != null
                && world.isAlive()
                && Universe.get().getWorld(world.getName()) == world;
    }

    private void deleteWorldByName(String worldName) {
        World existing = Universe.get().getWorld(worldName);
        if (existing != null) {
            Universe.get().removeWorld(worldName);
        }
        deleteWorldDirectoryIfPresent(Universe.get().validateWorldPath(worldName));
    }

    private void deleteLegacyInteriorDirectories() {
        Path worldsPath = Universe.get().getWorldsPath();
        if (!Files.isDirectory(worldsPath)) {
            return;
        }
        try (var stream = Files.list(worldsPath)) {
            stream.filter(Files::isDirectory)
                    .filter(path -> isTransientWorldName(path.getFileName().toString()))
                    .forEach(this::deleteWorldDirectoryIfPresent);
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Failed to prune orphaned interior directories.", exception);
        }
    }

    private boolean isTransientWorldName(String worldName) {
        return worldName != null && worldName.startsWith(LEGACY_INTERIOR_WORLD_NAME_PREFIX);
    }

    private void deleteWorldDirectoryIfPresent(Path worldPath) {
        if (worldPath == null || !Files.exists(worldPath)) {
            return;
        }
        try {
            FileUtil.deleteDirectory(worldPath);
        } catch (Throwable throwable) {
            LOGGER.log(Level.WARNING, "Failed to delete transient interior directory " + worldPath + '.', throwable);
        }
    }
}
