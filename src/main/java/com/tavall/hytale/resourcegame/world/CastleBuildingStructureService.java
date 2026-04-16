package com.tavall.hytale.resourcegame.world;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.tavall.hytale.resourcegame.domain.BuildingConstructionStage;
import com.tavall.hytale.resourcegame.domain.BuildingType;
import com.tavall.hytale.resourcegame.domain.CastleBuildingSummary;

/**
 * Builds simple staged block silhouettes for upgradeable buildings.
 */
public final class CastleBuildingStructureService {
    private static final String BLOCK_KEY = "Rock_Stone";

    public void ensureBuildingSite(World world, CastleBuildingSummary summary) {
        int originX = floor(summary.worldX());
        int originY = floor(summary.worldY()) - 1;
        int originZ = floor(summary.worldZ());
        clearVolume(world, originX, originY, originZ);
        paintPad(world, originX, originY, originZ, 2);
        if (summary.isUnderConstruction()) {
            applyConstructionShape(world, summary.buildingData().buildingType(), originX, originY, originZ, summary.constructionStage());
            return;
        }
        applyCompletedShape(world, summary.buildingData().buildingType(), originX, originY, originZ, summary.completedLevel());
    }

    public void clearBuildingSite(World world, Vector3d worldPosition) {
        int originX = floor(worldPosition.getX());
        int originY = floor(worldPosition.getY()) - 1;
        int originZ = floor(worldPosition.getZ());
        clearVolume(world, originX, originY, originZ);
    }

    private void applyConstructionShape(World world, BuildingType buildingType, int originX, int originY, int originZ, BuildingConstructionStage stage) {
        switch (stage) {
            case FOUNDATION -> paintPad(world, originX, originY, originZ, 1);
            case SCAFFOLDING -> {
                paintPad(world, originX, originY, originZ, 2);
                paintPosts(world, originX, originY, originZ, 2, 2);
            }
            case SHELL -> {
                paintPad(world, originX, originY, originZ, 2);
                paintPosts(world, originX, originY, originZ, 2, 3);
                paintRoofRim(world, originX, originY + 3, originZ, 2);
                if (buildingType == BuildingType.IRON_WORKS) {
                    paintColumn(world, originX, originY + 1, originZ, 4);
                }
            }
            case COMPLETE -> applyCompletedShape(world, buildingType, originX, originY, originZ, 1);
        }
    }

    private void applyCompletedShape(World world, BuildingType buildingType, int originX, int originY, int originZ, int level) {
        switch (buildingType) {
            case FARMSTEAD -> paintBarn(world, originX, originY, originZ, level);
            case LUMBER_MILL -> paintMill(world, originX, originY, originZ, level);
            case IRON_WORKS -> paintForge(world, originX, originY, originZ, level);
            case BARRACKS -> paintBarracks(world, originX, originY, originZ, level);
            case WORKSHOP -> paintWorkshop(world, originX, originY, originZ, level);
        }
    }

    private void paintBarn(World world, int originX, int originY, int originZ, int level) {
        int radius = Math.min(3, 1 + level);
        paintPad(world, originX, originY, originZ, radius);
        fillRect(world, originX - radius, originY + 1, originZ - 1, originX + radius, originY + level, originZ + 1);
        paintRoofRim(world, originX, originY + level + 1, originZ, radius);
    }

    private void paintMill(World world, int originX, int originY, int originZ, int level) {
        paintPad(world, originX, originY, originZ, 2);
        fillRect(world, originX - 2, originY + 1, originZ - 1, originX + 2, originY + Math.max(1, level), originZ + 1);
        fillRect(world, originX - 1, originY + 1, originZ - (2 + level), originX + 1, originY + 1, originZ + (2 + level));
    }

    private void paintForge(World world, int originX, int originY, int originZ, int level) {
        paintPad(world, originX, originY, originZ, 2);
        fillRect(world, originX - 1, originY + 1, originZ - 1, originX + 1, originY + (1 + level), originZ + 1);
        paintColumn(world, originX + 2, originY + 1, originZ, 2 + level);
    }

    private void paintBarracks(World world, int originX, int originY, int originZ, int level) {
        paintPad(world, originX, originY, originZ, 2);
        fillRect(world, originX - 2, originY + 1, originZ - 2, originX + 2, originY + Math.max(1, level), originZ + 2);
        clearDoor(world, originX, originY + 1, originZ - 2, Math.max(2, level));
    }

    private void paintWorkshop(World world, int originX, int originY, int originZ, int level) {
        paintPad(world, originX, originY, originZ, 2);
        fillRect(world, originX - 1, originY + 1, originZ - 1, originX + 1, originY + level, originZ + 1);
        paintColumn(world, originX - 2, originY + 1, originZ, 1 + level);
        paintColumn(world, originX + 2, originY + 1, originZ, 1 + level);
    }

    private void paintPad(World world, int originX, int originY, int originZ, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                setBlock(world, originX + dx, originY, originZ + dz, BLOCK_KEY);
                clearBlock(world, originX + dx, originY + 1, originZ + dz);
                clearBlock(world, originX + dx, originY + 2, originZ + dz);
                clearBlock(world, originX + dx, originY + 3, originZ + dz);
                clearBlock(world, originX + dx, originY + 4, originZ + dz);
                clearBlock(world, originX + dx, originY + 5, originZ + dz);
            }
        }
    }

    private void paintPosts(World world, int originX, int originY, int originZ, int radius, int height) {
        paintColumn(world, originX - radius, originY + 1, originZ - radius, height);
        paintColumn(world, originX - radius, originY + 1, originZ + radius, height);
        paintColumn(world, originX + radius, originY + 1, originZ - radius, height);
        paintColumn(world, originX + radius, originY + 1, originZ + radius, height);
    }

    private void paintRoofRim(World world, int originX, int originY, int originZ, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            setBlock(world, originX + dx, originY, originZ - radius, BLOCK_KEY);
            setBlock(world, originX + dx, originY, originZ + radius, BLOCK_KEY);
        }
        for (int dz = -radius; dz <= radius; dz++) {
            setBlock(world, originX - radius, originY, originZ + dz, BLOCK_KEY);
            setBlock(world, originX + radius, originY, originZ + dz, BLOCK_KEY);
        }
    }

    private void paintColumn(World world, int x, int y, int z, int height) {
        for (int step = 0; step < height; step++) {
            setBlock(world, x, y + step, z, BLOCK_KEY);
        }
    }

    private void fillRect(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    setBlock(world, x, y, z, BLOCK_KEY);
                }
            }
        }
    }

    private void clearDoor(World world, int x, int y, int z, int height) {
        for (int step = 0; step < height; step++) {
            clearBlock(world, x, y + step, z);
        }
    }

    private void clearVolume(World world, int originX, int originY, int originZ) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int dy = 0; dy <= 6; dy++) {
                    clearBlock(world, originX + dx, originY + dy, originZ + dz);
                }
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

    private int floor(double value) {
        return (int) Math.floor(value);
    }
}
