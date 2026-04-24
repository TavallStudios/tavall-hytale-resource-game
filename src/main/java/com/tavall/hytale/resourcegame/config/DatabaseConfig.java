package com.tavall.hytale.resourcegame.config;

/**
 * Postgres configuration.
 */
public final class DatabaseConfig {
    private final String jdbcUrl;
    private final String username;
    private final String password;

    public DatabaseConfig(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public String jdbcUrl() {
        return jdbcUrl;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public static DatabaseConfig fromEnv() {
        return new DatabaseConfig(
                EnvironmentConfig.get("TAVALL_POSTGRES_URL", ""),
                EnvironmentConfig.get("TAVALL_POSTGRES_USER", ""),
                EnvironmentConfig.get("TAVALL_POSTGRES_PASSWORD", "")
        );
    }
}
