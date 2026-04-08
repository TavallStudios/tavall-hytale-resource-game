package com.tavall.hytale.resourcegame.world;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;

/**
 * Vector math helpers for interaction checks.
 */
public final class VectorMath {
    private VectorMath() {
    }

    public static Vector3d lookVector(Vector3f rotation) {
        double yawRad = Math.toRadians(rotation.getYaw());
        double pitchRad = Math.toRadians(rotation.getPitch());
        double cosPitch = Math.cos(pitchRad);
        return new Vector3d(
                -Math.sin(yawRad) * cosPitch,
                -Math.sin(pitchRad),
                Math.cos(yawRad) * cosPitch
        );
    }

    public static double dot(Vector3d a, Vector3d b) {
        return a.getX() * b.getX() + a.getY() * b.getY() + a.getZ() * b.getZ();
    }

    public static Vector3d normalize(Vector3d value) {
        double length = Math.sqrt(value.getX() * value.getX() + value.getY() * value.getY() + value.getZ() * value.getZ());
        if (length == 0) {
            return new Vector3d(0, 0, 0);
        }
        return new Vector3d(value.getX() / length, value.getY() / length, value.getZ() / length);
    }
}
