package com.tavall.hytale.resourcegame.world;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.tavall.hytale.resourcegame.domain.ResourceNodeData;
import com.tavall.hytale.resourcegame.domain.ResourceNodeSummary;

/**
 * Builds a compact visible pad for a placed resource node.
 */
public final class ResourceNodeStructureService {
    private static final String FLOOR_BLOCK = "Rock_Stone";

    public void ensureNodeSite(World world, ResourceNodeData node, ResourceNodeSummary summary) {
        Vector3d position = new Vector3d(node.x(), node.y(), node.z());
        int originX = floorToInt(position.getX());
        int originY = floorToInt(position.getY()) - 1;
        int originZ = floorToInt(position.getZ());
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                clearBlock(world, originX + dx, originY + 1, originZ + dz);
                clearBlock(world, originX + dx, originY + 2, originZ + dz);
                clearBlock(world, originX + dx, originY + 3, originZ + dz);
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                setBlock(world, originX + dx, originY, originZ + dz, FLOOR_BLOCK);
            }
        }
        applyResourceShape(world, node, originX, originY, originZ);
        applyStockBeacon(world, originX, originY, originZ, summary);
    }

    private void applyResourceShape(World world, ResourceNodeData node, int originX, int originY, int originZ) {
        switch (node.resourceType()) {
            case FOOD -> {
                setBlock(world, originX + 2, originY, originZ, FLOOR_BLOCK);
                setBlock(world, originX - 2, originY, originZ, FLOOR_BLOCK);
            }
            case WOOD -> {
                setBlock(world, originX, originY, originZ + 2, FLOOR_BLOCK);
                setBlock(world, originX, originY, originZ - 2, FLOOR_BLOCK);
            }
            case IRON -> {
                setBlock(world, originX + 2, originY, originZ + 1, FLOOR_BLOCK);
                setBlock(world, originX - 2, originY, originZ + 1, FLOOR_BLOCK);
                setBlock(world, originX + 2, originY, originZ - 1, FLOOR_BLOCK);
                setBlock(world, originX - 2, originY, originZ - 1, FLOOR_BLOCK);
            }
        }
    }

    private void applyStockBeacon(World world, int originX, int originY, int originZ, ResourceNodeSummary summary) {
        int beaconHeight = summary.stockPercent() <= 0
                ? 0
                : summary.stockPercent() < 25
                ? 1
                : summary.stockPercent() < 70
                ? 2
                : 3;
        for (int step = 1; step <= 3; step++) {
            if (step <= beaconHeight) {
                setBlock(world, originX, originY + step, originZ, FLOOR_BLOCK);
            } else {
                clearBlock(world, originX, originY + step, originZ);
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
