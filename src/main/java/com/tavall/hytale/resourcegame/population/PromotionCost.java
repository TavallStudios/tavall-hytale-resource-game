package com.tavall.hytale.resourcegame.population;

/**
 * Cost data for promoting citizens into troops.
 */
public final class PromotionCost {
    private final int foodCost;
    private final int woodCost;
    private final int ironCost;

    public PromotionCost(int foodCost, int woodCost, int ironCost) {
        this.foodCost = foodCost;
        this.woodCost = woodCost;
        this.ironCost = ironCost;
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

    public static PromotionCost defaultCost() {
        return new PromotionCost(4, 2, 1);
    }
}
