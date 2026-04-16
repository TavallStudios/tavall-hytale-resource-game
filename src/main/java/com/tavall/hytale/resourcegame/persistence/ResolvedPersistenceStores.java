package com.tavall.hytale.resourcegame.persistence;

/**
 * Captures the active persistence stores selected during runtime bootstrap.
 */
public final class ResolvedPersistenceStores {
    private final PlayerProfileStore profileStore;
    private final PlayerGameStateStore gameStateStore;
    private final boolean postgresConfigured;
    private final boolean postgresReachable;
    private final boolean usesPostgres;

    public ResolvedPersistenceStores(
            PlayerProfileStore profileStore,
            PlayerGameStateStore gameStateStore,
            boolean postgresConfigured,
            boolean postgresReachable,
            boolean usesPostgres
    ) {
        this.profileStore = profileStore;
        this.gameStateStore = gameStateStore;
        this.postgresConfigured = postgresConfigured;
        this.postgresReachable = postgresReachable;
        this.usesPostgres = usesPostgres;
    }

    public PlayerProfileStore profileStore() {
        return profileStore;
    }

    public PlayerGameStateStore gameStateStore() {
        return gameStateStore;
    }

    public boolean postgresConfigured() {
        return postgresConfigured;
    }

    public boolean postgresReachable() {
        return postgresReachable;
    }

    public boolean usesPostgres() {
        return usesPostgres;
    }
}
