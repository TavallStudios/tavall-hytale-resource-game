package com.tavall.hytale.resourcegame.domain;

/**
 * Coarse construction visual stage used for readable prototype building feedback.
 */
public enum BuildingConstructionStage {
    FOUNDATION,
    SCAFFOLDING,
    SHELL,
    COMPLETE;

    public static BuildingConstructionStage fromProgress(boolean underConstruction, double progressRatio) {
        if (!underConstruction) {
            return COMPLETE;
        }
        if (progressRatio < 0.25D) {
            return FOUNDATION;
        }
        if (progressRatio < 0.65D) {
            return SCAFFOLDING;
        }
        return SHELL;
    }
}
