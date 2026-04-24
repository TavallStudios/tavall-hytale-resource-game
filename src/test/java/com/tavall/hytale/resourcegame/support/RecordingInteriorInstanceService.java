package com.tavall.hytale.resourcegame.support;

import com.hypixel.hytale.server.core.universe.world.World;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInteriorInstanceService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public final class RecordingInteriorInstanceService implements IInteriorInstanceService {
    private final AtomicReference<UUID> releasedPlayerId = new AtomicReference<>();

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
        return "kingdom-interiors";
    }

    @Override
    public void releaseInteriorWorld(UUID playerId) {
        releasedPlayerId.set(playerId);
    }

    @Override
    public void pruneTransientWorlds() {
    }

    public UUID releasedPlayerId() {
        return releasedPlayerId.get();
    }
}

