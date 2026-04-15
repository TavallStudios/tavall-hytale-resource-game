package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;

import java.util.UUID;

public interface ICastleSpawnService extends IDependencyInjectableInterface {
    void ensureCastleSpawned(Player player, CastleLocationData locationData);

    void replaceCastle(UUID playerId, CastleLocationData locationData);
}
