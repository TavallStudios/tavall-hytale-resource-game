package com.tavall.hytale.resourcegame.domain;

import com.tavall.hytale.resourcegame.population.PromotionCost;

/**
 * Read model for UI, command, and visual refresh around a specific building instance.
 */
public final class CastleBuildingSummary {
    private final CastleBuildingData buildingData;
    private final String worldName;
    private final double worldX;
    private final double worldY;
    private final double worldZ;
    private final double progressRatio;
    private final long remainingSeconds;
    private final BuildingConstructionStage constructionStage;
    private final BuildingLevelProfile completedLevelProfile;
    private final BuildingLevelProfile targetLevelProfile;
    private final BuildingLevelProfile nextUpgradeProfile;

    public CastleBuildingSummary(
            CastleBuildingData buildingData,
            String worldName,
            double worldX,
            double worldY,
            double worldZ,
            double progressRatio,
            long remainingSeconds,
            BuildingConstructionStage constructionStage,
            BuildingLevelProfile completedLevelProfile,
            BuildingLevelProfile targetLevelProfile,
            BuildingLevelProfile nextUpgradeProfile
    ) {
        this.buildingData = buildingData;
        this.worldName = worldName;
        this.worldX = worldX;
        this.worldY = worldY;
        this.worldZ = worldZ;
        this.progressRatio = progressRatio;
        this.remainingSeconds = remainingSeconds;
        this.constructionStage = constructionStage;
        this.completedLevelProfile = completedLevelProfile;
        this.targetLevelProfile = targetLevelProfile;
        this.nextUpgradeProfile = nextUpgradeProfile;
    }

    public CastleBuildingData buildingData() {
        return buildingData;
    }

    public String worldName() {
        return worldName;
    }

    public double worldX() {
        return worldX;
    }

    public double worldY() {
        return worldY;
    }

    public double worldZ() {
        return worldZ;
    }

    public double progressRatio() {
        return progressRatio;
    }

    public long remainingSeconds() {
        return remainingSeconds;
    }

    public BuildingConstructionStage constructionStage() {
        return constructionStage;
    }

    public BuildingLevelProfile completedLevelProfile() {
        return completedLevelProfile;
    }

    public BuildingLevelProfile targetLevelProfile() {
        return targetLevelProfile;
    }

    public BuildingLevelProfile nextUpgradeProfile() {
        return nextUpgradeProfile;
    }

    public int completedLevel() {
        return buildingData.currentLevel();
    }

    public int displayLevel() {
        return buildingData.isUnderConstruction() ? buildingData.targetLevel() : buildingData.currentLevel();
    }

    public boolean isUnderConstruction() {
        return buildingData.isUnderConstruction();
    }

    public String statusText() {
        if (buildingData.isUnderConstruction()) {
            return "Building " + constructionStage.name().toLowerCase();
        }
        return "Operational";
    }

    public int foodPerTickBonus() {
        return sumFoodBonus(completedLevelProfile);
    }

    public int woodPerTickBonus() {
        return sumWoodBonus(completedLevelProfile);
    }

    public int ironPerTickBonus() {
        return sumIronBonus(completedLevelProfile);
    }

    public double constructionSpeedBonus() {
        return completedLevelProfile == null ? 0.0D : completedLevelProfile.constructionSpeedBonus();
    }

    public PromotionCost promotionDiscount() {
        return completedLevelProfile == null ? new PromotionCost(0, 0, 0) : completedLevelProfile.promotionDiscount();
    }

    private int sumFoodBonus(BuildingLevelProfile profile) {
        return profile == null ? 0 : profile.foodPerTickBonus();
    }

    private int sumWoodBonus(BuildingLevelProfile profile) {
        return profile == null ? 0 : profile.woodPerTickBonus();
    }

    private int sumIronBonus(BuildingLevelProfile profile) {
        return profile == null ? 0 : profile.ironPerTickBonus();
    }
}
