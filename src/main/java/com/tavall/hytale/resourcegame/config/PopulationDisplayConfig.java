package com.tavall.hytale.resourcegame.config;

/**
 * Configuration for population display entities.
 */
public final class PopulationDisplayConfig {
    private final String npcRoleName;
    private final String citizenLabel;
    private final String troopLabel;

    public PopulationDisplayConfig(String npcRoleName, String citizenLabel, String troopLabel) {
        this.npcRoleName = npcRoleName;
        this.citizenLabel = citizenLabel;
        this.troopLabel = troopLabel;
    }

    public String npcRoleName() {
        return npcRoleName;
    }

    public String citizenLabel() {
        return citizenLabel;
    }

    public String troopLabel() {
        return troopLabel;
    }

    public static PopulationDisplayConfig defaults() {
        return new PopulationDisplayConfig("human", "Citizens", "Troops");
    }
}
