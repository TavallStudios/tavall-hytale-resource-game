package com.tavall.hytale.resourcegame.world;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.tavall.hytale.resourcegame.config.CastleAssetConfig;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Builds the placeholder castle column used for first-pass castle visuals.
 */
public final class CastleSiteStructureService {
    private static final String FLOOR_BLOCK = "Rock_Stone";
    private static final int CLEAR_RADIUS = 2;
    private final CastleAssetConfig castleAssetConfig;
    private final StructureBlockPainter blockPainter = new StructureBlockPainter();

    public CastleSiteStructureService(CastleAssetConfig castleAssetConfig) {
        this.castleAssetConfig = castleAssetConfig;
    }

    public Set<Vector3i> ensureSite(World world, CastleSiteLayout layout) {
        clearSite(world, layout);
        return paintCastleMarker(world, layout.origin());
    }

    public void clearSite(World world, CastleSiteLayout layout) {
        int originX = floorToInt(layout.origin().getX());
        int originY = floorToInt(layout.origin().getY()) - 1;
        int originZ = floorToInt(layout.origin().getZ());
        for (int dx = -CLEAR_RADIUS; dx <= CLEAR_RADIUS; dx++) {
            for (int dz = -CLEAR_RADIUS; dz <= CLEAR_RADIUS; dz++) {
                for (int dy = 0; dy <= 5; dy++) {
                    clearBlock(world, originX + dx, originY + dy, originZ + dz);
                }
            }
        }
    }

    private Set<Vector3i> paintCastleMarker(World world, Vector3d center) {
        int originX = floorToInt(center.getX());
        int originY = floorToInt(center.getY()) - 1;
        int originZ = floorToInt(center.getZ());
        Set<Vector3i> placedBlocks = new LinkedHashSet<>();
        recordBlock(world, originX, originY, originZ, FLOOR_BLOCK, placedBlocks);
        recordBlock(world, originX, originY + 1, originZ, castleAssetConfig.castleBlockType(), placedBlocks);
        recordBlock(world, originX, originY + 2, originZ, castleAssetConfig.castleBlockType(), placedBlocks);
        recordBlock(world, originX, originY + 3, originZ, castleAssetConfig.castleBlockType(), placedBlocks);
        clearBlock(world, originX, originY + 4, originZ);
        return Set.copyOf(placedBlocks);
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
