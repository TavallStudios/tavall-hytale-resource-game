package com.tavall.hytale.resourcegame.world;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Writes structure blocks through chunk access so failures are surfaced instead of silently ignored.
 */
public final class StructureBlockPainter {
    private static final Logger LOGGER = Logger.getLogger(StructureBlockPainter.class.getName());

    public boolean placeBlock(World world, int x, int y, int z, String blockKey) {
        if (world == null || blockKey == null || blockKey.isBlank()) {
            return false;
        }
        if (BlockType.getAssetMap().getIndex(blockKey) == Integer.MIN_VALUE) {
            LOGGER.warning(() -> "Skipped structure block write because the block key is unknown: " + blockKey);
            return false;
        }
        try {
            var chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
            if (chunk == null) {
                LOGGER.warning(() -> "Skipped structure block write because the chunk is unavailable at " + x + ", " + y + ", " + z);
                return false;
            }
            boolean changed = chunk.setBlock(x, y, z, blockKey);
            if (!changed) {
                LOGGER.fine(() -> "Structure block write reported no change at " + x + ", " + y + ", " + z + " for " + blockKey);
            }
            return changed;
        } catch (Throwable throwable) {
            LOGGER.log(Level.WARNING, "Failed structure block write for '" + blockKey + "' at " + x + ", " + y + ", " + z + '.', throwable);
            return false;
        }
    }

    public boolean clearBlock(World world, int x, int y, int z) {
        if (world == null) {
            return false;
        }
        try {
            var chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
            if (chunk == null) {
                LOGGER.warning(() -> "Skipped structure block clear because the chunk is unavailable at " + x + ", " + y + ", " + z);
                return false;
            }
            return chunk.breakBlock(x, y, z, 0);
        } catch (Throwable throwable) {
            LOGGER.log(Level.WARNING, "Failed structure block clear at " + x + ", " + y + ", " + z + '.', throwable);
            return false;
        }
    }
}
