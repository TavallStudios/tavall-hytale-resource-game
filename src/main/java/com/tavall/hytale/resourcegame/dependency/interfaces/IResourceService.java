package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.resources.ResourceType;

import java.util.UUID;

public interface IResourceService extends IDependencyInjectableInterface {
    PlayerGameState addResource(UUID playerId, ResourceType type, int amount);

    PlayerGameState setResource(UUID playerId, ResourceType type, int amount);
}