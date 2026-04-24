package com.tavall.hytale.resourcegame.services;

import com.tavall.hytale.resourcegame.config.CacheConfig;
import com.tavall.hytale.resourcegame.config.DatabaseConfig;
import com.tavall.hytale.resourcegame.domain.InfrastructureHealthSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public final class InfrastructureHealthServiceTest {
    @Test
    void snapshotReportsLocalFallbackWhenExternalSystemsAreNotConfigured() {
        InfrastructureHealthService service = new InfrastructureHealthService(
                new CacheConfig("", 6379, "", false),
                new DatabaseConfig("", "", "")
        );

        InfrastructureHealthSnapshot snapshot = service.snapshot();

        assertFalse(snapshot.redisConfigured());
        assertFalse(snapshot.postgresConfigured());
        assertEquals("memory-only (Redis not configured)", snapshot.cacheSummary());
        assertEquals("in-memory fallback (Postgres not configured)", snapshot.persistenceSummary());
    }
}
