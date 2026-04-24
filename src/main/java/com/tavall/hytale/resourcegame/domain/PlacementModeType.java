package com.tavall.hytale.resourcegame.domain;

/**
 * Describes the type of world placement the player is currently arming.
 */
public enum PlacementModeType {
    CASTLE("Castle"),
    RESOURCE_NODE("Resource Node"),
    BUILDING("Building");

    private final String displayName;

    PlacementModeType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
