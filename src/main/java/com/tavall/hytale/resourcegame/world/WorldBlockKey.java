package com.tavall.hytale.resourcegame.world;

import com.hypixel.hytale.math.vector.Vector3i;

import java.util.Objects;

/**
 * Stable world-space key for a single block coordinate.
 */
public final class WorldBlockKey {
    private final String worldName;
    private final int x;
    private final int y;
    private final int z;

    public WorldBlockKey(String worldName, int x, int y, int z) {
        this.worldName = Objects.requireNonNull(worldName, "worldName");
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static WorldBlockKey of(String worldName, Vector3i blockPosition) {
        Objects.requireNonNull(blockPosition, "blockPosition");
        return new WorldBlockKey(worldName, blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
    }

    public String worldName() {
        return worldName;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WorldBlockKey that)) {
            return false;
        }
        return x == that.x
                && y == that.y
                && z == that.z
                && worldName.equals(that.worldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldName, x, y, z);
    }
}
