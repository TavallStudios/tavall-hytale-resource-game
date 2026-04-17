package com.tavall.hytale.resourcegame.world;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;

/**
 * Builds a compact temporary pad so building-stage teleports always land on stable ground.
 */
public final class BuildingPlacementStageStructureService {
    private static final String FLOOR_BLOCK = "Rock_Stone";
    private static final int HALF_WIDTH = 1;
    private static final int HALF_DEPTH = 1;
    private final StructureBlockPainter blockPainter = new StructureBlockPainter();

    public void ensureStagePad(World world, Vector3d buildAnchor) {
        if (world == null || buildAnchor == null) {
            return;
        }
        int originX = floorToInt(buildAnchor.getX());
        int originY = floorToInt(buildAnchor.getY()) - 1;
        int originZ = floorToInt(buildAnchor.getZ());
        for (int dx = -HALF_WIDTH; dx <= HALF_WIDTH; dx++) {
            for (int dz = -HALF_DEPTH; dz <= HALF_DEPTH; dz++) {
                setBlock(world, originX + dx, originY, originZ + dz, FLOOR_BLOCK);
                clearBlock(world, originX + dx, originY + 1, originZ + dz);
                clearBlock(world, originX + dx, originY + 2, originZ + dz);
                clearBlock(world, originX + dx, originY + 3, originZ + dz);
            }
        }
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
