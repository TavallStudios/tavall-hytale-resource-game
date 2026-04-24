package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.math.vector.Vector3i;
import com.tavall.hytale.resourcegame.world.ProtectedStructureType;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class StructureProtectionServiceTest {
    @Test
    void replaceStructureIndexesBlocksWithOwnerAndAssetMetadata() {
        StructureProtectionService service = new StructureProtectionService();
        UUID ownerId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        Vector3i first = new Vector3i(4, 64, 9);
        Vector3i second = new Vector3i(5, 65, 9);

        service.replaceStructure(
                "castle:test",
                ownerId,
                ProtectedStructureType.CASTLE,
                "surface-world",
                "stone_column_castle",
                Set.of(first, second)
        );

        assertTrue(service.isProtected("surface-world", first));
        assertTrue(service.isProtected("surface-world", second));

        var metadata = service.metadata("surface-world", first).orElseThrow();
        assertEquals(ownerId, metadata.ownerId());
        assertEquals("castle:test", metadata.structureKey());
        assertEquals(ProtectedStructureType.CASTLE, metadata.structureType());
        assertEquals("stone_column_castle", metadata.assetType());
    }

    @Test
    void clearStructureRemovesEveryIndexedBlock() {
        StructureProtectionService service = new StructureProtectionService();
        UUID ownerId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        Vector3i protectedBlock = new Vector3i(12, 70, -4);

        service.replaceStructure(
                "building:test",
                ownerId,
                ProtectedStructureType.BUILDING,
                "interior-world",
                "farmstead",
                Set.of(protectedBlock)
        );
        assertTrue(service.isProtected("interior-world", protectedBlock));

        service.replacePlacementZone(
                "building:test",
                ProtectedStructureType.BUILDING,
                "interior-world",
                protectedBlock,
                6
        );
        assertTrue(service.isPlacementRestricted("interior-world", new Vector3i(14, 70, -4)));

        service.clearStructure("building:test");

        assertFalse(service.isProtected("interior-world", protectedBlock));
        assertFalse(service.isPlacementRestricted("interior-world", new Vector3i(14, 70, -4)));
    }

    @Test
    void placementZonesRejectBlockPlacementWithinRadius() {
        StructureProtectionService service = new StructureProtectionService();
        Vector3i center = new Vector3i(100, 64, 200);
        service.replacePlacementZone(
                "castle:test",
                ProtectedStructureType.CASTLE,
                "surface-world",
                center,
                10
        );

        assertTrue(service.isPlacementRestricted("surface-world", new Vector3i(109, 64, 200)));
        assertTrue(service.isPlacementRestricted("surface-world", new Vector3i(100, 999, 191)));
        assertFalse(service.isPlacementRestricted("surface-world", new Vector3i(111, 64, 200)));
        assertFalse(service.isPlacementRestricted("other-world", new Vector3i(109, 64, 200)));
    }
}
