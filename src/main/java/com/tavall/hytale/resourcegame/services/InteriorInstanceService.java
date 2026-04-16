package com.tavall.hytale.resourcegame.services;

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
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves dedicated same-process interior worlds for players.
 */
public final class InteriorInstanceService implements IInteriorInstanceService, IDependencyInjectableConcrete {
    private static final Logger LOGGER = Logger.getLogger(InteriorInstanceService.class.getName());
    private static final String SHARED_WORLD_NAME = "kingdom-interiors";
    private static final String LEGACY_WORLD_NAME_PREFIX = "kingdom-interior-";

    private final Object worldCreationLock = new Object();
    private volatile CompletableFuture<World> sharedWorldFuture;

    @Override
    public CompletableFuture<World> resolveInteriorWorld(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return warmInteriorWorld();
    }

    @Override
    public CompletableFuture<World> warmInteriorWorld() {
        World existing = Universe.get().getWorld(SHARED_WORLD_NAME);
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }

        CompletableFuture<World> pendingFuture = sharedWorldFuture;
        if (pendingFuture != null) {
            return pendingFuture;
        }

        synchronized (worldCreationLock) {
            existing = Universe.get().getWorld(SHARED_WORLD_NAME);
            if (existing != null) {
                return CompletableFuture.completedFuture(existing);
            }
            pendingFuture = sharedWorldFuture;
            if (pendingFuture != null) {
                return pendingFuture;
            }
            Path worldPath = Universe.get().validateWorldPath(SHARED_WORLD_NAME);
            deleteWorldDirectoryIfPresent(worldPath);
            sharedWorldFuture = Universe.get().makeWorld(SHARED_WORLD_NAME, worldPath, createWorldConfig())
                    .whenComplete((world, throwable) -> {
                        if (throwable != null) {
                            LOGGER.log(Level.WARNING, "Failed to warm shared interior world.", throwable);
                            synchronized (worldCreationLock) {
                                sharedWorldFuture = null;
                            }
                            return;
                        }
                        LOGGER.info(() -> "Shared kingdom interior world is ready: " + world.getName());
                    });
            return sharedWorldFuture;
        }
    }

    @Override
    public String worldNameFor(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return SHARED_WORLD_NAME;
    }

    @Override
    public void releaseInteriorWorld(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        World sharedWorld = Universe.get().getWorld(SHARED_WORLD_NAME);
        if (sharedWorld == null) {
            sharedWorldFuture = null;
        }
    }

    @Override
    public void pruneTransientWorlds() {
        Universe.get().getWorlds().keySet().stream()
                .filter(this::isLegacyTransientWorldName)
                .toList()
                .forEach(this::deleteLegacyWorldByName);
        deleteLegacyInteriorDirectories();
    }

    private WorldConfig createWorldConfig() {
        WorldConfig worldConfig = new WorldConfig();
        worldConfig.setDisplayName("Kingdom Interior");
        worldConfig.setWorldGenProvider(new VoidWorldGenProvider());
        worldConfig.setChunkStorageProvider(new EmptyChunkStorageProvider());
        worldConfig.setResourceStorageProvider(new EmptyResourceStorageProvider());
        worldConfig.setSpawningNPC(false);
        worldConfig.setSavingPlayers(false);
        worldConfig.setSavingConfig(false);
        worldConfig.setCanSaveChunks(false);
        worldConfig.setDeleteOnRemove(true);
        worldConfig.setDeleteOnUniverseStart(true);
        return worldConfig;
    }

    private void deleteLegacyWorldByName(String worldName) {
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
                    .filter(path -> isLegacyTransientWorldName(path.getFileName().toString()))
                    .forEach(this::deleteWorldDirectoryIfPresent);
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Failed to prune orphaned interior directories.", exception);
        }
    }

    private boolean isLegacyTransientWorldName(String worldName) {
        return worldName != null && worldName.startsWith(LEGACY_WORLD_NAME_PREFIX);
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
