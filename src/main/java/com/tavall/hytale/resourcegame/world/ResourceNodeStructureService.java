package com.tavall.hytale.resourcegame.world;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.tavall.hytale.resourcegame.domain.ResourceNodeData;
import com.tavall.hytale.resourcegame.domain.ResourceNodeSummary;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Builds a compact visible pad for a placed resource node.
 */
public final class ResourceNodeStructureService {
    private static final String FLOOR_BLOCK = "Rock_Stone";
    private static final String FOOD_BLOCK = "Soil_Grass";
    private static final String WOOD_BLOCK = "Soil_Needles";
    private static final String IRON_BLOCK = "Rock_Quartzite";
    private final StructureBlockPainter blockPainter = new StructureBlockPainter();

    public Set<Vector3i> ensureNodeSite(World world, ResourceNodeData node, ResourceNodeSummary summary) {
        Vector3d position = new Vector3d(node.x(), node.y(), node.z());
        int originX = floorToInt(position.getX());
        int originY = floorToInt(position.getY()) - 1;
        int originZ = floorToInt(position.getZ());
        clearNodeSite(world, new Vector3d(node.x(), node.y(), node.z()));
        Set<Vector3i> placedBlocks = new LinkedHashSet<>();
        recordBlock(world, originX, originY, originZ, FLOOR_BLOCK, placedBlocks);
        int columnHeight = summary.stockPercent() >= 70 ? 3 : 2;
        String resourceBlock = resourceBlock(node);
        for (int offset = 1; offset <= columnHeight; offset++) {
            recordBlock(world, originX, originY + offset, originZ, resourceBlock, placedBlocks);
        }
        return Set.copyOf(placedBlocks);
    }

    public void clearNodeSite(World world, Vector3d worldPosition) {
        int originX = floorToInt(worldPosition.getX());
        int originY = floorToInt(worldPosition.getY()) - 1;
        int originZ = floorToInt(worldPosition.getZ());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 4; dy++) {
                    clearBlock(world, originX + dx, originY + dy, originZ + dz);
                }
            }
        }
    }

    private String resourceBlock(ResourceNodeData node) {
        return switch (node.resourceType()) {
            case FOOD -> FOOD_BLOCK;
            case WOOD -> WOOD_BLOCK;
            case IRON -> IRON_BLOCK;
        };
    }

    private void setBlock(World world, int x, int y, int z, String blockKey) {
        blockPainter.placeBlock(world, x, y, z, blockKey);
    }

    private void clearBlock(World world, int x, int y, int z) {
        blockPainter.clearBlock(world, x, y, z);
    }

    private void recordBlock(World world, int x, int y, int z, String blockKey, Set<Vector3i> placedBlocks) {
        setBlock(world, x, y, z, blockKey);
        placedBlocks.add(new Vector3i(x, y, z));
    }

    private int floorToInt(double value) {
        return (int) Math.floor(value);
    }
}
