package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.math.vector.Vector3d;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.ResourceNodeData;
import com.tavall.hytale.resourcegame.domain.ResourceNodeSummary;
import com.tavall.hytale.resourcegame.world.VectorMath;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes moving convoy marker positions between a castle and a node.
 */
public final class ResourceNodeRoutePlanner {
    private static final double OUTBOUND_LANE_OFFSET = 0.75;
    private static final double RETURN_LANE_OFFSET = -0.75;
    private static final double ROUTE_PROGRESS_SPAN = 0.7;
    private static final double ROUTE_PROGRESS_START = 0.15;
    private static final int ROUTE_PERIOD_SECONDS = 8;

    public List<Vector3d> routePositions(CastleLocationData castleLocation, ResourceNodeSummary summary, Instant now, int limit) {
        if (castleLocation == null || summary.visibleRouteCount() <= 0 || limit <= 0) {
            return List.of();
        }
        ResourceNodeData node = summary.node();
        Vector3d start = new Vector3d(castleLocation.x(), castleLocation.y() + 1.0, castleLocation.z());
        Vector3d end = new Vector3d(node.x(), node.y(), node.z());
        Vector3d direction = new Vector3d(end.getX() - start.getX(), end.getY() - start.getY(), end.getZ() - start.getZ());
        double distance = Math.sqrt(direction.getX() * direction.getX() + direction.getY() * direction.getY() + direction.getZ() * direction.getZ());
        if (distance == 0) {
            return List.of();
        }
        Vector3d normalized = VectorMath.normalize(direction);
        Vector3d lateral = VectorMath.perpendicularFlat(normalized);
        int visibleCount = Math.min(limit, summary.visibleRouteCount());
        double phase = phase(now);
        List<Vector3d> positions = new ArrayList<>();
        for (int index = 0; index < visibleCount; index++) {
            double slotOffset = visibleCount == 1 ? 0.0 : index / (double) visibleCount;
            boolean outbound = index % 2 == 0;
            double baseProgress = wrap(phase + slotOffset);
            double laneProgress = ROUTE_PROGRESS_START + (ROUTE_PROGRESS_SPAN * baseProgress);
            double progress = outbound ? laneProgress : 1.0 - laneProgress;
            double laneOffset = outbound ? OUTBOUND_LANE_OFFSET : RETURN_LANE_OFFSET;
            positions.add(new Vector3d(
                    start.getX() + normalized.getX() * distance * progress + lateral.getX() * laneOffset,
                    start.getY() + normalized.getY() * distance * progress,
                    start.getZ() + normalized.getZ() * distance * progress + lateral.getZ() * laneOffset
            ));
        }
        return List.copyOf(positions);
    }

    private double phase(Instant now) {
        long seconds = now.getEpochSecond();
        long window = Math.floorMod(seconds, ROUTE_PERIOD_SECONDS);
        return window / (double) ROUTE_PERIOD_SECONDS;
    }

    private double wrap(double value) {
        double wrapped = value % 1.0;
        return wrapped < 0 ? wrapped + 1.0 : wrapped;
    }
}
