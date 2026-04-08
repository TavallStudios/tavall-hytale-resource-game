package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.tavall.hytale.resourcegame.config.CastleAssetConfig;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.world.CastleEntityRegistry;
import it.unimi.dsi.fastutil.Pair;

import java.util.Objects;
import java.util.UUID;

/**
 * Spawns and tracks player castles.
 */
public final class CastleSpawnService {
    private final CastleAssetConfig assetConfig;
    private final CastleEntityRegistry registry;

    public CastleSpawnService(CastleAssetConfig assetConfig, CastleEntityRegistry registry) {
        this.assetConfig = Objects.requireNonNull(assetConfig, "assetConfig");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public void ensureCastleSpawned(Player player, CastleLocationData locationData) {
        if (locationData == null) {
            return;
        }
        UUID playerId = player.getUuid();
        if (registry.get(playerId) != null) {
            return;
        }
        World world = player.getWorld();
        Store<EntityStore> store = world.getEntityStore().getStore();
        Vector3d position = new Vector3d(locationData.x(), locationData.y(), locationData.z());
        Ref<EntityStore> castleRef = spawnCastle(store, position);
        if (castleRef != null) {
            registry.register(playerId, castleRef);
        }
    }

    private Ref<EntityStore> spawnCastle(Store<EntityStore> store, Vector3d position) {
        NPCPlugin npcPlugin = NPCPlugin.get();
        int roleIndex = npcPlugin.getIndex(assetConfig.npcRoleName());
        if (roleIndex < 0) {
            return null;
        }
        Pair<Ref<EntityStore>, NPCEntity> pair = npcPlugin.spawnEntity(store, roleIndex, position, Vector3f.ZERO, null, null);
        Ref<EntityStore> ref = pair.first();
        if (ref != null && ref.isValid()) {
            store.putComponent(ref, DisplayNameComponent.getComponentType(), new DisplayNameComponent(Message.raw(assetConfig.displayName())));
        }
        return ref;
    }
}
