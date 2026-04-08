package org.tavall.abstractcache.semantic;

import org.tavall.abstractcache.cache.interfaces.ICacheValue;
import org.tavall.abstractcache.semantic.model.SemanticCacheKey;
import org.tavall.abstractcache.semantic.spi.CacheCodec;
import redis.clients.jedis.JedisPooled;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * In-memory compatibility cache with optional Redis persistence.
 */
final class SimpleSemanticCache implements SemanticCache {
    private static final Logger LOGGER = Logger.getLogger(SimpleSemanticCache.class.getName());

    private final String cacheName;
    private final Map<String, SimpleCacheValue<?>> entries;
    private final JedisPooled redis;

    SimpleSemanticCache(String cacheName, String redisUrl) {
        this.cacheName = cacheName;
        this.entries = new ConcurrentHashMap<>();
        this.redis = createRedis(redisUrl);
    }

    @Override
    public <T> Optional<ICacheValue<T>> get(SemanticCacheKey key, CacheCodec<T> codec) {
        String scopedKey = scopedKey(key);
        SimpleCacheValue<?> cached = entries.get(scopedKey);
        if (cached == null) {
            return readRemote(key, codec);
        }
        if (cached.expiresAt().isBefore(Instant.now())) {
            entries.remove(scopedKey);
            return readRemote(key, codec);
        }
        @SuppressWarnings("unchecked")
        SimpleCacheValue<T> typed = (SimpleCacheValue<T>) cached;
        return Optional.of(typed);
    }

    @Override
    public <T> void put(SemanticCacheKey key, T value, Duration ttl, CacheCodec<T> codec) {
        String scopedKey = scopedKey(key);
        Instant expiresAt = Instant.now().plus(ttl == null ? Duration.ofMinutes(5) : ttl);
        entries.put(scopedKey, new SimpleCacheValue<>(value, expiresAt));
        if (redis == null) {
            return;
        }
        try {
            long ttlSeconds = Math.max(1L, (ttl == null ? Duration.ofMinutes(5) : ttl).toSeconds());
            redis.setex(scopedKey.getBytes(), ttlSeconds, codec.encode(value));
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Redis cache write failed for " + scopedKey + ".", ex);
        }
    }

    private String scopedKey(SemanticCacheKey key) {
        return cacheName + "::" + key.value();
    }

    private <T> Optional<ICacheValue<T>> readRemote(SemanticCacheKey key, CacheCodec<T> codec) {
        if (redis == null) {
            return Optional.empty();
        }
        String scopedKey = scopedKey(key);
        try {
            byte[] payload = redis.get(scopedKey.getBytes());
            if (payload == null) {
                return Optional.empty();
            }
            long ttlMillis = redis.pttl(scopedKey.getBytes());
            Instant expiresAt = ttlMillis > 0
                    ? Instant.now().plusMillis(ttlMillis)
                    : Instant.now().plus(Duration.ofMinutes(5));
            T value = codec.decode(payload);
            SimpleCacheValue<T> cached = new SimpleCacheValue<>(value, expiresAt);
            entries.put(scopedKey, cached);
            return Optional.of(cached);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Redis cache read failed for " + scopedKey + ".", ex);
            return Optional.empty();
        }
    }

    private JedisPooled createRedis(String redisUrl) {
        if (redisUrl == null || redisUrl.isBlank()) {
            return null;
        }
        try {
            return new JedisPooled(redisUrl);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Redis cache initialization failed for " + redisUrl + ".", ex);
            return null;
        }
    }
}
