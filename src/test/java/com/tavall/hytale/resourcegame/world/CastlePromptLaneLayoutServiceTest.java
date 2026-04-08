package com.tavall.hytale.resourcegame.world;

import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class CastlePromptLaneLayoutServiceTest {
    @Test
    void createLayoutBuildsStableStandingPointInFrontOfCastle() {
        CastlePromptLaneLayoutService service = new CastlePromptLaneLayoutService();

        CastlePromptLaneLayout layout = service.createLayout(new CastleLocationData("overworld", 18.5, 72.9, 33.5));

        assertEquals(18.5, layout.origin().getX());
        assertEquals(72.0, layout.origin().getY());
        assertEquals(29.5, layout.origin().getZ());

        assertEquals(18.5, layout.alignmentPoint().getX());
        assertEquals(72.0, layout.alignmentPoint().getY());
        assertEquals(29.5, layout.alignmentPoint().getZ());
    }
}
