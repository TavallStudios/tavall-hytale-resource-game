package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.tavall.hytale.resourcegame.config.CastleAssetConfig;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleSiteVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleSpawnService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerSessionStore;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.world.CastleEntityRegistry;
import it.unimi.dsi.fastutil.Pair;

import java.util.Objects;
import java.util.UUID;

/**
 * Spawns and tracks player castles.
 */
public final class CastleSpawnService implements ICastleSpawnService, IDependencyInjectableConcrete {
    private final CastleAssetConfig assetConfig;
    private final CastleEntityRegistry registry;
    private final IPlayerSessionStore sessionStore;
    private final ICastleSiteVisualService castleSiteVisualService;

    public CastleSpawnService(
            CastleAssetConfig assetConfig,
            CastleEntityRegistry registry,
            IPlayerSessionStore sessionStore,
            ICastleSiteVisualService castleSiteVisualService
    ) {
        this.assetConfig = Objects.requireNonNull(assetConfig, "assetConfig");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.castleSiteVisualService = Objects.requireNonNull(castleSiteVisualService, "castleSiteVisualService");
    }

    public void ensureCastleSpawned(Player player, CastleLocationData locationData) {
        if (locationData == null) {
            return;
        }
        UUID playerId = player.getUuid();
        Ref<EntityStore> existingRef = registry.get(playerId);
        if (existingRef == null || !existingRef.isValid()) {
            World world = resolveWorld(locationData, player.getWorld());
            spawnCastleForPlayer(playerId, world, locationData);
        }
        PlayerSession session = sessionStore.get(playerId);
        if (session != null) {
            castleSiteVisualService.ensureSite(playerId, session.gameState());
        }
    }

    @Override
    public void replaceCastle(UUID playerId, CastleLocationData locationData) {
        if (playerId == null || locationData == null) {
            return;
        }
        World world = resolveWorld(locationData, null);
        if (world == null) {
            return;
        }
        world.execute(() -> {
            removeCastle(playerId);
            spawnCastleForPlayer(playerId, world, locationData);
        });
    }

    private void spawnCastleForPlayer(UUID playerId, World world, CastleLocationData locationData) {
        if (world == null || locationData == null) {
            return;
        }
        Store<EntityStore> store = world.getEntityStore().getStore();
        Ref<EntityStore> castleRef = spawnCastle(store, locationData.toVector());
        if (castleRef != null) {
            registry.register(playerId, castleRef);
        }
    }

    private World resolveWorld(CastleLocationData locationData, World fallbackWorld) {
        if (locationData == null) {
            return fallbackWorld;
        }
        World world = Universe.get().getWorld(locationData.worldName());
        return world == null ? fallbackWorld : world;
    }

    private void removeCastle(UUID playerId) {
        Ref<EntityStore> existingRef = registry.remove(playerId);
        if (existingRef != null && existingRef.isValid()) {
            existingRef.getStore().removeEntity(existingRef, RemoveReason.REMOVE);
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
            store.putComponent(ref, EntityScaleComponent.getComponentType(), new EntityScaleComponent(2.35F));
        }
        return ref;
    }
}
