package com.tavall.hytale.resourcegame.interior;

import com.hypixel.hytale.math.vector.Vector3d;

import java.util.List;
import java.util.UUID;

/**
 * Provides consistent interior anchor positions.
 */
public final class InteriorLayoutService {
    private static final double DEFAULT_Y = 120.0D;
    private static final double CELL_SPACING = 96.0D;
    private static final int CELL_OFFSET = 128;

    public Vector3d defaultOrigin() {
        return new Vector3d(0.5D, DEFAULT_Y, 0.5D);
    }

    public Vector3d originFor(UUID playerId) {
        if (playerId == null) {
            return defaultOrigin();
        }
        String normalized = playerId.toString().replace("-", "");
        long xSeed = Long.parseUnsignedLong(normalized.substring(0, 8), 16);
        long zSeed = Long.parseUnsignedLong(normalized.substring(8, 16), 16);
        int xCell = (int) (xSeed % 256L) - CELL_OFFSET;
        int zCell = (int) (zSeed % 256L) - CELL_OFFSET;
        return new Vector3d((xCell * CELL_SPACING) + 0.5D, DEFAULT_Y, (zCell * CELL_SPACING) + 0.5D);
    }

    public InteriorLayout createLayoutForPlayer(UUID playerId) {
        return createLayout(originFor(playerId));
    }

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
