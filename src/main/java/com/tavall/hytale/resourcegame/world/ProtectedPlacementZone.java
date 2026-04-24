package com.tavall.hytale.resourcegame.world;

import com.hypixel.hytale.math.vector.Vector3i;

import java.util.Objects;

/**
 * Circular horizontal placement exclusion zone around a gameplay structure.
 */
public final class ProtectedPlacementZone {
    private final String structureKey;
    private final ProtectedStructureType structureType;
    private final String worldName;
    private final int centerX;
    private final int centerY;
    private final int centerZ;
    private final int radiusBlocks;

    public ProtectedPlacementZone(
            String structureKey,
            ProtectedStructureType structureType,
            String worldName,
            int centerX,
            int centerY,
            int centerZ,
            int radiusBlocks
    ) {
        this.structureKey = Objects.requireNonNull(structureKey, "structureKey");
        this.structureType = Objects.requireNonNull(structureType, "structureType");
        this.worldName = Objects.requireNonNull(worldName, "worldName");
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radiusBlocks = Math.max(0, radiusBlocks);
    }

    public String structureKey() {
        return structureKey;
    }

    public ProtectedStructureType structureType() {
        return structureType;
    }

    public String worldName() {
        return worldName;
    }

    public int centerX() {
        return centerX;
    }

    public int centerY() {
        return centerY;
    }

    public int centerZ() {
        return centerZ;
    }

    public int radiusBlocks() {
        return radiusBlocks;
    }

    public boolean contains(Vector3i blockPosition) {
        if (blockPosition == null || radiusBlocks <= 0) {
            return false;
        }
        long dx = (long) blockPosition.getX() - (long) centerX;
        long dz = (long) blockPosition.getZ() - (long) centerZ;
        long distanceSquared = (dx * dx) + (dz * dz);
        long radiusSquared = (long) radiusBlocks * (long) radiusBlocks;
        return distanceSquared <= radiusSquared;
    }
}

