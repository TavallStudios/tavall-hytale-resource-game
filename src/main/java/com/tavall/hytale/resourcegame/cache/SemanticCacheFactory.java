package com.tavall.hytale.resourcegame.cache;

import com.tavall.hytale.resourcegame.config.CacheConfig;
import org.tavall.abstractcache.semantic.SemanticCache;
import org.tavall.abstractcache.semantic.SemanticCacheBuilder;
import org.tavall.abstractcache.semantic.model.CacheTier;

import java.util.Objects;

/**
 * Builds semantic cache instances with Redis support when configured.
 */
public final class SemanticCacheFactory {
    private final CacheConfig cacheConfig;

    public SemanticCacheFactory(CacheConfig cacheConfig) {
        this.cacheConfig = Objects.requireNonNull(cacheConfig, "cacheConfig");
    }

    public SemanticCache build(String cacheName) {
        SemanticCacheBuilder builder = new SemanticCacheBuilder().cacheName(cacheName).withHotMemoryTier();
        String redisUrl = cacheConfig.redisUrl();
        if (redisUrl != null && !redisUrl.isBlank()) {
            builder.withRedisTier(CacheTier.REMOTE_HOT, redisUrl);
        }
        return builder.build();
    }
}
