package com.tavall.hytale.resourcegame.interior;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

/**
 * Builds a simple same-process placeholder interior platform that is safe to teleport onto.
 */
public final class InteriorStructureService {
    private static final String FLOOR_BLOCK = "Rock_Stone";
    private static final int HALF_SIZE = 4;
    private static final int WALL_HEIGHT = 3;

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
    }

    private void setBlock(World world, int x, int y, int z, String blockKey) {
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk != null) {
            chunk.setBlock(x, y, z, blockKey);
        }
    }

    private void clearBlock(World world, int x, int y, int z) {
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk != null) {
            chunk.setBlock(x, y, z, 0);
        }
    }

    private int floorToInt(double value) {
        return (int) Math.floor(value);
    }
}
