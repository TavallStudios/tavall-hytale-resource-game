package org.tavall.abstractcache.semantic;

import org.tavall.abstractcache.cache.interfaces.ICacheValue;
import org.tavall.abstractcache.semantic.model.SemanticCacheKey;
import org.tavall.abstractcache.semantic.spi.CacheCodec;

import java.time.Duration;
import java.util.Optional;

/**
 * Minimal semantic cache contract.
 */
public interface SemanticCache {
    <T> Optional<ICacheValue<T>> get(SemanticCacheKey key, CacheCodec<T> codec);

    <T> void put(SemanticCacheKey key, T value, Duration ttl, CacheCodec<T> codec);
}
