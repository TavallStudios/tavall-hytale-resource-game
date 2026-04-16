package com.tavall.hytale.resourcegame.config;

/**
 * Castle asset and interaction defaults.
 */
public final class CastleAssetConfig {
    private final String npcRoleName;
    private final String displayName;
    private final double interactionDistance;
    private final double lookDotThreshold;

    public CastleAssetConfig(
            String npcRoleName,
            String displayName,
            double interactionDistance,
            double lookDotThreshold
    ) {
        this.npcRoleName = npcRoleName;
        this.displayName = displayName;
        this.interactionDistance = interactionDistance;
        this.lookDotThreshold = lookDotThreshold;
    }

    public String npcRoleName() {
        return npcRoleName;
    }

    public String displayName() {
        return displayName;
    }

    public double interactionDistance() {
        return interactionDistance;
    }

    public double lookDotThreshold() {
        return lookDotThreshold;
    }

    public static CastleAssetConfig defaults() {
        return new CastleAssetConfig("Kweebec_Elder", "Your Castle", 6.0, 0.85);
    }
}
