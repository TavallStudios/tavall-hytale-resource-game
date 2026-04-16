package com.tavall.hytale.resourcegame.persistence;

import com.tavall.hytale.resourcegame.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Resolves the active persistence stores for the current runtime.
 */
public class PersistenceStoreBootstrap {
    private static final int CONNECTION_VALIDATION_SECONDS = 2;

    private final Logger logger;
    private final PostgresSchemaBootstrap schemaBootstrap;

    public PersistenceStoreBootstrap(Logger logger) {
        this(logger, new PostgresSchemaBootstrap(logger));
    }

    public PersistenceStoreBootstrap(Logger logger, PostgresSchemaBootstrap schemaBootstrap) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.schemaBootstrap = Objects.requireNonNull(schemaBootstrap, "schemaBootstrap");
    }

    public ResolvedPersistenceStores resolve(DatabaseConfig databaseConfig) {
        Objects.requireNonNull(databaseConfig, "databaseConfig");
        boolean configured = databaseConfig.jdbcUrl() != null && !databaseConfig.jdbcUrl().isBlank();
        if (!configured) {
            logger.info("Postgres config not found. Using in-memory profile and game-state stores.");
            return inMemory(false, false);
        }

        PostgresConnectionProvider connectionProvider = new PostgresConnectionProvider(databaseConfig);
        if (!isReachable(connectionProvider)) {
            logger.warning("Postgres is configured but unavailable. Falling back to in-memory profile and game-state stores.");
            return inMemory(true, false);
        }
        if (!schemaBootstrap.ensureSchema(connectionProvider)) {
            logger.warning("Postgres is reachable but schema verification failed. Falling back to in-memory profile and game-state stores.");
            return inMemory(true, true);
        }

        logger.info("Postgres connection verified. Using JDBC profile and game-state stores.");
        return new ResolvedPersistenceStores(
                new PlayerProfileRepository(connectionProvider),
                new PlayerGameStateRepository(connectionProvider),
                true,
                true,
                true
        );
    }

    protected boolean isReachable(PostgresConnectionProvider connectionProvider) {
        try (Connection connection = connectionProvider.open()) {
            return connection.isValid(CONNECTION_VALIDATION_SECONDS);
        } catch (SQLException ex) {
            logger.warning("Postgres connectivity check failed: " + ex.getMessage());
            return false;
        }
    }

    private ResolvedPersistenceStores inMemory(boolean postgresConfigured, boolean postgresReachable) {
        return new ResolvedPersistenceStores(
                new InMemoryPlayerProfileStore(),
                new InMemoryPlayerGameStateStore(),
                postgresConfigured,
                postgresReachable,
                false
        );
    }
}
