package com.tavall.hytale.resourcegame.support;

import com.hypixel.hytale.server.core.universe.world.World;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInteriorInstanceService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class StubInteriorInstanceService implements IInteriorInstanceService {
    private static final String WORLD_NAME = "kingdom-interiors";

    @Override
    public CompletableFuture<World> resolveInteriorWorld(UUID playerId) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<World> warmInteriorWorld() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public String worldNameFor(UUID playerId) {
        return WORLD_NAME;
    }

    @Override
    public void releaseInteriorWorld(UUID playerId) {
    }

    @Override
    public void pruneTransientWorlds() {
    }
}
