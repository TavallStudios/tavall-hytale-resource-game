package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.math.vector.Vector3d;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.ResourceNodeData;
import com.tavall.hytale.resourcegame.domain.ResourceNodeSummary;
import com.tavall.hytale.resourcegame.resources.ResourceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ResourceNodeRoutePlannerTest {
    @Test
    void routePositionsShiftAcrossPulseWindow() {
        ResourceNodeRoutePlanner planner = new ResourceNodeRoutePlanner();
        CastleLocationData castleLocation = new CastleLocationData("default", 0.0, 72.0, 0.0);
        ResourceNodeData node = new ResourceNodeData(
                UUID.randomUUID(),
                ResourceType.WOOD,
                "default",
                12.0,
                72.0,
                0.0,
                6,
                150,
                150,
                7,
                Instant.parse("2026-04-15T17:00:00Z")
        );
        ResourceNodeSummary summary = new ResourceNodeSummary(node, 3, 6, 18, 150, 150, 7, 100, 2, "Rich");

        List<Vector3d> first = planner.routePositions(castleLocation, summary, Instant.parse("2026-04-15T17:00:00Z"), 4);
        List<Vector3d> second = planner.routePositions(castleLocation, summary, Instant.parse("2026-04-15T17:00:03Z"), 4);

        assertEquals(2, first.size());
        assertEquals(2, second.size());
        assertNotEquals(first.getFirst().getX(), second.getFirst().getX());
        assertTrue(first.stream().allMatch(position -> position.getX() > 0.0 && position.getX() < 12.0));
    }
}
