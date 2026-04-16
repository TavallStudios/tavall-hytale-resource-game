package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;

import java.util.Optional;
import java.util.UUID;

public interface ICastleBuildingVisualService extends IDependencyInjectableInterface {
    void ensureBuildings(UUID playerId, PlayerGameState state);

    void refreshBuildings(UUID playerId, PlayerGameState state);

    void clearBuildings(UUID playerId);

    Optional<UUID> findBuildingId(UUID playerId, Ref<EntityStore> targetRef);
}
