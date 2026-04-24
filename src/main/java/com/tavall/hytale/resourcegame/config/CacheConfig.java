package com.tavall.hytale.resourcegame.config;

/**
 * Cache configuration for Redis usage.
 */
public final class CacheConfig {
    private final String redisHost;
    private final int redisPort;
    private final String redisPassword;
    private final boolean redisTls;

    public CacheConfig(String redisHost, int redisPort, String redisPassword, boolean redisTls) {
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.redisPassword = redisPassword;
        this.redisTls = redisTls;
    }

    public String redisHost() {
        return redisHost;
    }

    public int redisPort() {
        return redisPort;
    }

    public String redisPassword() {
        return redisPassword;
    }

    public boolean redisTls() {
        return redisTls;
    }

    public String redisUrl() {
        if (redisHost == null || redisHost.isBlank()) {
            return null;
        }
        String scheme = redisTls ? "rediss" : "redis";
        String auth = redisPassword == null || redisPassword.isBlank()
                ? ""
                : ":" + redisPassword + "@";
        return scheme + "://" + auth + redisHost + ":" + redisPort;
    }

    public static CacheConfig fromEnv() {
        return new CacheConfig(
                EnvironmentConfig.get("TAVALL_REDIS_HOST", ""),
                EnvironmentConfig.getInt("TAVALL_REDIS_PORT", 6379),
                EnvironmentConfig.get("TAVALL_REDIS_PASSWORD", ""),
                EnvironmentConfig.getBoolean("TAVALL_REDIS_TLS", false)
        );
    }
}
