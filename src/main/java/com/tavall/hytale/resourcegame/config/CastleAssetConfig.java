package com.tavall.hytale.resourcegame.config;

/**
 * Castle asset and interaction defaults.
 */
public final class CastleAssetConfig {
    private final String npcRoleName;
    private final String displayName;
    private final String structureAssetType;
    private final String castleBlockType;
    private final double interactionDistance;
    private final double lookDotThreshold;

    public CastleAssetConfig(
            String npcRoleName,
            String displayName,
            String structureAssetType,
            String castleBlockType,
            double interactionDistance,
            double lookDotThreshold
    ) {
        this.npcRoleName = npcRoleName;
        this.displayName = displayName;
        this.structureAssetType = structureAssetType;
        this.castleBlockType = castleBlockType;
        this.interactionDistance = interactionDistance;
        this.lookDotThreshold = lookDotThreshold;
    }

    public String npcRoleName() {
        return npcRoleName;
    }

    public String displayName() {
        return displayName;
    }

    public String structureAssetType() {
        return structureAssetType;
    }

    public String castleBlockType() {
        return castleBlockType;
    }

    public double interactionDistance() {
        return interactionDistance;
    }

    public double lookDotThreshold() {
        return lookDotThreshold;
    }

    public static CastleAssetConfig defaults() {
        return new CastleAssetConfig("Kweebec_Elder", "Castle", "stone_column_castle", "Rock_Stone_Brick", 7.5, 0.85);
    }
}
