package org.tavall.abstractcache.semantic;

import org.tavall.abstractcache.semantic.model.CacheTier;

/**
 * Minimal builder that returns an in-memory cache.
 */
public final class SemanticCacheBuilder {
    private String cacheName;
    private String redisUrl;

    public SemanticCacheBuilder cacheName(String cacheName) {
        this.cacheName = cacheName;
        return this;
    }

    public SemanticCacheBuilder withHotMemoryTier() {
        return this;
    }

    public SemanticCacheBuilder withRedisTier(CacheTier cacheTier, String redisUrl) {
        this.redisUrl = redisUrl;
        return this;
    }

    public SemanticCache build() {
        return new SimpleSemanticCache(cacheName == null ? "default" : cacheName, redisUrl);
    }
}
