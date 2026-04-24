package org.tavall.abstractcache.semantic;

import org.tavall.abstractcache.cache.interfaces.ICacheValue;

import java.time.Instant;

/**
 * Simple cache value wrapper.
 */
public record SimpleCacheValue<T>(T getValue, Instant expiresAt) implements ICacheValue<T> {
}
