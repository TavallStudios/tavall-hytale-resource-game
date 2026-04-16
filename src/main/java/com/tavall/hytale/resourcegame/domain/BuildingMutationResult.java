package com.tavall.hytale.resourcegame.domain;

/**
 * Result of a building mutation or construction action.
 */
public final class BuildingMutationResult {
    private final PlayerGameState state;
    private final boolean changed;
    private final String message;

    public BuildingMutationResult(PlayerGameState state, boolean changed, String message) {
        this.state = state;
        this.changed = changed;
        this.message = message == null ? "" : message;
    }

    public PlayerGameState state() {
        return state;
    }

    public boolean changed() {
        return changed;
    }

    public String message() {
        return message;
    }

    public static BuildingMutationResult unchanged(PlayerGameState state, String message) {
        return new BuildingMutationResult(state, false, message);
    }

    public static BuildingMutationResult changed(PlayerGameState state, String message) {
        return new BuildingMutationResult(state, true, message);
    }
}
