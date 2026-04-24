package com.tavall.hytale.resourcegame.domain;

/**
 * Outcome of a placement request operation.
 */
public final class PlacementResult {
    private final boolean handled;
    private final boolean success;
    private final String message;
    private final PlayerGameState updatedState;

    private PlacementResult(boolean handled, boolean success, String message, PlayerGameState updatedState) {
        this.handled = handled;
        this.success = success;
        this.message = message == null ? "" : message;
        this.updatedState = updatedState;
    }

    public static PlacementResult ignored() {
        return new PlacementResult(false, false, "", null);
    }

    public static PlacementResult success(String message, PlayerGameState updatedState) {
        return new PlacementResult(true, true, message, updatedState);
    }

    public static PlacementResult failure(String message) {
        return new PlacementResult(true, false, message, null);
    }

    public boolean handled() {
        return handled;
    }

    public boolean success() {
        return success;
    }

    public String message() {
        return message;
    }

    public PlayerGameState updatedState() {
        return updatedState;
    }
}
