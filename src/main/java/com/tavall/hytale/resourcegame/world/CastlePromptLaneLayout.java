package com.tavall.hytale.resourcegame.world;

import com.hypixel.hytale.math.vector.Vector3d;

/**
 * Stable prompt-lane coordinates used to line a player up with their castle.
 */
public final class CastlePromptLaneLayout {
    private final Vector3d origin;
    private final Vector3d alignmentPoint;

    public CastlePromptLaneLayout(Vector3d origin, Vector3d alignmentPoint) {
        this.origin = origin;
        this.alignmentPoint = alignmentPoint;
    }

    public Vector3d origin() {
        return origin;
    }

    public Vector3d alignmentPoint() {
        return alignmentPoint;
    }
}
