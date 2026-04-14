package com.tavall.hytale.resourcegame.interior;

import com.hypixel.hytale.math.vector.Vector3d;

import java.util.List;

/**
 * Coordinates for interior placeholder layout.
 */
public final class InteriorLayout {
    private final Vector3d origin;
    private final Vector3d entryPoint;
    private final Vector3d citizenAnchor;
    private final Vector3d troopAnchor;
    private final Vector3d exitPoint;
    private final List<InteriorTourStop> tourStops;

    public InteriorLayout(
            Vector3d origin,
            Vector3d entryPoint,
            Vector3d citizenAnchor,
            Vector3d troopAnchor,
            Vector3d exitPoint,
            List<InteriorTourStop> tourStops
    ) {
        this.origin = origin;
        this.entryPoint = entryPoint;
        this.citizenAnchor = citizenAnchor;
        this.troopAnchor = troopAnchor;
        this.exitPoint = exitPoint;
        this.tourStops = List.copyOf(tourStops);
    }

    public Vector3d origin() {
        return origin;
    }

    public Vector3d entryPoint() {
        return entryPoint;
    }

    public Vector3d citizenAnchor() {
        return citizenAnchor;
    }

    public Vector3d troopAnchor() {
        return troopAnchor;
    }

    public Vector3d exitPoint() {
        return exitPoint;
    }

    public List<InteriorTourStop> tourStops() {
        return tourStops;
    }
}
