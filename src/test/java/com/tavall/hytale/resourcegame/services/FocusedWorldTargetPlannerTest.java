package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.math.vector.Vector3d;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.FocusedWorldTarget;
import com.tavall.hytale.resourcegame.domain.FocusedWorldTargetType;
import com.tavall.hytale.resourcegame.domain.ResourceNodeData;
import com.tavall.hytale.resourcegame.resources.ResourceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class FocusedWorldTargetPlannerTest {
    private final FocusedWorldTargetPlanner planner = new FocusedWorldTargetPlanner();

    @Test
    void resolvesCastleWhenPlayerIsLookingStraightDownPromptLane() {
        CastleLocationData castleLocation = new CastleLocationData("default", 10.0, 64.0, 10.0);
        Optional<FocusedWorldTarget> target = planner.resolve(
                "default",
                new Vector3d(10.0, 64.0, 6.0),
                new Vector3d(0.0, 0.0, 1.0),
                castleLocation,
                List.of()
        );

        assertTrue(target.isPresent());
        assertEquals(FocusedWorldTargetType.CASTLE, target.get().type());
    }

    @Test
    void resolvesNodeWhenNodeIsBetterAlignedThanCastle() {
        UUID nodeId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        ResourceNodeData node = new ResourceNodeData(
                nodeId,
                ResourceType.WOOD,
                "default",
                8.0,
                64.0,
                14.0,
                2,
                120,
                150,
                7,
                Instant.parse("2026-04-15T12:00:00Z")
        );

        Optional<FocusedWorldTarget> target = planner.resolve(
                "default",
                new Vector3d(8.0, 64.0, 10.0),
                new Vector3d(0.0, 0.0, 1.0),
                new CastleLocationData("default", 10.0, 64.0, 10.0),
                List.of(node)
        );

        assertTrue(target.isPresent());
        assertEquals(FocusedWorldTargetType.RESOURCE_NODE, target.get().type());
        assertEquals(nodeId, target.get().nodeId());
    }

    @Test
    void ignoresTargetsOutsideRangeOrAlignmentWindow() {
        Optional<FocusedWorldTarget> target = planner.resolve(
                "default",
                new Vector3d(0.0, 64.0, 0.0),
                new Vector3d(1.0, 0.0, 0.0),
                new CastleLocationData("default", 0.0, 64.0, 10.0),
                List.of()
        );

        assertTrue(target.isEmpty());
    }
}
