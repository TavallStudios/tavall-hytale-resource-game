package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.math.vector.Vector3d;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.CastleBuildingSummary;
import com.tavall.hytale.resourcegame.domain.FocusedWorldTarget;
import com.tavall.hytale.resourcegame.domain.ResourceNodeData;
import com.tavall.hytale.resourcegame.world.VectorMath;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves the strongest focus candidate from the player's current world-space look direction.
 */
public final class FocusedWorldTargetPlanner implements IDependencyInjectableConcrete {
    private static final double CASTLE_MAX_DISTANCE = 7.5D;
    private static final double CASTLE_MIN_DOT = 0.85D;
    private static final double NODE_MAX_DISTANCE = 7.5D;
    private static final double NODE_MIN_DOT = 0.82D;
    private static final double BUILDING_MAX_DISTANCE = 7.5D;
    private static final double BUILDING_MIN_DOT = 0.81D;

    public Optional<FocusedWorldTarget> resolve(
            String worldName,
            Vector3d playerPosition,
            Vector3d lookVector,
            CastleLocationData castleLocation,
            List<ResourceNodeData> nodes,
            List<CastleBuildingSummary> buildings
    ) {
        if (worldName == null || playerPosition == null || lookVector == null) {
            return Optional.empty();
        }
        FocusCandidate best = null;
        if (castleLocation != null && Objects.equals(worldName, castleLocation.worldName())) {
            best = bestCandidate(best, castleCandidate(playerPosition, lookVector, castleLocation));
        }
        if (nodes != null) {
            for (ResourceNodeData node : nodes) {
                if (!Objects.equals(worldName, node.worldName())) {
                    continue;
                }
                best = bestCandidate(best, nodeCandidate(playerPosition, lookVector, node));
            }
        }
        if (buildings != null) {
            for (CastleBuildingSummary building : buildings) {
                if (!Objects.equals(worldName, building.worldName())) {
                    continue;
                }
                best = bestCandidate(best, buildingCandidate(playerPosition, lookVector, building));
            }
        }
        return best == null ? Optional.empty() : Optional.of(best.target());
    }

    private FocusCandidate bestCandidate(FocusCandidate currentBest, FocusCandidate candidate) {
        if (candidate == null) {
            return currentBest;
        }
        if (currentBest == null || candidate.score() > currentBest.score()) {
            return candidate;
        }
        return currentBest;
    }

    private FocusCandidate castleCandidate(Vector3d playerPosition, Vector3d lookVector, CastleLocationData castleLocation) {
        Vector3d castlePosition = new Vector3d(castleLocation.x(), castleLocation.y(), castleLocation.z());
        return candidate(playerPosition, lookVector, castlePosition, CASTLE_MAX_DISTANCE, CASTLE_MIN_DOT, FocusedWorldTarget.castle(0.0D, 0.0D));
    }

    private FocusCandidate nodeCandidate(Vector3d playerPosition, Vector3d lookVector, ResourceNodeData node) {
        Vector3d nodePosition = new Vector3d(node.x(), node.y(), node.z());
        String label = node.resourceType() + " node " + node.nodeId().toString().substring(0, 8);
        return candidate(
                playerPosition,
                lookVector,
                nodePosition,
                NODE_MAX_DISTANCE,
                NODE_MIN_DOT,
                FocusedWorldTarget.resourceNode(node.nodeId(), label, 0.0D, 0.0D)
        );
    }

    private FocusCandidate buildingCandidate(Vector3d playerPosition, Vector3d lookVector, CastleBuildingSummary building) {
        Vector3d buildingPosition = new Vector3d(building.worldX(), building.worldY(), building.worldZ());
        String label = building.buildingData().buildingType().displayName() + " " + building.buildingData().buildingId().toString().substring(0, 8);
        return candidate(
                playerPosition,
                lookVector,
                buildingPosition,
                BUILDING_MAX_DISTANCE,
                BUILDING_MIN_DOT,
                FocusedWorldTarget.building(building.buildingData().buildingId(), label, 0.0D, 0.0D)
        );
    }

    private FocusCandidate candidate(
            Vector3d playerPosition,
            Vector3d lookVector,
            Vector3d targetPosition,
            double maxDistance,
            double minDot,
            FocusedWorldTarget template
    ) {
        Vector3d toTarget = new Vector3d(
                targetPosition.getX() - playerPosition.getX(),
                targetPosition.getY() - playerPosition.getY(),
                targetPosition.getZ() - playerPosition.getZ()
        );
        double distance = Math.sqrt(
                (toTarget.getX() * toTarget.getX())
                        + (toTarget.getY() * toTarget.getY())
                        + (toTarget.getZ() * toTarget.getZ())
        );
        if (distance <= 0.0D || distance > maxDistance) {
            return null;
        }
        Vector3d normalizedTarget = VectorMath.normalize(toTarget);
        double dot = VectorMath.dot(lookVector, normalizedTarget);
        if (dot < minDot) {
            return null;
        }
        double score = dot - (distance * 0.01D);
        FocusedWorldTarget target = new FocusedWorldTarget(
                template.type(),
                template.nodeId(),
                template.buildingId(),
                template.label(),
                distance,
                dot
        );
        return new FocusCandidate(target, score);
    }

}
