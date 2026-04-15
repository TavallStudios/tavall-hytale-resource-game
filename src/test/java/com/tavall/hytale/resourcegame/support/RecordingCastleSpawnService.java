package com.tavall.hytale.resourcegame.support;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleSpawnService;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RecordingCastleSpawnService implements ICastleSpawnService {
    private final Map<UUID, CastleLocationData> replacedLocations = new ConcurrentHashMap<>();

    @Override
    public void ensureCastleSpawned(Player player, CastleLocationData locationData) {
    }

    @Override
    public void replaceCastle(UUID playerId, CastleLocationData locationData) {
        if (playerId == null || locationData == null) {
            return;
        }
        replacedLocations.put(playerId, locationData);
    }

    public CastleLocationData replacedLocation(UUID playerId) {
        return replacedLocations.get(playerId);
    }
}
