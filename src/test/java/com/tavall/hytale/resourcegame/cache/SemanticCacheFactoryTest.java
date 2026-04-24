package com.tavall.hytale.resourcegame.cache;

import com.tavall.hytale.resourcegame.config.CacheConfig;
import org.junit.jupiter.api.Test;
import org.tavall.abstractcache.semantic.SemanticCache;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class SemanticCacheFactoryTest {
    @Test
    void buildFallsBackToMemoryWhenRedisIsUnreachable() {
        SemanticCacheFactory factory = new SemanticCacheFactory(new CacheConfig("127.0.0.1", 1, "", false));

        SemanticCache cache = factory.build("test-cache");

        assertNotNull(cache);
    }
}
