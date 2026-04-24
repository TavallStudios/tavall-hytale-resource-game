package com.tavall.hytale.resourcegame.world;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.tavall.hytale.resourcegame.domain.BuildingConstructionStage;
import com.tavall.hytale.resourcegame.domain.BuildingType;
import com.tavall.hytale.resourcegame.domain.CastleBuildingSummary;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Builds block-only staged silhouettes for upgradeable buildings.
 */
public final class CastleBuildingStructureService {
    private static final String STONE_BLOCK = "Rock_Stone";
    private static final String BRICK_BLOCK = "Rock_Stone_Brick";
    private static final String WOOD_BLOCK = "Rock_Shale";
    private static final String METAL_BLOCK = "Rock_Quartzite";
    private final StructureBlockPainter blockPainter = new StructureBlockPainter();

    public Set<Vector3i> ensureBuildingSite(World world, CastleBuildingSummary summary) {
        int originX = floor(summary.worldX());
        int originY = floor(summary.worldY()) - 1;
        int originZ = floor(summary.worldZ());
        clearVolume(world, originX, originY, originZ);
        Set<Vector3i> placedBlocks = new LinkedHashSet<>();
        paintPad(world, originX, originY, originZ, 2, foundationBlock(summary.buildingData().buildingType()), placedBlocks);
        if (summary.isUnderConstruction()) {
            applyConstructionShape(world, summary.buildingData().buildingType(), originX, originY, originZ, summary.constructionStage(), placedBlocks);
            return Set.copyOf(placedBlocks);
        }
        applyCompletedShape(world, summary.buildingData().buildingType(), originX, originY, originZ, summary.completedLevel(), placedBlocks);
        return Set.copyOf(placedBlocks);
    }

    public void clearBuildingSite(World world, Vector3d worldPosition) {
        int originX = floor(worldPosition.getX());
        int originY = floor(worldPosition.getY()) - 1;
        int originZ = floor(worldPosition.getZ());
        clearVolume(world, originX, originY, originZ);
    }

    private void applyConstructionShape(
            World world,
            BuildingType buildingType,
            int originX,
            int originY,
            int originZ,
            BuildingConstructionStage stage,
            Set<Vector3i> placedBlocks
    ) {
        String foundationBlock = foundationBlock(buildingType);
        String scaffoldBlock = scaffoldBlock(buildingType);
        String wallBlock = wallBlock(buildingType);
        switch (stage) {
            case FOUNDATION -> paintPad(world, originX, originY, originZ, 1, foundationBlock, placedBlocks);
            case SCAFFOLDING -> {
                paintPad(world, originX, originY, originZ, 2, foundationBlock, placedBlocks);
                paintPosts(world, originX, originY, originZ, 2, 2, scaffoldBlock, placedBlocks);
            }
            case SHELL -> {
                paintPad(world, originX, originY, originZ, 2, foundationBlock, placedBlocks);
                paintPosts(world, originX, originY, originZ, 2, 3, scaffoldBlock, placedBlocks);
                paintRoofRim(world, originX, originY + 3, originZ, 2, wallBlock, placedBlocks);
                if (buildingType == BuildingType.IRON_WORKS) {
                    paintColumn(world, originX, originY + 1, originZ, 4, METAL_BLOCK, placedBlocks);
                }
            }
            case COMPLETE -> applyCompletedShape(world, buildingType, originX, originY, originZ, 1, placedBlocks);
        }
    }

    private void applyCompletedShape(World world, BuildingType buildingType, int originX, int originY, int originZ, int level, Set<Vector3i> placedBlocks) {
        int visualLevel = visualLevel(buildingType, level);
        switch (buildingType) {
            case FARMSTEAD -> paintFarmstead(world, originX, originY, originZ, visualLevel, placedBlocks);
            case LUMBER_MILL -> paintLumberMill(world, originX, originY, originZ, visualLevel, placedBlocks);
            case IRON_WORKS -> paintIronWorks(world, originX, originY, originZ, visualLevel, placedBlocks);
            case BARRACKS -> paintBarracks(world, originX, originY, originZ, visualLevel, placedBlocks);
            case WORKSHOP -> paintWorkshop(world, originX, originY, originZ, visualLevel, placedBlocks);
        }
    }

    private int visualLevel(BuildingType buildingType, int level) {
        int safeLevel = Math.max(1, level);
        if (buildingType == null) {
            return Math.min(3, safeLevel);
        }
        if (buildingType.areaType() == com.tavall.hytale.resourcegame.domain.BuildingAreaType.CASTLE_SURFACE) {
            return Math.min(3, safeLevel);
        }
        return Math.min(4, safeLevel);
    }

    private void paintFarmstead(World world, int originX, int originY, int originZ, int level, Set<Vector3i> placedBlocks) {
        int radius = Math.min(3, 1 + level);
        paintPad(world, originX, originY, originZ, radius, WOOD_BLOCK, placedBlocks);
        fillRect(world, originX - radius, originY + 1, originZ - 1, originX + radius, originY + level, originZ + 1, WOOD_BLOCK, placedBlocks);
        paintRoofRim(world, originX, originY + level + 1, originZ, radius, BRICK_BLOCK, placedBlocks);
    }

    private void paintLumberMill(World world, int originX, int originY, int originZ, int level, Set<Vector3i> placedBlocks) {
        paintPad(world, originX, originY, originZ, 2, WOOD_BLOCK, placedBlocks);
        fillRect(world, originX - 2, originY + 1, originZ - 1, originX + 2, originY + Math.max(1, level), originZ + 1, WOOD_BLOCK, placedBlocks);
        fillRect(world, originX - 1, originY + 1, originZ - (2 + level), originX + 1, originY + 1, originZ + (2 + level), WOOD_BLOCK, placedBlocks);
        paintColumn(world, originX + 2, originY + 1, originZ, 1 + level, BRICK_BLOCK, placedBlocks);
    }

    private void paintIronWorks(World world, int originX, int originY, int originZ, int level, Set<Vector3i> placedBlocks) {
        paintPad(world, originX, originY, originZ, 2, BRICK_BLOCK, placedBlocks);
        fillRect(world, originX - 1, originY + 1, originZ - 1, originX + 1, originY + (1 + level), originZ + 1, METAL_BLOCK, placedBlocks);
        paintColumn(world, originX + 2, originY + 1, originZ, 2 + level, BRICK_BLOCK, placedBlocks);
        paintColumn(world, originX - 2, originY + 1, originZ, 1 + level, METAL_BLOCK, placedBlocks);
    }

    private void paintBarracks(World world, int originX, int originY, int originZ, int level, Set<Vector3i> placedBlocks) {
        paintPad(world, originX, originY, originZ, 2, BRICK_BLOCK, placedBlocks);
        fillRect(world, originX - 2, originY + 1, originZ - 2, originX + 2, originY + Math.max(1, level), originZ + 2, BRICK_BLOCK, placedBlocks);
        clearDoor(world, originX, originY + 1, originZ - 2, Math.max(2, level));
        paintRoofRim(world, originX, originY + Math.max(1, level) + 1, originZ, 2, STONE_BLOCK, placedBlocks);
    }

    private void paintWorkshop(World world, int originX, int originY, int originZ, int level, Set<Vector3i> placedBlocks) {
        paintPad(world, originX, originY, originZ, 2, WOOD_BLOCK, placedBlocks);
        fillRect(world, originX - 1, originY + 1, originZ - 1, originX + 1, originY + level, originZ + 1, WOOD_BLOCK, placedBlocks);
        paintColumn(world, originX - 2, originY + 1, originZ, 1 + level, BRICK_BLOCK, placedBlocks);
        paintColumn(world, originX + 2, originY + 1, originZ, 1 + level, BRICK_BLOCK, placedBlocks);
        paintRoofRim(world, originX, originY + level + 1, originZ, 1, METAL_BLOCK, placedBlocks);
    }

    private String foundationBlock(BuildingType buildingType) {
        return switch (buildingType) {
            case FARMSTEAD, LUMBER_MILL, WORKSHOP -> WOOD_BLOCK;
            case IRON_WORKS, BARRACKS -> BRICK_BLOCK;
        };
    }

    private String scaffoldBlock(BuildingType buildingType) {
        return switch (buildingType) {
            case IRON_WORKS -> METAL_BLOCK;
            case BARRACKS -> BRICK_BLOCK;
            case FARMSTEAD, LUMBER_MILL, WORKSHOP -> WOOD_BLOCK;
        };
    }

    private String wallBlock(BuildingType buildingType) {
        return switch (buildingType) {
            case FARMSTEAD, LUMBER_MILL, WORKSHOP -> WOOD_BLOCK;
            case IRON_WORKS -> METAL_BLOCK;
            case BARRACKS -> BRICK_BLOCK;
        };
    }

    private void paintPad(World world, int originX, int originY, int originZ, int radius, String blockKey, Set<Vector3i> placedBlocks) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                recordBlock(world, originX + dx, originY, originZ + dz, blockKey, placedBlocks);
                clearBlock(world, originX + dx, originY + 1, originZ + dz);
                clearBlock(world, originX + dx, originY + 2, originZ + dz);
                clearBlock(world, originX + dx, originY + 3, originZ + dz);
                clearBlock(world, originX + dx, originY + 4, originZ + dz);
                clearBlock(world, originX + dx, originY + 5, originZ + dz);
            }
        }
    }

    private void paintPosts(World world, int originX, int originY, int originZ, int radius, int height, String blockKey, Set<Vector3i> placedBlocks) {
        paintColumn(world, originX - radius, originY + 1, originZ - radius, height, blockKey, placedBlocks);
        paintColumn(world, originX - radius, originY + 1, originZ + radius, height, blockKey, placedBlocks);
        paintColumn(world, originX + radius, originY + 1, originZ - radius, height, blockKey, placedBlocks);
        paintColumn(world, originX + radius, originY + 1, originZ + radius, height, blockKey, placedBlocks);
    }

    private void paintRoofRim(World world, int originX, int originY, int originZ, int radius, String blockKey, Set<Vector3i> placedBlocks) {
        for (int dx = -radius; dx <= radius; dx++) {
            recordBlock(world, originX + dx, originY, originZ - radius, blockKey, placedBlocks);
            recordBlock(world, originX + dx, originY, originZ + radius, blockKey, placedBlocks);
        }
        for (int dz = -radius; dz <= radius; dz++) {
            recordBlock(world, originX - radius, originY, originZ + dz, blockKey, placedBlocks);
            recordBlock(world, originX + radius, originY, originZ + dz, blockKey, placedBlocks);
        }
    }

    private void paintColumn(World world, int x, int y, int z, int height, String blockKey, Set<Vector3i> placedBlocks) {
        for (int step = 0; step < height; step++) {
            recordBlock(world, x, y + step, z, blockKey, placedBlocks);
        }
    }

    private void fillRect(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, String blockKey, Set<Vector3i> placedBlocks) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    recordBlock(world, x, y, z, blockKey, placedBlocks);
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
        blockPainter.placeBlock(world, x, y, z, blockKey);
    }

    private void clearBlock(World world, int x, int y, int z) {
        blockPainter.clearBlock(world, x, y, z);
    }

    private void recordBlock(World world, int x, int y, int z, String blockKey, Set<Vector3i> placedBlocks) {
        setBlock(world, x, y, z, blockKey);
        placedBlocks.add(new Vector3i(x, y, z));
    }

    private int floor(double value) {
        return (int) Math.floor(value);
    }
}
