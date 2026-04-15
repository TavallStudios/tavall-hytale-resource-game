package com.tavall.hytale.resourcegame.world;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

import java.util.List;

/**
 * Builds low-risk placeholder pads around the castle so the world scene reads cleanly.
 */
public final class CastleSiteStructureService {
    private static final String FLOOR_BLOCK = "Rock_Stone";

    public void ensureSite(World world, CastleSiteLayout layout) {
        paintPad(world, layout.origin(), 2);
        paintPad(world, layout.stockpileAnchor(), 2);
        paintPad(world, layout.citizenAnchor(), 1);
        paintPad(world, layout.troopAnchor(), 1);
        paintPad(world, layout.foodNodeAnchor(), 1);
        paintPad(world, layout.woodNodeAnchor(), 1);
        paintPad(world, layout.ironNodeAnchor(), 1);
        paintLane(world, layout.origin(), layout.stockpileAnchor());
        paintLane(world, layout.stockpileAnchor(), layout.foodNodeAnchor());
        paintLane(world, layout.stockpileAnchor(), layout.woodNodeAnchor());
        paintLane(world, layout.stockpileAnchor(), layout.ironNodeAnchor());
        clearAnchorColumns(world, List.of(
                layout.origin(),
                layout.stockpileAnchor(),
                layout.citizenAnchor(),
                layout.troopAnchor(),
                layout.foodNodeAnchor(),
                layout.woodNodeAnchor(),
                layout.ironNodeAnchor()
        ));
    }

    private void paintPad(World world, Vector3d center, int radius) {
        int originX = floorToInt(center.getX());
        int originY = floorToInt(center.getY()) - 1;
        int originZ = floorToInt(center.getZ());
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                setBlock(world, originX + dx, originY, originZ + dz, FLOOR_BLOCK);
                clearBlock(world, originX + dx, originY + 1, originZ + dz);
                clearBlock(world, originX + dx, originY + 2, originZ + dz);
                clearBlock(world, originX + dx, originY + 3, originZ + dz);
            }
        }
    }

    private void paintLane(World world, Vector3d start, Vector3d end) {
        int startX = floorToInt(start.getX());
        int startZ = floorToInt(start.getZ());
        int endX = floorToInt(end.getX());
        int endZ = floorToInt(end.getZ());
        int y = floorToInt(Math.min(start.getY(), end.getY())) - 1;
        int steps = Math.max(Math.abs(endX - startX), Math.abs(endZ - startZ));
        if (steps <= 0) {
            return;
        }
        for (int step = 0; step <= steps; step++) {
            double progress = step / (double) steps;
            int x = floorToInt(startX + ((endX - startX) * progress));
            int z = floorToInt(startZ + ((endZ - startZ) * progress));
            setBlock(world, x, y, z, FLOOR_BLOCK);
            clearBlock(world, x, y + 1, z);
            clearBlock(world, x, y + 2, z);
        }
    }

    private void clearAnchorColumns(World world, List<Vector3d> positions) {
        for (Vector3d position : positions) {
            int x = floorToInt(position.getX());
            int y = floorToInt(position.getY());
            int z = floorToInt(position.getZ());
            clearBlock(world, x, y, z);
            clearBlock(world, x, y + 1, z);
            clearBlock(world, x, y + 2, z);
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
