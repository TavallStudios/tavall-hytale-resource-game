package com.tavall.hytale.resourcegame.domain;

import java.util.Objects;

/**
 * Result of a manual resource-node pillage action.
 */
public final class ResourceNodePillageResult {
    private final PlayerGameState state;
    private final boolean changed;
    private final int reward;
    private final String message;

    public ResourceNodePillageResult(PlayerGameState state, boolean changed, int reward, String message) {
        this.state = state;
        this.changed = changed;
        this.reward = Math.max(0, reward);
        this.message = Objects.requireNonNull(message, "message");
    }

    public PlayerGameState state() {
        return state;
    }

    public boolean changed() {
        return changed;
    }

    public int reward() {
        return reward;
    }

    public String message() {
        return message;
    }
}
