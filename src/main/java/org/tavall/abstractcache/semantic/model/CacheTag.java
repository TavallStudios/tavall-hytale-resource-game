package org.tavall.abstractcache.semantic.model;

import java.util.Objects;

/**
 * Lightweight cache tag.
 */
public record CacheTag(String value) {
    public CacheTag {
        Objects.requireNonNull(value, "value");
    }

    public static CacheTag of(String value) {
        return new CacheTag(value);
    }
}
