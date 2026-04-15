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

    public String stockpileLabel(PlayerGameState state) {
        return "Stockpile | Food " + state.resources().food()
                + " | Wood " + state.resources().wood()
                + " | Iron " + state.resources().iron();
    }
}
