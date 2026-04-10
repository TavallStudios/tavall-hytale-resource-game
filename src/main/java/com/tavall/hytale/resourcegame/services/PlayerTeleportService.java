package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerTeleportService;
import java.util.concurrent.TimeUnit;

/**
 * Centralizes safe player teleports through the server teleport component pipeline.
 */
public final class PlayerTeleportService implements IPlayerTeleportService, IDependencyInjectableConcrete {
    public Vector3d standingPosition(Player player, Vector3d floorPosition) {
        Ref<EntityStore> ref = player.getPlayerRef().getReference();
        if (ref == null || !ref.isValid()) {
            return floorPosition;
        }
        Store<EntityStore> store = ref.getStore();
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            return floorPosition;
        }
        double currentY = transform.getPosition().getY();
        double standingOffset = currentY - Math.floor(currentY);
        if (standingOffset <= 0.0D) {
            standingOffset = 0.1D;
        }
        return new Vector3d(floorPosition.getX(), floorPosition.getY() + standingOffset, floorPosition.getZ());
    }

    public void teleportAfterDelay(Player player, Vector3d position, long delayMillis) {
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> player.getWorld().execute(() -> teleport(player, position)),
                delayMillis,
                TimeUnit.MILLISECONDS
        );
    }

    public void teleport(Player player, Vector3d position) {
        teleport(player, player.getWorld(), position);
    }

    public void teleport(Player player, World targetWorld, Vector3d position) {
        Ref<EntityStore> ref = player.getPlayerRef().getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        Runnable applyTeleport = () -> {
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) {
                return;
            }
            HeadRotation headRotation = store.getComponent(ref, HeadRotation.getComponentType());
            Vector3f bodyRotation = transform.getRotation().clone();
            Vector3f headRotationValue = headRotation != null
                    ? headRotation.getRotation().clone()
                    : bodyRotation.clone();
            Transform targetTransform = new Transform(position, bodyRotation);
            Teleport teleport = targetWorld == null
                    ? Teleport.createForPlayer(targetTransform)
                    : Teleport.createForPlayer(targetWorld, targetTransform);
            store.putComponent(ref, Teleport.getComponentType(), teleport.setHeadRotation(headRotationValue));
        };
        if (store.getExternalData() instanceof EntityStore entityStore) {
            World currentWorld = entityStore.getWorld();
            if (currentWorld != null) {
                currentWorld.execute(applyTeleport);
                return;
            }
        }
        applyTeleport.run();
    }
}
