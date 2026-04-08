package org.tavall.abstractcache.semantic.spi;

/**
 * Codec contract for cache payloads.
 */
public interface CacheCodec<T> {
    String codecId();

    byte[] encode(T value);

    T decode(byte[] bytes);
}
