package com.tavall.hytale.resourcegame.services;

import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;

/**
 * Derives visible crowd, convoy, and stockpile counts for the castle-site scene.
 */
public final class CastleSiteScenePlanner implements IDependencyInjectableConcrete {
    public int visibleWorkerCount(int workers) {
        if (workers <= 0) {
            return 0;
        }
        return Math.min(6, Math.max(1, (int) Math.ceil(workers / 2.0)));
    }

    public int visibleStorageCount(int storedAmount) {
        if (storedAmount <= 0) {
            return 0;
        }
        return Math.min(6, Math.max(1, (int) Math.ceil(storedAmount / 30.0)));
    }

    public int visibleConvoyCount(int workers, int gainPerTick) {
        if (workers <= 0 || gainPerTick <= 0) {
            return 0;
        }
        return Math.min(4, Math.max(1, (int) Math.ceil(Math.max(workers, gainPerTick) / 3.0)));
    }

    public float stockpileAnchorScale(int totalStored) {
        return clampScale(1.1F + (totalStored / 240.0F), 1.1F, 2.4F);
    }

    public float populationAnchorScale(int count) {
        return clampScale(1.0F + (count / 18.0F), 1.0F, 2.1F);
    }

    public float nodeAnchorScale(int storedAmount, int workers) {
        return clampScale(0.95F + (storedAmount / 160.0F) + (workers / 8.0F), 0.95F, 2.0F);
    }

    public float crowdScale(int crowdCount) {
        return clampScale(0.65F + (crowdCount / 12.0F), 0.65F, 1.35F);
    }

    public float convoyScale(int workers, int gainPerTick) {
        return clampScale(0.55F + (Math.max(workers, gainPerTick) / 12.0F), 0.55F, 1.2F);
    }

    public String stockpileLabel(PlayerGameState state) {
        return "Stockpile | Food " + state.resources().food()
                + " | Wood " + state.resources().wood()
                + " | Iron " + state.resources().iron();
    }

    private float clampScale(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
