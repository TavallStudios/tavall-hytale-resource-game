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
    private static final String WORLD_NAME_PREFIX = "kingdom-interior-";

    @Override
    public CompletableFuture<World> resolveInteriorWorld(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        String worldName = worldNameFor(playerId);
        World existing = Universe.get().getWorld(worldName);
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }

        Path worldPath = Universe.get().validateWorldPath(worldName);
        deleteWorldDirectoryIfPresent(worldPath);
        return Universe.get().makeWorld(worldName, worldPath, createWorldConfig());
    }

    @Override
    public String worldNameFor(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return WORLD_NAME_PREFIX + playerId.toString().replace("-", "");
    }

    @Override
    public void releaseInteriorWorld(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        releaseInteriorWorldByName(worldNameFor(playerId));
    }

    @Override
    public void pruneTransientWorlds() {
        Universe.get().getWorlds().keySet().stream()
                .filter(this::isTransientWorldName)
                .toList()
                .forEach(this::releaseInteriorWorldByName);
        deleteOrphanedInteriorDirectories();
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

    private void releaseInteriorWorldByName(String worldName) {
        World existing = Universe.get().getWorld(worldName);
        if (existing != null) {
            Universe.get().removeWorld(worldName);
        }
        deleteWorldDirectoryIfPresent(Universe.get().validateWorldPath(worldName));
    }

    private void deleteOrphanedInteriorDirectories() {
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
        return worldName != null && worldName.startsWith(WORLD_NAME_PREFIX);
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
