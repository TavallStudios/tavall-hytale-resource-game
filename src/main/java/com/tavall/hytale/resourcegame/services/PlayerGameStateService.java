package com.tavall.hytale.resourcegame.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tavall.hytale.resourcegame.cache.CacheKeyFactory;
import com.tavall.hytale.resourcegame.cache.JacksonCacheCodec;
import com.tavall.hytale.resourcegame.domain.AgingState;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.GameStateMetadata;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.PopulationSummary;
import com.tavall.hytale.resourcegame.domain.ResourceInventory;
import com.tavall.hytale.resourcegame.persistence.PlayerGameStateStore;
import com.tavall.hytale.resourcegame.persistence.PopulationSummaryDefaults;
import org.tavall.abstractcache.cache.interfaces.ICacheValue;
import org.tavall.abstractcache.semantic.SemanticCache;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Loads and persists player game state with cache support.
 */
public final class PlayerGameStateService {
    private static final Logger LOGGER = Logger.getLogger(PlayerGameStateService.class.getName());
    private static final Duration STATE_TTL = Duration.ofMinutes(15);

    private final PlayerGameStateStore repository;
    private final SemanticCache cache;
    private final JacksonCacheCodec<PlayerGameState> codec;
    private final ObjectMapper objectMapper;

    public PlayerGameStateService(
            PlayerGameStateStore repository,
            SemanticCache cache,
            JacksonCacheCodec<PlayerGameState> codec,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.cache = cache;
        this.codec = codec;
        this.objectMapper = objectMapper;
    }

    public Optional<PlayerGameState> readCached(UUID playerId) {
        Optional<ICacheValue<PlayerGameState>> cached = cache.get(CacheKeyFactory.playerGameStateKey(playerId.toString()), codec);
        return cached.map(ICacheValue::getValue);
    }

    public PlayerGameState loadOrCreate(long profileId, UUID playerId, CastleLocationData spawnLocation, Instant now) {
        Optional<PlayerGameState> cached = readCached(playerId);
        if (cached.isPresent()) {
            LOGGER.info(() -> "Player game state cache hit for " + playerId + ".");
            return cached.get();
        }

        try {
            Optional<PlayerGameState> existing = repository.findByProfileId(profileId);
            if (existing.isPresent()) {
                LOGGER.info(() -> "Player game state repository hit for profile " + profileId + ".");
            } else {
                LOGGER.info(() -> "Creating default player game state for profile " + profileId + ".");
            }
            PlayerGameState state = existing.orElseGet(() -> createDefaultState(profileId, spawnLocation, now));
            if (state.castleLocation() == null && spawnLocation != null) {
                state = state.withCastleLocation(spawnLocation, state.castleId() == null ? UUID.randomUUID() : state.castleId(), now);
            }
            state = refreshAging(state, now);
            PlayerGameState persisted = persistState(state, now);
            cache.put(CacheKeyFactory.playerGameStateKey(playerId.toString()), persisted, STATE_TTL, codec);
            return persisted;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load player game state", ex);
        }
    }

    public PlayerGameState persistState(PlayerGameState state, Instant now) {
        try {
            PlayerGameState prepared = withMetadata(state);
            return repository.upsert(prepared, now);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to persist player game state", ex);
        }
    }

    public void cacheState(UUID playerId, PlayerGameState state) {
        cache.put(CacheKeyFactory.playerGameStateKey(playerId.toString()), state, STATE_TTL, codec);
    }

    private PlayerGameState createDefaultState(long profileId, CastleLocationData spawnLocation, Instant now) {
        PopulationSummary populationSummary = new PopulationSummary(
                12,
                0,
                PopulationSummaryDefaults.citizenMetaData(),
                PopulationSummaryDefaults.troopMetaData(),
                AgingState.defaults(now)
        );
        ResourceInventory resources = ResourceInventory.starterPack();
        return new PlayerGameState(
                0,
                profileId,
                UUID.randomUUID(),
                spawnLocation,
                populationSummary,
                resources,
                null,
                null,
                now,
                now
        );
    }

    private PlayerGameState withMetadata(PlayerGameState state) throws JsonProcessingException {
        GameStateMetadata metadata = GameStateMetadata.fromPopulation(state.populationSummary());
        String json = objectMapper.writeValueAsString(metadata);
        return state.withMetadataJson(json, Instant.now());
    }

    private PlayerGameState refreshAging(PlayerGameState state, Instant now) {
        PopulationSummary summary = state.populationSummary();
        PopulationSummary updated = summary.withAgingState(summary.agingState().tick(now));
        return state.withPopulation(updated, now);
    }
}
