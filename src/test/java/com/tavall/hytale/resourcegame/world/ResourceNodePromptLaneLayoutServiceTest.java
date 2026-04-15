package com.tavall.hytale.resourcegame.world;

import com.tavall.hytale.resourcegame.domain.ResourceNodeData;
import com.tavall.hytale.resourcegame.resources.ResourceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ResourceNodePromptLaneLayoutServiceTest {
    @Test
    void promptLaneStandsFourBlocksInFrontOfNodeLane() {
        ResourceNodePromptLaneLayoutService service = new ResourceNodePromptLaneLayoutService();
        ResourceNodeData node = new ResourceNodeData(
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                ResourceType.IRON,
                "default",
                18.5,
                71.0,
                -4.5,
                0,
                120,
                120,
                5,
                Instant.parse("2026-04-15T13:00:00Z")
        );

        ResourceNodePromptLaneLayout layout = service.createLayout(node);

        assertEquals(18.5, layout.alignmentPoint().getX());
        assertEquals(71.0, layout.alignmentPoint().getY());
        assertEquals(-8.5, layout.alignmentPoint().getZ());
    }
}
