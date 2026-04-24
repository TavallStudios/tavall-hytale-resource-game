package com.tavall.hytale.resourcegame.world;

import com.hypixel.hytale.math.vector.Vector3d;

import java.util.List;

/**
 * Stable anchor points for the main-world castle scene.
 */
public final class CastleSiteLayout {
    private final Vector3d origin;
    private final Vector3d stockpileAnchor;
    private final Vector3d citizenAnchor;
    private final Vector3d troopAnchor;
    private final Vector3d foodNodeAnchor;
    private final Vector3d woodNodeAnchor;
    private final Vector3d ironNodeAnchor;
    private final List<Vector3d> stockpilePositions;
    private final List<Vector3d> citizenCrowdPositions;
    private final List<Vector3d> troopCrowdPositions;
    private final List<Vector3d> foodNodePositions;
    private final List<Vector3d> woodNodePositions;
    private final List<Vector3d> ironNodePositions;
    private final List<Vector3d> foodConvoyPositions;
    private final List<Vector3d> woodConvoyPositions;
    private final List<Vector3d> ironConvoyPositions;

    public CastleSiteLayout(
            Vector3d origin,
            Vector3d stockpileAnchor,
            Vector3d citizenAnchor,
            Vector3d troopAnchor,
            Vector3d foodNodeAnchor,
            Vector3d woodNodeAnchor,
            Vector3d ironNodeAnchor,
            List<Vector3d> stockpilePositions,
            List<Vector3d> citizenCrowdPositions,
            List<Vector3d> troopCrowdPositions,
            List<Vector3d> foodNodePositions,
            List<Vector3d> woodNodePositions,
            List<Vector3d> ironNodePositions,
            List<Vector3d> foodConvoyPositions,
            List<Vector3d> woodConvoyPositions,
            List<Vector3d> ironConvoyPositions
    ) {
        this.origin = origin;
        this.stockpileAnchor = stockpileAnchor;
        this.citizenAnchor = citizenAnchor;
        this.troopAnchor = troopAnchor;
        this.foodNodeAnchor = foodNodeAnchor;
        this.woodNodeAnchor = woodNodeAnchor;
        this.ironNodeAnchor = ironNodeAnchor;
        this.stockpilePositions = List.copyOf(stockpilePositions);
        this.citizenCrowdPositions = List.copyOf(citizenCrowdPositions);
        this.troopCrowdPositions = List.copyOf(troopCrowdPositions);
        this.foodNodePositions = List.copyOf(foodNodePositions);
        this.woodNodePositions = List.copyOf(woodNodePositions);
        this.ironNodePositions = List.copyOf(ironNodePositions);
        this.foodConvoyPositions = List.copyOf(foodConvoyPositions);
        this.woodConvoyPositions = List.copyOf(woodConvoyPositions);
        this.ironConvoyPositions = List.copyOf(ironConvoyPositions);
    }

    public Vector3d origin() {
        return origin;
    }

    public Vector3d stockpileAnchor() {
        return stockpileAnchor;
    }

    public Vector3d citizenAnchor() {
        return citizenAnchor;
    }

    public Vector3d troopAnchor() {
        return troopAnchor;
    }

    public Vector3d foodNodeAnchor() {
        return foodNodeAnchor;
    }

    public Vector3d woodNodeAnchor() {
        return woodNodeAnchor;
    }

    public Vector3d ironNodeAnchor() {
        return ironNodeAnchor;
    }

    public List<Vector3d> stockpilePositions() {
        return stockpilePositions;
    }

    public List<Vector3d> citizenCrowdPositions() {
        return citizenCrowdPositions;
    }

    public List<Vector3d> troopCrowdPositions() {
        return troopCrowdPositions;
    }

    public List<Vector3d> foodNodePositions() {
        return foodNodePositions;
    }

    public List<Vector3d> woodNodePositions() {
        return woodNodePositions;
    }

    public List<Vector3d> ironNodePositions() {
        return ironNodePositions;
    }

    public List<Vector3d> foodConvoyPositions() {
        return foodConvoyPositions;
    }

    public List<Vector3d> woodConvoyPositions() {
        return woodConvoyPositions;
    }

    public List<Vector3d> ironConvoyPositions() {
        return ironConvoyPositions;
    }
}
