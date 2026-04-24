package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;

public interface IPlayerTeleportService extends IDependencyInjectableInterface {
    Vector3d standingPosition(Player player, Vector3d floorPosition);

    void teleportAfterDelay(Player player, Vector3d position, long delayMillis);

    void teleport(Player player, Vector3d position);

    void teleport(Player player, World targetWorld, Vector3d position);

    void moveWithoutTeleportAck(Player player, Vector3d position);

    void orientPlayer(Player player, Vector3d lookTarget);
}
