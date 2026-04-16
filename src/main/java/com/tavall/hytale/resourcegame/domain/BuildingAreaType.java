package com.tavall.hytale.resourcegame.domain;

/**
 * High-level ownership zone for kingdom buildings.
 */
public enum BuildingAreaType {
    CASTLE_SURFACE("Castle Surface"),
    CASTLE_INTERIOR("Castle Interior");

    private final String displayName;

    BuildingAreaType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
