package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;

import java.util.Optional;
import java.util.UUID;

public interface IResourceNodeVisualService extends IDependencyInjectableInterface {
    void ensureNodes(UUID playerId, PlayerGameState state);

    void refreshNodes(UUID playerId, PlayerGameState state);

    void clearNodes(UUID playerId);

    Optional<UUID> findNodeId(UUID playerId, Ref<EntityStore> targetRef);
}
