package org.tavall.abstractcache.semantic.model;

import org.tavall.abstractcache.cache.enums.CacheDomain;
import org.tavall.abstractcache.cache.enums.CacheSource;
import org.tavall.abstractcache.cache.enums.CacheVersion;

import java.util.Objects;
import java.util.Set;

/**
 * Immutable cache key metadata.
 */
public record SemanticCacheKey(
        String value,
        CacheDomain domain,
        CacheSource source,
        CacheVersion version,
        Set<CacheTag> tags
) {
    public SemanticCacheKey {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(version, "version");
        tags = tags == null ? Set.of() : Set.copyOf(tags);
    }
}
