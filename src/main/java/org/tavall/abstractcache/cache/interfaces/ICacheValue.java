package org.tavall.abstractcache.cache.interfaces;

import java.time.Instant;

/**
 * Value wrapper for cached payloads.
 */
public interface ICacheValue<T> {
    T getValue();

    Instant expiresAt();
}
