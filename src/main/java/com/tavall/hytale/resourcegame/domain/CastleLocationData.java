package com.tavall.hytale.resourcegame.domain;

import com.hypixel.hytale.math.vector.Vector3d;

import java.util.Objects;

/**
 * Persistent location metadata for a castle.
 */
public final class CastleLocationData {
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;

    public CastleLocationData(String worldName, double x, double y, double z) {
        this.worldName = Objects.requireNonNull(worldName, "worldName");
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String worldName() {
        return worldName;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public Vector3d toVector() {
        return new Vector3d(x, y, z);
    }

    public Vector3d supportBlockVector() {
        return new Vector3d(x, Math.floor(y) - 1.0D, z);
    }

    public Vector3d standingBaseVector() {
        return new Vector3d(x, Math.floor(y), z);
    }
}
