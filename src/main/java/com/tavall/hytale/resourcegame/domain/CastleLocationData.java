package com.tavall.hytale.resourcegame.domain;

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
}
