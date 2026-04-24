package com.tavall.hytale.resourcegame.world;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks castle entity references per player.
 */
public final class CastleEntityRegistry {
    private final Map<UUID, Ref<EntityStore>> castleRefs = new ConcurrentHashMap<>();

    public void register(UUID playerId, Ref<EntityStore> castleRef) {
        if (playerId == null || castleRef == null) {
            return;
        }
        castleRefs.put(playerId, castleRef);
    }

    public Ref<EntityStore> get(UUID playerId) {
        return castleRefs.get(playerId);
    }

    public Ref<EntityStore> remove(UUID playerId) {
        return castleRefs.remove(playerId);
    }
}
