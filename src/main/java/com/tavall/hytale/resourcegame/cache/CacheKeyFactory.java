package com.tavall.hytale.resourcegame.cache;

import org.tavall.abstractcache.cache.enums.CacheDomain;
import org.tavall.abstractcache.cache.enums.CacheSource;
import org.tavall.abstractcache.cache.enums.CacheVersion;
import org.tavall.abstractcache.semantic.model.CacheTag;
import org.tavall.abstractcache.semantic.model.SemanticCacheKey;

import java.util.Set;

/**
 * Generates semantic cache keys for player data.
 */
public final class CacheKeyFactory {
    private CacheKeyFactory() {
    }

    public static SemanticCacheKey playerProfileKey(String uuid) {
        return new SemanticCacheKey(
                "player-profile:" + uuid,
                CacheDomain.PLAYER_PROFILE,
                CacheSource.GLOBAL,
                CacheVersion.V1_0,
                Set.of(CacheTag.of("profile"))
        );
    }

    public static SemanticCacheKey playerGameStateKey(String uuid) {
        return new SemanticCacheKey(
                "player-game-state:" + uuid,
                CacheDomain.KINGDOMS,
                CacheSource.GLOBAL,
                CacheVersion.V1_0,
                Set.of(CacheTag.of("game-state"))
        );
    }
}
