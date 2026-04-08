package com.tavall.hytale.resourcegame.domain;

/**
 * Inventory of core resources.
 */
public final class ResourceInventory {
    private final int food;
    private final int wood;
    private final int iron;

    public ResourceInventory(int food, int wood, int iron) {
        this.food = Math.max(0, food);
        this.wood = Math.max(0, wood);
        this.iron = Math.max(0, iron);
    }

    public int food() {
        return food;
    }

    public int wood() {
        return wood;
    }

    public int iron() {
        return iron;
    }

    public ResourceInventory withFood(int food) {
        return new ResourceInventory(food, wood, iron);
    }

    public ResourceInventory withWood(int wood) {
        return new ResourceInventory(food, wood, iron);
    }

    public ResourceInventory withIron(int iron) {
        return new ResourceInventory(food, wood, iron);
    }

    public static ResourceInventory starterPack() {
        return new ResourceInventory(40, 25, 10);
    }
}
