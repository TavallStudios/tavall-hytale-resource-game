package com.tavall.hytale.resourcegame.interior;

import com.hypixel.hytale.math.vector.Vector3d;

import java.util.List;

/**
 * Provides consistent interior anchor positions.
 */
public final class InteriorLayoutService {
    public InteriorLayout createLayout(Vector3d origin) {
        Vector3d entryPoint = new Vector3d(origin.getX(), origin.getY(), origin.getZ());
        Vector3d citizenAnchor = new Vector3d(origin.getX() + 3.0, origin.getY() + 1.0, origin.getZ() + 2.0);
        Vector3d troopAnchor = new Vector3d(origin.getX() - 3.0, origin.getY() + 1.0, origin.getZ() + 2.0);
        Vector3d exitPoint = new Vector3d(origin.getX(), origin.getY(), origin.getZ() - 4.0);
        List<InteriorTourStop> tourStops = List.of(
                new InteriorTourStop(
                        1,
                        "Entry Lane",
                        "Start here and orient yourself to the interior shell.",
                        new Vector3d(origin.getX(), origin.getY() + 1.0, origin.getZ() - 1.5)
                ),
                new InteriorTourStop(
                        2,
                        "Citizen Anchor",
                        "Citizens stay visible here so the pipeline always has a readable baseline.",
                        new Vector3d(origin.getX() + 1.5, origin.getY() + 1.0, origin.getZ() + 1.0)
                ),
                new InteriorTourStop(
                        3,
                        "Troop Anchor",
                        "Troops stay visible here while training and battle visuals expand later.",
                        new Vector3d(origin.getX() - 1.5, origin.getY() + 1.0, origin.getZ() + 1.0)
                ),
                new InteriorTourStop(
                        4,
                        "Exit Gate",
                        "Use this path to leave the prototype interior cleanly.",
                        new Vector3d(origin.getX(), origin.getY() + 1.0, origin.getZ() - 3.0)
                )
        );
        return new InteriorLayout(origin, entryPoint, citizenAnchor, troopAnchor, exitPoint, tourStops);
    }
}
