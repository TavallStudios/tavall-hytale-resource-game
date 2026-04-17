package com.tavall.hytale.resourcegame.interior;

import com.hypixel.hytale.server.core.universe.world.World;

/**
 * Builds a simple same-process placeholder interior platform that is safe to teleport onto.
 */
public final class InteriorStructureService {
    private static final String FLOOR_BLOCK = "Rock_Stone";
    private static final String PORTAL_BLOCK = "Metal_Iron";
    private static final int HALF_SIZE = 4;
    private static final int WALL_HEIGHT = 3;
    private final com.tavall.hytale.resourcegame.world.StructureBlockPainter blockPainter
            = new com.tavall.hytale.resourcegame.world.StructureBlockPainter();

    public void ensureStructure(World world, InteriorLayout layout) {
        int originX = floorToInt(layout.origin().getX());
        int originY = floorToInt(layout.origin().getY());
        int originZ = floorToInt(layout.origin().getZ());
        for (int dx = -HALF_SIZE; dx <= HALF_SIZE; dx++) {
            for (int dz = -HALF_SIZE; dz <= HALF_SIZE; dz++) {
                setBlock(world, originX + dx, originY, originZ + dz, FLOOR_BLOCK);
                clearBlock(world, originX + dx, originY + 1, originZ + dz);
                clearBlock(world, originX + dx, originY + 2, originZ + dz);
                clearBlock(world, originX + dx, originY + 3, originZ + dz);
                if (Math.abs(dx) == HALF_SIZE || Math.abs(dz) == HALF_SIZE) {
                    setBlock(world, originX + dx, originY + 1, originZ + dz, FLOOR_BLOCK);
                    setBlock(world, originX + dx, originY + 2, originZ + dz, FLOOR_BLOCK);
                }
            }
        }
        clearBlock(world, originX, originY + 1, originZ - HALF_SIZE);
        clearBlock(world, originX, originY + 2, originZ - HALF_SIZE);
        clearBlock(world, originX, originY + WALL_HEIGHT, originZ - HALF_SIZE);
        clearBlock(world, originX, originY + 1, originZ + HALF_SIZE);
        clearBlock(world, originX, originY + 2, originZ + HALF_SIZE);
        clearBlock(world, originX, originY + WALL_HEIGHT, originZ + HALF_SIZE);
        ensureWorkerPlatform(world, layout);
    }

    private void ensureWorkerPlatform(World world, InteriorLayout layout) {
        int originX = floorToInt(layout.workerPlatformAnchor().getX());
        int originY = floorToInt(layout.origin().getY());
        int originZ = floorToInt(layout.workerPlatformAnchor().getZ());
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -3; dz <= 4; dz++) {
                setBlock(world, originX + dx, originY, originZ + dz, FLOOR_BLOCK);
                clearBlock(world, originX + dx, originY + 1, originZ + dz);
                clearBlock(world, originX + dx, originY + 2, originZ + dz);
                clearBlock(world, originX + dx, originY + 3, originZ + dz);
            }
        }
        for (int dz = HALF_SIZE; dz <= 6; dz++) {
            setBlock(world, floorToInt(layout.origin().getX()), originY, floorToInt(layout.origin().getZ()) + dz, FLOOR_BLOCK);
        }
        ensurePortal(world, layout);
    }

    private void ensurePortal(World world, InteriorLayout layout) {
        int portalX = floorToInt(layout.workerPortalAnchor().getX());
        int portalY = floorToInt(layout.origin().getY());
        int portalZ = floorToInt(layout.workerPortalAnchor().getZ());
        setBlock(world, portalX - 1, portalY + 1, portalZ, PORTAL_BLOCK);
        setBlock(world, portalX + 1, portalY + 1, portalZ, PORTAL_BLOCK);
        setBlock(world, portalX - 1, portalY + 2, portalZ, PORTAL_BLOCK);
        setBlock(world, portalX + 1, portalY + 2, portalZ, PORTAL_BLOCK);
        setBlock(world, portalX, portalY + 3, portalZ, PORTAL_BLOCK);
    }

    private void setBlock(World world, int x, int y, int z, String blockKey) {
        blockPainter.placeBlock(world, x, y, z, blockKey);
    }

    private void clearBlock(World world, int x, int y, int z) {
        blockPainter.clearBlock(world, x, y, z);
    }

    private int floorToInt(double value) {
        return (int) Math.floor(value);
    }
}
