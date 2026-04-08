package com.tavall.hytale.resourcegame.interior;

import com.hypixel.hytale.math.vector.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class InteriorLayoutServiceTest {
    @Test
    void createLayoutBuildsStableEntryAndAnchorPositions() {
        InteriorLayoutService service = new InteriorLayoutService();

        InteriorLayout layout = service.createLayout(new Vector3d(100.5, 200.0, 300.5));

        assertEquals(100.5, layout.origin().getX());
        assertEquals(200.0, layout.origin().getY());
        assertEquals(300.5, layout.origin().getZ());

        assertEquals(100.5, layout.entryPoint().getX());
        assertEquals(200.0, layout.entryPoint().getY());
        assertEquals(300.5, layout.entryPoint().getZ());

        assertEquals(103.5, layout.citizenAnchor().getX());
        assertEquals(201.0, layout.citizenAnchor().getY());
        assertEquals(302.5, layout.citizenAnchor().getZ());

        assertEquals(97.5, layout.troopAnchor().getX());
        assertEquals(201.0, layout.troopAnchor().getY());
        assertEquals(302.5, layout.troopAnchor().getZ());

        assertEquals(100.5, layout.exitPoint().getX());
        assertEquals(200.0, layout.exitPoint().getY());
        assertEquals(296.5, layout.exitPoint().getZ());
    }
}
