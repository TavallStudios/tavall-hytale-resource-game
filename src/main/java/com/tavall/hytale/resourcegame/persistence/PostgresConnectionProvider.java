package com.tavall.hytale.resourcegame.persistence;

import com.tavall.hytale.resourcegame.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Provides Postgres connections for repositories.
 */
public final class PostgresConnectionProvider {
    private static final String POSTGRES_DRIVER_CLASS = "org.postgresql.Driver";

    private final DatabaseConfig databaseConfig;

    public PostgresConnectionProvider(DatabaseConfig databaseConfig) {
        this.databaseConfig = Objects.requireNonNull(databaseConfig, "databaseConfig");
    }

    public Connection open() throws SQLException {
        if (databaseConfig.jdbcUrl() == null || databaseConfig.jdbcUrl().isBlank()) {
            throw new SQLException("Postgres JDBC URL is not configured.");
        }
        ensureDriverLoaded();
        return DriverManager.getConnection(
                databaseConfig.jdbcUrl(),
                databaseConfig.username(),
                databaseConfig.password()
        );
    }

    private void ensureDriverLoaded() throws SQLException {
        try {
            Class.forName(POSTGRES_DRIVER_CLASS);
        } catch (ClassNotFoundException ex) {
            throw new SQLException("Postgres JDBC driver is not available.", ex);
        }
    }
}
