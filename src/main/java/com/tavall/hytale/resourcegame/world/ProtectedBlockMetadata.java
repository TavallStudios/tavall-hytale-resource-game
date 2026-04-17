package com.tavall.hytale.resourcegame.world;

import java.util.Objects;
import java.util.UUID;

/**
 * Runtime metadata for a block claimed by a castle, building, or resource node.
 */
public final class ProtectedBlockMetadata {
    private final UUID ownerId;
    private final String structureKey;
    private final ProtectedStructureType structureType;
    private final String assetType;

    public ProtectedBlockMetadata(
            UUID ownerId,
            String structureKey,
            ProtectedStructureType structureType,
            String assetType
    ) {
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.structureKey = Objects.requireNonNull(structureKey, "structureKey");
        this.structureType = Objects.requireNonNull(structureType, "structureType");
        this.assetType = assetType == null ? "" : assetType;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public String structureKey() {
        return structureKey;
    }

    public ProtectedStructureType structureType() {
        return structureType;
    }

    public String assetType() {
        return assetType;
    }
}
