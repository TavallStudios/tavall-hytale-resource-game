package com.tavall.hytale.resourcegame.interior;

import com.hypixel.hytale.math.vector.Vector3d;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.CitizenJobType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Provides consistent interior anchor positions.
 */
public final class InteriorLayoutService {
    private static final double DEFAULT_Y = 120.0D;
    private static final double INTERIOR_INSTANCE_SPACING = 32.0D;
    private static final double INTERIOR_SLOT_SPACING = 1024.0D;
    private static final int INTERIOR_SLOT_GRID_WIDTH = 8;
    private static final double INTERIOR_SLOT_ORIGIN = 32.5D;

    public Vector3d defaultOrigin() {
        return new Vector3d(0.5D, DEFAULT_Y, 0.5D);
    }

    public Vector3d originFor(UUID playerId) {
        return defaultOrigin();
    }

    public Vector3d originForCastle(CastleLocationData castleLocation) {
        if (castleLocation == null) {
            return defaultOrigin();
        }
        long slot = stableInteriorSlot(castleLocation);
        int column = (int) Math.floorMod(slot, INTERIOR_SLOT_GRID_WIDTH);
        int row = (int) (slot / INTERIOR_SLOT_GRID_WIDTH);
        return new Vector3d(
                INTERIOR_SLOT_ORIGIN + (column * INTERIOR_SLOT_SPACING),
                DEFAULT_Y,
                INTERIOR_SLOT_ORIGIN + (row * INTERIOR_SLOT_SPACING)
        );
    }

    public Vector3d originForCastle(CastleLocationData castleLocation, int interiorInstanceIndex) {
        Vector3d base = originForCastle(castleLocation);
        int safeIndex = Math.max(0, interiorInstanceIndex);
        if (safeIndex == 0) {
            return base;
        }
        return new Vector3d(
                base.getX() + (INTERIOR_INSTANCE_SPACING * safeIndex),
                base.getY(),
                base.getZ()
        );
    }

    public InteriorLayout createLayoutForPlayer(UUID playerId) {
        return createLayout(originFor(playerId));
    }

    public InteriorLayout createLayoutForCastle(CastleLocationData castleLocation) {
        return createLayout(originForCastle(castleLocation));
    }

    public InteriorLayout createLayoutForCastle(CastleLocationData castleLocation, int interiorInstanceIndex) {
        return createLayout(originForCastle(castleLocation, interiorInstanceIndex));
    }

    public InteriorLayout createLayout(Vector3d origin) {
        Vector3d entryPoint = new Vector3d(origin.getX(), origin.getY(), origin.getZ());
        Vector3d citizenAnchor = new Vector3d(origin.getX() + 3.0, origin.getY() + 1.0, origin.getZ() + 2.0);
        Vector3d troopAnchor = new Vector3d(origin.getX() - 3.0, origin.getY() + 1.0, origin.getZ() + 2.0);
        Vector3d workerPlatformAnchor = new Vector3d(origin.getX(), origin.getY() + 1.0, origin.getZ() + 8.0);
        Vector3d workerPortalAnchor = new Vector3d(origin.getX(), origin.getY() + 1.0, origin.getZ() + 4.8);
        Map<CitizenJobType, Vector3d> workerAnchors = workerAnchors(origin);
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
                        "Worker Platform",
                        "Stationary worker anchors live outside the main room. Task copies will leave through the portal.",
                        new Vector3d(origin.getX(), origin.getY() + 1.0, origin.getZ() + 7.0)
                ),
                new InteriorTourStop(
                        5,
                        "Exit Gate",
                        "Use this path to leave the prototype interior cleanly.",
                        new Vector3d(origin.getX(), origin.getY() + 1.0, origin.getZ() - 3.0)
                )
        );
        return new InteriorLayout(origin, entryPoint, citizenAnchor, troopAnchor, workerPlatformAnchor, workerPortalAnchor, workerAnchors, exitPoint, tourStops);
    }

    private Map<CitizenJobType, Vector3d> workerAnchors(Vector3d origin) {
        EnumMap<CitizenJobType, Vector3d> anchors = new EnumMap<>(CitizenJobType.class);
        double x = origin.getX();
        double y = origin.getY() + 1.0;
        double z = origin.getZ() + 8.0;
        anchors.put(CitizenJobType.IDLE, new Vector3d(x - 4.0, y, z - 1.5));
        anchors.put(CitizenJobType.GATHERER, new Vector3d(x - 2.0, y, z - 1.5));
        anchors.put(CitizenJobType.HUNTER, new Vector3d(x, y, z - 1.5));
        anchors.put(CitizenJobType.COOK, new Vector3d(x + 2.0, y, z - 1.5));
        anchors.put(CitizenJobType.MINER, new Vector3d(x + 4.0, y, z - 1.5));
        anchors.put(CitizenJobType.BLACKSMITH, new Vector3d(x - 4.0, y, z + 1.5));
        anchors.put(CitizenJobType.ARCHITECT, new Vector3d(x - 2.0, y, z + 1.5));
        anchors.put(CitizenJobType.GRUNT_BUILDER, new Vector3d(x, y, z + 1.5));
        anchors.put(CitizenJobType.BUILDER, new Vector3d(x + 2.0, y, z + 1.5));
        anchors.put(CitizenJobType.TRAINEE, new Vector3d(x + 4.0, y, z + 1.5));
        anchors.put(CitizenJobType.SOLDIER, new Vector3d(x, y, z + 3.5));
        return anchors;
    }

    private long stableInteriorSlot(CastleLocationData castleLocation) {
        long hash = 1469598103934665603L;
        hash = mix(hash, castleLocation.worldName());
        hash = mix(hash, (long) Math.floor(castleLocation.x()));
        hash = mix(hash, (long) Math.floor(castleLocation.y()));
        hash = mix(hash, (long) Math.floor(castleLocation.z()));
        return Math.floorMod(hash, (long) INTERIOR_SLOT_GRID_WIDTH * INTERIOR_SLOT_GRID_WIDTH);
    }

    private long mix(long current, String value) {
        long hash = current;
        if (value == null) {
            return mix(hash, 0L);
        }
        for (int index = 0; index < value.length(); index++) {
            hash ^= value.charAt(index);
            hash *= 1099511628211L;
        }
        return hash;
    }

    private long mix(long current, long value) {
        long hash = current ^ value;
        hash *= 1099511628211L;
        return hash;
    }
}
