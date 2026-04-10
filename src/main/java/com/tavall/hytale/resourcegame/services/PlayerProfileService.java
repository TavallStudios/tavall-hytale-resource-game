package com.tavall.hytale.resourcegame.services;

import com.tavall.hytale.resourcegame.cache.CacheKeyFactory;
import com.tavall.hytale.resourcegame.cache.JacksonCacheCodec;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerProfileService;
import com.tavall.hytale.resourcegame.domain.PlayerProfile;
import com.tavall.hytale.resourcegame.persistence.PlayerProfileStore;
import org.tavall.abstractcache.cache.interfaces.ICacheValue;
import org.tavall.abstractcache.semantic.SemanticCache;
import org.tavall.abstractcache.semantic.model.SemanticCacheKey;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Loads and caches player profile data.
 */
public final class PlayerProfileService implements IPlayerProfileService, IDependencyInjectableConcrete {
    private static final Logger LOGGER = Logger.getLogger(PlayerProfileService.class.getName());
    private static final Duration PROFILE_TTL = Duration.ofMinutes(30);

    private final PlayerProfileStore repository;
    private final SemanticCache cache;
    private final JacksonCacheCodec<PlayerProfile> codec;

    public PlayerProfileService(PlayerProfileStore repository, SemanticCache cache, JacksonCacheCodec<PlayerProfile> codec) {
        this.repository = repository;
        this.cache = cache;
        this.codec = codec;
    }

    public Optional<PlayerProfile> readCached(UUID playerId) {
        SemanticCacheKey key = CacheKeyFactory.playerProfileKey(playerId.toString());
        Optional<ICacheValue<PlayerProfile>> cached = cache.get(key, codec);
        return cached.map(ICacheValue::getValue);
    }

    public PlayerProfile loadOrCreate(UUID playerId, String name, String timezone, String ipHash, Instant now) {
        Optional<PlayerProfile> cached = readCached(playerId);
        if (cached.isPresent()) {
            LOGGER.info(() -> "Player profile cache hit for " + playerId + ".");
            return cached.get();
        }

        try {
            Optional<PlayerProfile> existing = repository.findByUuid(playerId);
            if (existing.isPresent()) {
                LOGGER.info(() -> "Player profile repository hit for " + playerId + ".");
            } else {
                LOGGER.info(() -> "Creating new player profile for " + playerId + ".");
            }
            PlayerProfile profile = existing.orElseGet(() -> {
                try {
                    return repository.upsert(playerId, name, timezone, ipHash, now);
                } catch (Exception ex) {
                    throw new IllegalStateException("Failed to create player profile", ex);
                }
            });
            PlayerProfile refreshed = repository.upsert(playerId, name, timezone, ipHash, now);
            cache.put(CacheKeyFactory.playerProfileKey(playerId.toString()), refreshed, PROFILE_TTL, codec);
            return refreshed;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load player profile", ex);
        }
    }

    public void persist(PlayerProfile profile, Instant now) {
        try {
            PlayerProfile refreshed = repository.upsert(profile.uuid(), profile.name(), profile.timezone(), profile.ipHash(), now);
            cache.put(CacheKeyFactory.playerProfileKey(profile.uuid().toString()), refreshed, PROFILE_TTL, codec);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to persist profile", ex);
        }
    }
}
