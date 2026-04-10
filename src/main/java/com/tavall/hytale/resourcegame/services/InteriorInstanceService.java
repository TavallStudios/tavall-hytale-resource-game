package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInteriorInstanceService;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Resolves dedicated same-process interior worlds for players.
 */
public final class InteriorInstanceService implements IInteriorInstanceService, IDependencyInjectableConcrete {
    private static final String WORLD_NAME_PREFIX = "kingdom-interior-";
    private static final String WORLD_GENERATOR = "Void";
    private static final String WORLD_STORAGE = "Empty";

    public CompletableFuture<World> resolveInteriorWorld(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        String worldName = worldNameFor(playerId);
        World existing = Universe.get().getWorld(worldName);
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }
        return Universe.get().addWorld(worldName, WORLD_GENERATOR, WORLD_STORAGE);
    }

    public String worldNameFor(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return WORLD_NAME_PREFIX + playerId.toString().replace("-", "");
    }
}
