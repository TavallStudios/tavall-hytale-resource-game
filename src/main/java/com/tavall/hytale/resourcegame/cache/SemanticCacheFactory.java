package com.tavall.hytale.resourcegame.cache;

import com.tavall.hytale.resourcegame.config.CacheConfig;
import org.tavall.abstractcache.semantic.SemanticCache;
import org.tavall.abstractcache.semantic.SemanticCacheBuilder;
import org.tavall.abstractcache.semantic.model.CacheTier;
import redis.clients.jedis.JedisPooled;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds semantic cache instances with Redis support when configured.
 */
public final class SemanticCacheFactory {
    private static final Logger LOGGER = Logger.getLogger(SemanticCacheFactory.class.getName());

    private final CacheConfig cacheConfig;

    public SemanticCacheFactory(CacheConfig cacheConfig) {
        this.cacheConfig = Objects.requireNonNull(cacheConfig, "cacheConfig");
    }

    public SemanticCache build(String cacheName) {
        SemanticCacheBuilder builder = new SemanticCacheBuilder().cacheName(cacheName).withHotMemoryTier();
        String redisUrl = cacheConfig.redisUrl();
        if (redisUrl != null && !redisUrl.isBlank() && isRedisReachable(redisUrl)) {
            builder.withRedisTier(CacheTier.REMOTE_HOT, redisUrl);
        }
        return builder.build();
    }

    private boolean isRedisReachable(String redisUrl) {
        try (JedisPooled jedis = new JedisPooled(redisUrl)) {
            boolean reachable = "PONG".equalsIgnoreCase(jedis.ping());
            if (reachable) {
                LOGGER.info("Redis cache connectivity verified for resource game cache bootstrap.");
            }
            return reachable;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Redis is configured but unavailable. Falling back to memory-only semantic cache.", ex);
            return false;
        }
    }
}
