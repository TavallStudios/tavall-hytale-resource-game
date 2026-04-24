package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;

public interface ICastlePromptLaneService extends IDependencyInjectableInterface {
    void alignPlayer(Player player, CastleLocationData castleLocation);
}