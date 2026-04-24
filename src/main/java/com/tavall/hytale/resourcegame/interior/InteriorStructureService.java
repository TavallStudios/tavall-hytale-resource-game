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

    public void clearStructure(World world, InteriorLayout layout) {
        if (world == null || layout == null) {
            return;
        }
        clearMainRoom(world, layout);
        clearWorkerPlatform(world, layout);
    }

    private void clearMainRoom(World world, InteriorLayout layout) {
        int originX = floorToInt(layout.origin().getX());
        int originY = floorToInt(layout.origin().getY());
        int originZ = floorToInt(layout.origin().getZ());
        for (int dx = -HALF_SIZE; dx <= HALF_SIZE; dx++) {
            for (int dz = -HALF_SIZE; dz <= HALF_SIZE; dz++) {
                for (int dy = 0; dy <= WALL_HEIGHT + 1; dy++) {
                    clearBlock(world, originX + dx, originY + dy, originZ + dz);
                }
            }
        }
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

    private void clearWorkerPlatform(World world, InteriorLayout layout) {
        int platformX = floorToInt(layout.workerPlatformAnchor().getX());
        int originY = floorToInt(layout.origin().getY());
        int platformZ = floorToInt(layout.workerPlatformAnchor().getZ());
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -3; dz <= 4; dz++) {
                for (int dy = 0; dy <= 4; dy++) {
                    clearBlock(world, platformX + dx, originY + dy, platformZ + dz);
                }
            }
        }
        int originX = floorToInt(layout.origin().getX());
        int originZ = floorToInt(layout.origin().getZ());
        for (int dz = HALF_SIZE; dz <= 6; dz++) {
            for (int dy = 0; dy <= 1; dy++) {
                clearBlock(world, originX, originY + dy, originZ + dz);
            }
        }
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
