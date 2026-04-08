package com.tavall.hytale.resourcegame.ui;

import java.util.Objects;

/**
 * Describes whether a UI-driven upgrade action is currently available.
 */
public final class UpgradeActionState {
    private final boolean allowed;
    private final String message;

    public UpgradeActionState(boolean allowed, String message) {
        this.allowed = allowed;
        this.message = Objects.requireNonNull(message, "message");
    }

    public boolean allowed() {
        return allowed;
    }

    public String message() {
        return message;
    }
}
