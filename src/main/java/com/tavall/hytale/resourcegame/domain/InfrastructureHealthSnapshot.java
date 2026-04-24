package com.tavall.hytale.resourcegame.domain;

/**
 * Captures the current cache and persistence operating modes.
 */
public final class InfrastructureHealthSnapshot {
    private final boolean redisConfigured;
    private final boolean redisReachable;
    private final boolean postgresConfigured;
    private final boolean postgresReachable;

    public InfrastructureHealthSnapshot(
            boolean redisConfigured,
            boolean redisReachable,
            boolean postgresConfigured,
            boolean postgresReachable
    ) {
        this.redisConfigured = redisConfigured;
        this.redisReachable = redisReachable;
        this.postgresConfigured = postgresConfigured;
        this.postgresReachable = postgresReachable;
    }

    public boolean redisConfigured() {
        return redisConfigured;
    }

    public boolean redisReachable() {
        return redisReachable;
    }

    public boolean postgresConfigured() {
        return postgresConfigured;
    }

    public boolean postgresReachable() {
        return postgresReachable;
    }

    public String cacheSummary() {
        if (!redisConfigured) {
            return "memory-only (Redis not configured)";
        }
        return redisReachable ? "memory+redis (connected)" : "memory+redis (degraded)";
    }

    public String persistenceSummary() {
        if (!postgresConfigured) {
            return "in-memory fallback (Postgres not configured)";
        }
        return postgresReachable ? "postgres (connected)" : "postgres (degraded)";
    }
}
