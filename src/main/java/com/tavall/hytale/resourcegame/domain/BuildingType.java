package com.tavall.hytale.resourcegame.domain;

import com.tavall.hytale.resourcegame.population.PromotionCost;

import java.util.Locale;

/**
 * Supported upgradeable building types for the vertical slice.
 */
public enum BuildingType {
    FARMSTEAD("Farmstead", BuildingAreaType.CASTLE_SURFACE, "Passive food income and larger farm visuals.", 30),
    LUMBER_MILL("Lumber Mill", BuildingAreaType.CASTLE_SURFACE, "Passive wood income and visible saw-yard visuals.", 30),
    IRON_WORKS("Iron Works", BuildingAreaType.CASTLE_SURFACE, "Passive iron income and forge-like placeholder visuals.", 30),
    BARRACKS("Barracks", BuildingAreaType.CASTLE_INTERIOR, "Reduces citizen-to-troop promotion cost and expands troop staging.", 30),
    WORKSHOP("Workshop", BuildingAreaType.CASTLE_INTERIOR, "Speeds up future construction and upgrade work.", 30);

    private final String displayName;
    private final BuildingAreaType areaType;
    private final String description;
    private final int maxLevel;

    BuildingType(String displayName, BuildingAreaType areaType, String description, int maxLevel) {
        this.displayName = displayName;
        this.areaType = areaType;
        this.description = description;
        this.maxLevel = maxLevel;
    }

    public String displayName() {
        return displayName;
    }

    public BuildingAreaType areaType() {
        return areaType;
    }

    public String description() {
        return description;
    }

    public int maxLevel() {
        return maxLevel;
    }

    public BuildingLevelProfile levelProfile(int targetLevel) {
        int safeLevel = Math.max(1, Math.min(maxLevel, targetLevel));
        return switch (this) {
            case FARMSTEAD -> switch (safeLevel) {
                case 1 -> new BuildingLevelProfile(28, 12, 0, 24, 2, 0, 0, 0.0D, new PromotionCost(0, 0, 0));
                case 2 -> new BuildingLevelProfile(42, 20, 6, 36, 4, 0, 0, 0.0D, new PromotionCost(0, 0, 0));
                case 3 -> new BuildingLevelProfile(64, 28, 12, 48, 7, 0, 0, 0.0D, new PromotionCost(0, 0, 0));
                default -> scaledYieldProfile(safeLevel, new BuildingLevelProfile(64, 28, 12, 48, 7, 0, 0, 0.0D, new PromotionCost(0, 0, 0)), 10, 6, 4, 6, 1, 0, 0);
            };
            case LUMBER_MILL -> switch (safeLevel) {
                case 1 -> new BuildingLevelProfile(18, 18, 0, 24, 0, 2, 0, 0.0D, new PromotionCost(0, 0, 0));
                case 2 -> new BuildingLevelProfile(30, 28, 8, 36, 0, 4, 0, 0.0D, new PromotionCost(0, 0, 0));
                case 3 -> new BuildingLevelProfile(44, 40, 16, 48, 0, 7, 0, 0.0D, new PromotionCost(0, 0, 0));
                default -> scaledYieldProfile(safeLevel, new BuildingLevelProfile(44, 40, 16, 48, 0, 7, 0, 0.0D, new PromotionCost(0, 0, 0)), 9, 9, 5, 6, 0, 1, 0);
            };
            case IRON_WORKS -> switch (safeLevel) {
                case 1 -> new BuildingLevelProfile(16, 12, 10, 30, 0, 0, 1, 0.0D, new PromotionCost(0, 0, 0));
                case 2 -> new BuildingLevelProfile(24, 18, 18, 42, 0, 0, 2, 0.0D, new PromotionCost(0, 0, 0));
                case 3 -> new BuildingLevelProfile(34, 28, 28, 54, 0, 0, 4, 0.0D, new PromotionCost(0, 0, 0));
                default -> scaledYieldProfile(safeLevel, new BuildingLevelProfile(34, 28, 28, 54, 0, 0, 4, 0.0D, new PromotionCost(0, 0, 0)), 8, 8, 8, 7, 0, 0, 1);
            };
            case BARRACKS -> switch (safeLevel) {
                case 1 -> new BuildingLevelProfile(24, 20, 10, 30, 0, 0, 0, 0.0D, new PromotionCost(1, 0, 0));
                case 2 -> new BuildingLevelProfile(36, 28, 18, 42, 0, 0, 0, 0.0D, new PromotionCost(1, 1, 0));
                case 3 -> new BuildingLevelProfile(48, 38, 28, 54, 0, 0, 0, 0.0D, new PromotionCost(2, 1, 1));
                default -> scaledBarracksProfile(safeLevel);
            };
            case WORKSHOP -> switch (safeLevel) {
                case 1 -> new BuildingLevelProfile(20, 24, 8, 30, 0, 0, 0, 0.15D, new PromotionCost(0, 0, 0));
                case 2 -> new BuildingLevelProfile(28, 36, 14, 42, 0, 0, 0, 0.30D, new PromotionCost(0, 0, 0));
                case 3 -> new BuildingLevelProfile(40, 48, 24, 54, 0, 0, 0, 0.45D, new PromotionCost(0, 0, 0));
                default -> scaledWorkshopProfile(safeLevel);
            };
        };
    }

    private static BuildingLevelProfile scaledYieldProfile(
            int level,
            BuildingLevelProfile baseLevelThree,
            int foodCostStep,
            int woodCostStep,
            int ironCostStep,
            int buildSecondsStep,
            int foodPerTickStep,
            int woodPerTickStep,
            int ironPerTickStep
    ) {
        int delta = Math.max(0, level - 3);
        return new BuildingLevelProfile(
                baseLevelThree.foodCost() + (foodCostStep * delta),
                baseLevelThree.woodCost() + (woodCostStep * delta),
                baseLevelThree.ironCost() + (ironCostStep * delta),
                baseLevelThree.buildSeconds() + (buildSecondsStep * delta),
                baseLevelThree.foodPerTickBonus() + (foodPerTickStep * delta),
                baseLevelThree.woodPerTickBonus() + (woodPerTickStep * delta),
                baseLevelThree.ironPerTickBonus() + (ironPerTickStep * delta),
                0.0D,
                new PromotionCost(0, 0, 0)
        );
    }

    private static BuildingLevelProfile scaledBarracksProfile(int level) {
        int delta = Math.max(0, level - 3);
        int foodDiscount = Math.min(8, 2 + (delta / 5));
        int woodDiscount = Math.min(6, 1 + (delta / 8));
        int ironDiscount = Math.min(6, 1 + (delta / 10));
        return new BuildingLevelProfile(
                48 + (10 * delta),
                38 + (8 * delta),
                28 + (6 * delta),
                54 + (6 * delta),
                0,
                0,
                0,
                0.0D,
                new PromotionCost(foodDiscount, woodDiscount, ironDiscount)
        );
    }

    private static BuildingLevelProfile scaledWorkshopProfile(int level) {
        int delta = Math.max(0, level - 3);
        double speed = Math.min(0.90D, 0.45D + (0.015D * delta));
        return new BuildingLevelProfile(
                40 + (8 * delta),
                48 + (10 * delta),
                24 + (6 * delta),
                54 + (6 * delta),
                0,
                0,
                0,
                speed,
                new PromotionCost(0, 0, 0)
        );
    }

    public String shortKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static BuildingType parse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String normalized = token.trim().toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
        for (BuildingType value : values()) {
            if (value.shortKey().equals(normalized)) {
                return value;
            }
        }
        return null;
    }
}
