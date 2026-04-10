package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;

public interface ICastleSpawnService extends IDependencyInjectableInterface {
    void ensureCastleSpawned(Player player, CastleLocationData locationData);
}