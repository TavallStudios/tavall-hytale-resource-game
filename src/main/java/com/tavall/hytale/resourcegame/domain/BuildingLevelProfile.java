package com.tavall.hytale.resourcegame.domain;

import com.tavall.hytale.resourcegame.population.PromotionCost;

/**
 * Describes the gameplay effect and construction profile for a building target level.
 */
public final class BuildingLevelProfile {
    private final int foodCost;
    private final int woodCost;
    private final int ironCost;
    private final int buildSeconds;
    private final int foodPerTickBonus;
    private final int woodPerTickBonus;
    private final int ironPerTickBonus;
    private final double constructionSpeedBonus;
    private final PromotionCost promotionDiscount;

    public BuildingLevelProfile(
            int foodCost,
            int woodCost,
            int ironCost,
            int buildSeconds,
            int foodPerTickBonus,
            int woodPerTickBonus,
            int ironPerTickBonus,
            double constructionSpeedBonus,
            PromotionCost promotionDiscount
    ) {
        this.foodCost = Math.max(0, foodCost);
        this.woodCost = Math.max(0, woodCost);
        this.ironCost = Math.max(0, ironCost);
        this.buildSeconds = Math.max(1, buildSeconds);
        this.foodPerTickBonus = Math.max(0, foodPerTickBonus);
        this.woodPerTickBonus = Math.max(0, woodPerTickBonus);
        this.ironPerTickBonus = Math.max(0, ironPerTickBonus);
        this.constructionSpeedBonus = Math.max(0.0D, constructionSpeedBonus);
        this.promotionDiscount = promotionDiscount == null ? new PromotionCost(0, 0, 0) : promotionDiscount;
    }

    public int foodCost() {
        return foodCost;
    }

    public int woodCost() {
        return woodCost;
    }

    public int ironCost() {
        return ironCost;
    }

    public int buildSeconds() {
        return buildSeconds;
    }

    public int foodPerTickBonus() {
        return foodPerTickBonus;
    }

    public int woodPerTickBonus() {
        return woodPerTickBonus;
    }

    public int ironPerTickBonus() {
        return ironPerTickBonus;
    }

    public double constructionSpeedBonus() {
        return constructionSpeedBonus;
    }

    public PromotionCost promotionDiscount() {
        return promotionDiscount;
    }
}
