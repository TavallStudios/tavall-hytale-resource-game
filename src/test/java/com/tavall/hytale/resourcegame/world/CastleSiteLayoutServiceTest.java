package com.tavall.hytale.resourcegame.world;

import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CastleSiteLayoutServiceTest {
    @Test
    void layoutPlacesResourceNodesBehindThePromptLaneAndAroundTheCastle() {
        CastleSiteLayout layout = new CastleSiteLayoutService().createLayout(new CastleLocationData("default", 20.0, 64.7, 40.0));

        assertEquals(20.0, layout.origin().getX());
        assertEquals(64.0, layout.origin().getY());
        assertEquals(44.0, layout.stockpileAnchor().getZ());
        assertEquals(25.0, layout.citizenAnchor().getX());
        assertEquals(15.0, layout.troopAnchor().getX());
        assertTrue(layout.foodNodeAnchor().getZ() > layout.origin().getZ());
        assertTrue(layout.woodNodeAnchor().getZ() > layout.origin().getZ());
        assertTrue(layout.ironNodeAnchor().getZ() > layout.origin().getZ());
        assertEquals(6, layout.stockpilePositions().size());
        assertEquals(6, layout.citizenCrowdPositions().size());
        assertEquals(6, layout.troopCrowdPositions().size());
        assertEquals(4, layout.foodNodePositions().size());
        assertEquals(4, layout.woodNodePositions().size());
        assertEquals(4, layout.ironNodePositions().size());
        assertEquals(4, layout.foodConvoyPositions().size());
        assertEquals(4, layout.woodConvoyPositions().size());
        assertEquals(4, layout.ironConvoyPositions().size());
    }
}
