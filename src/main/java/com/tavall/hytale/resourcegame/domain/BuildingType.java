package com.tavall.hytale.resourcegame.domain;

import com.tavall.hytale.resourcegame.population.PromotionCost;

import java.util.Locale;

/**
 * Supported upgradeable building types for the vertical slice.
 */
public enum BuildingType {
    FARMSTEAD("Farmstead", BuildingAreaType.CASTLE_SURFACE, "Passive food income and larger farm visuals.", 3),
    LUMBER_MILL("Lumber Mill", BuildingAreaType.CASTLE_SURFACE, "Passive wood income and visible saw-yard visuals.", 3),
    IRON_WORKS("Iron Works", BuildingAreaType.CASTLE_SURFACE, "Passive iron income and forge-like placeholder visuals.", 3),
    BARRACKS("Barracks", BuildingAreaType.CASTLE_INTERIOR, "Reduces citizen-to-troop promotion cost and expands troop staging.", 3),
    WORKSHOP("Workshop", BuildingAreaType.CASTLE_INTERIOR, "Speeds up future construction and upgrade work.", 3);

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
                default -> new BuildingLevelProfile(64, 28, 12, 48, 7, 0, 0, 0.0D, new PromotionCost(0, 0, 0));
            };
            case LUMBER_MILL -> switch (safeLevel) {
                case 1 -> new BuildingLevelProfile(18, 18, 0, 24, 0, 2, 0, 0.0D, new PromotionCost(0, 0, 0));
                case 2 -> new BuildingLevelProfile(30, 28, 8, 36, 0, 4, 0, 0.0D, new PromotionCost(0, 0, 0));
                default -> new BuildingLevelProfile(44, 40, 16, 48, 0, 7, 0, 0.0D, new PromotionCost(0, 0, 0));
            };
            case IRON_WORKS -> switch (safeLevel) {
                case 1 -> new BuildingLevelProfile(16, 12, 10, 30, 0, 0, 1, 0.0D, new PromotionCost(0, 0, 0));
                case 2 -> new BuildingLevelProfile(24, 18, 18, 42, 0, 0, 2, 0.0D, new PromotionCost(0, 0, 0));
                default -> new BuildingLevelProfile(34, 28, 28, 54, 0, 0, 4, 0.0D, new PromotionCost(0, 0, 0));
            };
            case BARRACKS -> switch (safeLevel) {
                case 1 -> new BuildingLevelProfile(24, 20, 10, 30, 0, 0, 0, 0.0D, new PromotionCost(1, 0, 0));
                case 2 -> new BuildingLevelProfile(36, 28, 18, 42, 0, 0, 0, 0.0D, new PromotionCost(1, 1, 0));
                default -> new BuildingLevelProfile(48, 38, 28, 54, 0, 0, 0, 0.0D, new PromotionCost(2, 1, 1));
            };
            case WORKSHOP -> switch (safeLevel) {
                case 1 -> new BuildingLevelProfile(20, 24, 8, 30, 0, 0, 0, 0.15D, new PromotionCost(0, 0, 0));
                case 2 -> new BuildingLevelProfile(28, 36, 14, 42, 0, 0, 0, 0.30D, new PromotionCost(0, 0, 0));
                default -> new BuildingLevelProfile(40, 48, 24, 54, 0, 0, 0, 0.45D, new PromotionCost(0, 0, 0));
            };
        };
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
