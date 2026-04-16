package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.hypixel.hytale.server.core.universe.world.World;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IInteriorInstanceService extends IDependencyInjectableInterface {
    CompletableFuture<World> resolveInteriorWorld(UUID playerId);

    CompletableFuture<World> warmInteriorWorld();

    String worldNameFor(UUID playerId);

    void releaseInteriorWorld(UUID playerId);

    void pruneTransientWorlds();
}
