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
        teleport(player, null, position);
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

    public void moveWithoutTeleportAck(Player player, Vector3d position) {
        Ref<EntityStore> ref = player.getPlayerRef().getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        Runnable applyMove = () -> {
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) {
                return;
            }
            transform.teleportPosition(position);
            store.putComponent(ref, TransformComponent.getComponentType(), transform);
        };
        if (store.getExternalData() instanceof EntityStore entityStore) {
            World currentWorld = entityStore.getWorld();
            if (currentWorld != null) {
                currentWorld.execute(applyMove);
                return;
            }
        }
        applyMove.run();
    }

    public void orientPlayer(Player player, Vector3d lookTarget) {
        if (lookTarget == null) {
            return;
        }
        Ref<EntityStore> ref = player.getPlayerRef().getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        Runnable applyOrientation = () -> {
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null || transform.getPosition() == null) {
                return;
            }
            Vector3d currentPosition = transform.getPosition();
            double dx = lookTarget.getX() - currentPosition.getX();
            double dy = lookTarget.getY() - currentPosition.getY();
            double dz = lookTarget.getZ() - currentPosition.getZ();
            double horizontalDistance = Math.sqrt((dx * dx) + (dz * dz));
            if (horizontalDistance <= 0.0001D && Math.abs(dy) <= 0.0001D) {
                return;
            }
            float pitch = (float) Math.toDegrees(-Math.atan2(dy, Math.max(horizontalDistance, 0.0001D)));
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float roll = transform.getRotation() == null ? 0.0F : transform.getRotation().getRoll();
            Vector3f rotation = new Vector3f(pitch, yaw, roll);
            transform.setRotation(rotation);
            store.putComponent(ref, TransformComponent.getComponentType(), transform);

            HeadRotation headRotation = store.getComponent(ref, HeadRotation.getComponentType());
            if (headRotation == null) {
                headRotation = new HeadRotation(rotation.clone());
            } else {
                headRotation.setRotation(rotation.clone());
            }
            store.putComponent(ref, HeadRotation.getComponentType(), headRotation);
        };
        if (store.getExternalData() instanceof EntityStore entityStore) {
            World currentWorld = entityStore.getWorld();
            if (currentWorld != null) {
                currentWorld.execute(applyOrientation);
                return;
            }
        }
        applyOrientation.run();
    }
}
