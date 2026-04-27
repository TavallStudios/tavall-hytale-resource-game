package com.tavall.hytale.resourcegame.interior;

import com.hypixel.hytale.math.vector.Vector3d;
import com.tavall.hytale.resourcegame.domain.BuildingType;
import com.tavall.hytale.resourcegame.domain.CitizenJobType;

import java.util.List;
import java.util.Map;

/**
 * Coordinates for interior placeholder layout.
 */
public final class InteriorLayout {
    private final Vector3d origin;
    private final Vector3d entryPoint;
    private final Vector3d citizenAnchor;
    private final Vector3d troopAnchor;
    private final Vector3d workerPlatformAnchor;
    private final Vector3d workerPortalAnchor;
    private final Map<CitizenJobType, Vector3d> workerAnchors;
    private final Map<BuildingType, Vector3d> buildingAnchors;
    private final Vector3d exitPoint;
    private final List<InteriorTourStop> tourStops;

    public InteriorLayout(
            Vector3d origin,
            Vector3d entryPoint,
            Vector3d citizenAnchor,
            Vector3d troopAnchor,
            Vector3d workerPlatformAnchor,
            Vector3d workerPortalAnchor,
            Map<CitizenJobType, Vector3d> workerAnchors,
            Map<BuildingType, Vector3d> buildingAnchors,
            Vector3d exitPoint,
            List<InteriorTourStop> tourStops
    ) {
        this.origin = origin;
        this.entryPoint = entryPoint;
        this.citizenAnchor = citizenAnchor;
        this.troopAnchor = troopAnchor;
        this.workerPlatformAnchor = workerPlatformAnchor;
        this.workerPortalAnchor = workerPortalAnchor;
        this.workerAnchors = Map.copyOf(workerAnchors);
        this.buildingAnchors = Map.copyOf(buildingAnchors);
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

    public Vector3d workerPlatformAnchor() {
        return workerPlatformAnchor;
    }

    public Vector3d workerPortalAnchor() {
        return workerPortalAnchor;
    }

    public Map<CitizenJobType, Vector3d> workerAnchors() {
        return workerAnchors;
    }

    public Map<BuildingType, Vector3d> buildingAnchors() {
        return buildingAnchors;
    }

    public Vector3d buildingAnchor(BuildingType buildingType) {
        return buildingAnchors.get(buildingType);
    }

    public Vector3d exitPoint() {
        return exitPoint;
    }

    public List<InteriorTourStop> tourStops() {
        return tourStops;
    }
}
