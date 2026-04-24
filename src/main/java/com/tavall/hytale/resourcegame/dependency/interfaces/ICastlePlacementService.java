package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;

import java.time.Instant;
import java.util.UUID;

/**
 * Owns persisted castle relocation for a player.
 */
public interface ICastlePlacementService extends IDependencyInjectableInterface {
    PlayerGameState placeCastle(UUID playerId, CastleLocationData castleLocation, Instant now);
}
