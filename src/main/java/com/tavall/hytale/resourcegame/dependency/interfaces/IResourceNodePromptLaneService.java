package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;
import com.tavall.hytale.resourcegame.domain.ResourceNodeData;

public interface IResourceNodePromptLaneService extends IDependencyInjectableInterface {
    void alignPlayer(Player player, ResourceNodeData node);
}
