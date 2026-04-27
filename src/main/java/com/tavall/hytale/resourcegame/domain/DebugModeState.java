package com.tavall.hytale.resourcegame.domain;

public record DebugModeState(boolean levelRestrictionsIgnored) {
    public static DebugModeState disabled() {
        return new DebugModeState(false);
    }

    public static DebugModeState enabled() {
        return new DebugModeState(true);
    }

    public DebugModeState withLevelRestrictionsIgnored(boolean ignored) {
        return new DebugModeState(ignored);
    }
}
