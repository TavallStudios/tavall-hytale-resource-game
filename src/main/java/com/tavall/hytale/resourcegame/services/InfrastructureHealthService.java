package com.tavall.hytale.resourcegame.services;

import com.tavall.hytale.resourcegame.config.CacheConfig;
import com.tavall.hytale.resourcegame.config.DatabaseConfig;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInfrastructureHealthService;
import com.tavall.hytale.resourcegame.domain.InfrastructureHealthSnapshot;
import redis.clients.jedis.JedisPooled;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Probes the externally configured cache and database layers with short timeouts.
 */
public final class InfrastructureHealthService implements IInfrastructureHealthService, IDependencyInjectableConcrete {
    private static final Logger LOGGER = Logger.getLogger(InfrastructureHealthService.class.getName());
    private static final long PROBE_TIMEOUT_SECONDS = 2L;
    private static final String POSTGRES_DRIVER_CLASS = "org.postgresql.Driver";

    private final CacheConfig cacheConfig;
    private final DatabaseConfig databaseConfig;

    public InfrastructureHealthService(CacheConfig cacheConfig, DatabaseConfig databaseConfig) {
        this.cacheConfig = Objects.requireNonNull(cacheConfig, "cacheConfig");
        this.databaseConfig = Objects.requireNonNull(databaseConfig, "databaseConfig");
    }

    @Override
    public InfrastructureHealthSnapshot snapshot() {
        boolean redisConfigured = cacheConfig.redisUrl() != null && !cacheConfig.redisUrl().isBlank();
        boolean postgresConfigured = databaseConfig.jdbcUrl() != null && !databaseConfig.jdbcUrl().isBlank();
        return new InfrastructureHealthSnapshot(
                redisConfigured,
                redisConfigured && probeRedis(),
                postgresConfigured,
                postgresConfigured && probePostgres()
        );
    }

    private boolean probeRedis() {
        return runProbe(() -> {
            try (JedisPooled jedis = new JedisPooled(cacheConfig.redisUrl())) {
                return "PONG".equalsIgnoreCase(jedis.ping());
            }
        }, "Redis");
    }

    private boolean probePostgres() {
        return runProbe(() -> {
            Class.forName(POSTGRES_DRIVER_CLASS);
            try (Connection connection = DriverManager.getConnection(
                    databaseConfig.jdbcUrl(),
                    databaseConfig.username(),
                    databaseConfig.password()
            )) {
                return connection.isValid(2);
            }
        }, "Postgres");
    }

    private boolean runProbe(CheckedBooleanSupplier supplier, String systemName) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return supplier.getAsBoolean();
                } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
            }).get(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, systemName + " health probe failed.", ex);
            return false;
        }
    }
}
