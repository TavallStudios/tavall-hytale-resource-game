package com.tavall.hytale.resourcegame.interior;

import com.hypixel.hytale.math.vector.Vector3d;
import com.tavall.hytale.resourcegame.domain.CitizenJobType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

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

        assertEquals(100.5, layout.workerPlatformAnchor().getX());
        assertEquals(201.0, layout.workerPlatformAnchor().getY());
        assertEquals(308.5, layout.workerPlatformAnchor().getZ());
        assertEquals(100.5, layout.workerPortalAnchor().getX());
        assertEquals(201.0, layout.workerPortalAnchor().getY());
        assertEquals(305.3, layout.workerPortalAnchor().getZ(), 0.0001D);
        assertEquals(11, layout.workerAnchors().size());
        assertEquals(96.5, layout.workerAnchors().get(CitizenJobType.IDLE).getX());
        assertEquals(104.5, layout.workerAnchors().get(CitizenJobType.MINER).getX());
        assertEquals(312.0, layout.workerAnchors().get(CitizenJobType.SOLDIER).getZ());

        assertEquals(5, layout.tourStops().size());
        assertEquals("Entry Lane", layout.tourStops().get(0).label());
        assertEquals(100.5, layout.tourStops().get(0).position().getX());
        assertEquals(201.0, layout.tourStops().get(0).position().getY());
        assertEquals(299.0, layout.tourStops().get(0).position().getZ());
        assertEquals("Citizen Anchor", layout.tourStops().get(1).label());
        assertEquals(102.0, layout.tourStops().get(1).position().getX());
        assertEquals(201.0, layout.tourStops().get(1).position().getY());
        assertEquals(301.5, layout.tourStops().get(1).position().getZ());
        assertEquals("Troop Anchor", layout.tourStops().get(2).label());
        assertEquals(99.0, layout.tourStops().get(2).position().getX());
        assertEquals(201.0, layout.tourStops().get(2).position().getY());
        assertEquals(301.5, layout.tourStops().get(2).position().getZ());
        assertEquals("Worker Platform", layout.tourStops().get(3).label());
        assertEquals(100.5, layout.tourStops().get(3).position().getX());
        assertEquals(201.0, layout.tourStops().get(3).position().getY());
        assertEquals(307.5, layout.tourStops().get(3).position().getZ());
        assertEquals("Exit Gate", layout.tourStops().get(4).label());
        assertEquals(100.5, layout.tourStops().get(4).position().getX());
        assertEquals(201.0, layout.tourStops().get(4).position().getY());
        assertEquals(297.5, layout.tourStops().get(4).position().getZ());
    }

    @Test
    void originForUsesSharedSafePrototypeOrigin() {
        InteriorLayoutService service = new InteriorLayoutService();
        UUID firstPlayerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID secondPlayerId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        Vector3d firstOrigin = service.originFor(firstPlayerId);
        Vector3d firstOriginRepeat = service.originFor(firstPlayerId);
        Vector3d secondOrigin = service.originFor(secondPlayerId);

        assertEquals(firstOrigin.getX(), firstOriginRepeat.getX(), 0.0001D);
        assertEquals(firstOrigin.getY(), firstOriginRepeat.getY(), 0.0001D);
        assertEquals(firstOrigin.getZ(), firstOriginRepeat.getZ(), 0.0001D);
        assertEquals(120.0D, firstOrigin.getY(), 0.0001D);
        assertEquals(firstOrigin.getX(), secondOrigin.getX(), 0.0001D);
        assertEquals(firstOrigin.getZ(), secondOrigin.getZ(), 0.0001D);
    }
}
