package com.tavall.hytale.resourcegame.interior;

import com.hypixel.hytale.math.vector.Vector3d;

/**
 * Provides consistent interior anchor positions.
 */
public final class InteriorLayoutService {
    public InteriorLayout createLayout(Vector3d origin) {
        Vector3d entryPoint = new Vector3d(origin.getX(), origin.getY(), origin.getZ());
        Vector3d citizenAnchor = new Vector3d(origin.getX() + 3.0, origin.getY() + 1.0, origin.getZ() + 2.0);
        Vector3d troopAnchor = new Vector3d(origin.getX() - 3.0, origin.getY() + 1.0, origin.getZ() + 2.0);
        Vector3d exitPoint = new Vector3d(origin.getX(), origin.getY(), origin.getZ() - 4.0);
        return new InteriorLayout(origin, entryPoint, citizenAnchor, troopAnchor, exitPoint);
    }
}
