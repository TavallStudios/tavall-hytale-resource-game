package com.tavall.hytale.resourcegame.world;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

/**
 * Builds a compact platform under the castle prompt lane so teleports land on stable ground.
 */
public final class CastlePromptLaneStructureService {
    private static final String FLOOR_BLOCK = "Rock_Stone";
    private static final int HALF_WIDTH = 1;
    private static final int HALF_DEPTH = 1;

    public void ensurePromptLane(World world, CastlePromptLaneLayout layout) {
        int originX = floorToInt(layout.origin().getX());
        int originY = floorToInt(layout.origin().getY());
        int originZ = floorToInt(layout.origin().getZ());
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
