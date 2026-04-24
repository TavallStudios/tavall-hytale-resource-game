package com.tavall.hytale.resourcegame.persistence;

import com.tavall.hytale.resourcegame.config.DatabaseConfig;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PersistenceStoreBootstrapTest {
    @Test
    void resolveUsesInMemoryStoresWhenPostgresIsNotConfigured() {
        PersistenceStoreBootstrap bootstrap = new PersistenceStoreBootstrap(Logger.getLogger("test"));

        ResolvedPersistenceStores stores = bootstrap.resolve(new DatabaseConfig("", "", ""));

        assertInstanceOf(InMemoryPlayerProfileStore.class, stores.profileStore());
        assertInstanceOf(InMemoryPlayerGameStateStore.class, stores.gameStateStore());
        assertFalse(stores.postgresConfigured());
        assertFalse(stores.postgresReachable());
        assertFalse(stores.usesPostgres());
    }

    @Test
    void resolveFallsBackToInMemoryStoresWhenPostgresIsUnreachable() {
        PersistenceStoreBootstrap bootstrap = new PersistenceStoreBootstrap(Logger.getLogger("test"));

        ResolvedPersistenceStores stores = bootstrap.resolve(
                new DatabaseConfig("jdbc:postgresql://127.0.0.1:1/not-real", "invalid", "invalid")
        );

        assertInstanceOf(InMemoryPlayerProfileStore.class, stores.profileStore());
        assertInstanceOf(InMemoryPlayerGameStateStore.class, stores.gameStateStore());
        assertTrue(stores.postgresConfigured());
        assertFalse(stores.postgresReachable());
        assertFalse(stores.usesPostgres());
    }

    @Test
    void resolveFallsBackToInMemoryStoresWhenSchemaBootstrapFails() {
        PersistenceStoreBootstrap bootstrap = new PersistenceStoreBootstrap(
                Logger.getLogger("test"),
                new PostgresSchemaBootstrap(Logger.getLogger("test")) {
                    @Override
                    public boolean ensureSchema(PostgresConnectionProvider connectionProvider) {
                        return false;
                    }
                }
        ) {
            @Override
            protected boolean isReachable(PostgresConnectionProvider connectionProvider) {
                return true;
            }
        };

        ResolvedPersistenceStores stores = bootstrap.resolve(
                new DatabaseConfig("jdbc:postgresql://127.0.0.1:5432/not-real", "invalid", "invalid")
        );

        assertInstanceOf(InMemoryPlayerProfileStore.class, stores.profileStore());
        assertInstanceOf(InMemoryPlayerGameStateStore.class, stores.gameStateStore());
        assertTrue(stores.postgresConfigured());
        assertTrue(stores.postgresReachable());
        assertFalse(stores.usesPostgres());
    }
}
