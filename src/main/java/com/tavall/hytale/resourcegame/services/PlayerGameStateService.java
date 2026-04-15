package com.tavall.hytale.resourcegame.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tavall.hytale.resourcegame.cache.CacheKeyFactory;
import com.tavall.hytale.resourcegame.cache.JacksonCacheCodec;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerGameStateService;
import com.tavall.hytale.resourcegame.domain.AgingState;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.CitizenMetaData;
import com.tavall.hytale.resourcegame.domain.CitizenJobType;
import com.tavall.hytale.resourcegame.domain.GameStateMetadata;
import com.tavall.hytale.resourcegame.domain.OnboardingProgress;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.PopulationSummary;
import com.tavall.hytale.resourcegame.domain.ResourceInventory;
import com.tavall.hytale.resourcegame.domain.ResourceNodeData;
import com.tavall.hytale.resourcegame.domain.TroopMetaData;
import com.tavall.hytale.resourcegame.persistence.PlayerGameStateStore;
import com.tavall.hytale.resourcegame.persistence.PopulationSummaryDefaults;
import org.tavall.abstractcache.cache.interfaces.ICacheValue;
import org.tavall.abstractcache.semantic.SemanticCache;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.logging.Logger;

/**
 * Loads and persists player game state with cache support.
 */
public final class PlayerGameStateService implements IPlayerGameStateService, IDependencyInjectableConcrete {
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
            return refreshAging(hydrateMetadata(cached.get(), now), now);
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
            state = refreshAging(hydrateMetadata(state, now), now);
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

    public boolean isInteriorTutorialPending(PlayerGameState state) {
        return metadataOf(state, Instant.now()).onboardingProgress().firstInteriorTutorialPending();
    }

    public boolean isInteriorTourPending(PlayerGameState state) {
        return metadataOf(state, Instant.now()).onboardingProgress().firstInteriorTourPending();
    }

    public boolean isUpgradeTutorialPending(PlayerGameState state) {
        return metadataOf(state, Instant.now()).onboardingProgress().firstUpgradeTutorialPending();
    }

    public PlayerGameState markInteriorTutorialSeen(PlayerGameState state, Instant now) {
        OnboardingProgress progress = metadataOf(state, now).onboardingProgress();
        if (!progress.firstInteriorTutorialPending()) {
            return state;
        }
        return rewriteMetadata(state, progress.markInteriorTutorialSeen(), now);
    }

    public PlayerGameState markInteriorTourSeen(PlayerGameState state, Instant now) {
        OnboardingProgress progress = metadataOf(state, now).onboardingProgress();
        if (!progress.firstInteriorTourPending()) {
            return state;
        }
        return rewriteMetadata(state, progress.markInteriorTourSeen(), now);
    }

    public PlayerGameState markUpgradeTutorialSeen(PlayerGameState state, Instant now) {
        OnboardingProgress progress = metadataOf(state, now).onboardingProgress();
        if (!progress.firstUpgradeTutorialPending()) {
            return state;
        }
        return rewriteMetadata(state, progress.markUpgradeTutorialSeen(), now);
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
        GameStateMetadata existingMetadata = metadataOf(state, state.updatedAt() == null ? Instant.now() : state.updatedAt());
        OnboardingProgress progress = existingMetadata.onboardingProgress();
        GameStateMetadata metadata = GameStateMetadata.fromPopulation(state.populationSummary(), progress, existingMetadata.resourceNodes());
        String json = objectMapper.writeValueAsString(metadata);
        return state.withMetadataJson(json, state.updatedAt() == null ? Instant.now() : state.updatedAt());
    }

    private PlayerGameState refreshAging(PlayerGameState state, Instant now) {
        PopulationSummary summary = state.populationSummary();
        PopulationSummary updated = summary.withAgingState(summary.agingState().tick(now));
        return state.withPopulation(updated, now);
    }

    private PlayerGameState hydrateMetadata(PlayerGameState state, Instant now) {
        GameStateMetadata metadata = metadataOf(state, now);
        PopulationSummary summary = rehydratePopulation(state.populationSummary(), metadata, now);
        PlayerGameState hydrated = state.withPopulation(summary, state.updatedAt() == null ? now : state.updatedAt());
        if (state.metadataJson() == null || state.metadataJson().isBlank()) {
            return rewriteMetadata(hydrated, metadata.onboardingProgress(), metadata.resourceNodes(), now);
        }
        return hydrated;
    }

    private PopulationSummary rehydratePopulation(PopulationSummary summary, GameStateMetadata metadata, Instant now) {
        CitizenMetaData citizens = metadata.citizenMetaData() == null
                ? summary.citizenMetaData()
                : metadata.citizenMetaData().withJobCounts(resolveJobCounts(metadata));
        TroopMetaData troops = metadata.troopMetaData() == null ? summary.troopMetaData() : metadata.troopMetaData();
        AgingState aging = metadata.agingState() == null ? AgingState.defaults(now) : metadata.agingState();
        return new PopulationSummary(summary.citizenCount(), summary.troopCount(), citizens, troops, aging);
    }

    private Map<CitizenJobType, Integer> resolveJobCounts(GameStateMetadata metadata) {
        if (metadata.jobCounts() != null && !metadata.jobCounts().isEmpty()) {
            return metadata.jobCounts();
        }
        if (metadata.citizenMetaData() != null && metadata.citizenMetaData().jobCounts() != null) {
            return metadata.citizenMetaData().jobCounts();
        }
        return PopulationSummaryDefaults.citizenMetaData().jobCounts();
    }

    private GameStateMetadata metadataOf(PlayerGameState state, Instant now) {
        if (state.metadataJson() == null || state.metadataJson().isBlank()) {
            return GameStateMetadata.fromPopulation(state.populationSummary(), OnboardingProgress.defaults(), List.of());
        }
        try {
            GameStateMetadata decoded = objectMapper.readValue(state.metadataJson(), GameStateMetadata.class);
            OnboardingProgress onboarding = decoded.onboardingProgress() == null
                    ? OnboardingProgress.defaults()
                    : decoded.onboardingProgress();
            return new GameStateMetadata(
                    decoded.citizenMetaData(),
                    decoded.troopMetaData(),
                    decoded.agingState() == null ? AgingState.defaults(now) : decoded.agingState(),
                    resolveJobCounts(decoded),
                    onboarding,
                    decoded.resourceNodes()
            );
        } catch (Exception ex) {
            LOGGER.warning(() -> "Failed to decode game state metadata. Falling back to defaults. " + ex.getMessage());
            return GameStateMetadata.fromPopulation(state.populationSummary(), OnboardingProgress.defaults(), List.of());
        }
    }

    private PlayerGameState rewriteMetadata(PlayerGameState state, OnboardingProgress onboardingProgress, Instant now) {
        return rewriteMetadata(state, onboardingProgress, metadataOf(state, now).resourceNodes(), now);
    }

    private PlayerGameState rewriteMetadata(
            PlayerGameState state,
            OnboardingProgress onboardingProgress,
            List<ResourceNodeData> resourceNodes,
            Instant now
    ) {
        try {
            GameStateMetadata metadata = GameStateMetadata.fromPopulation(state.populationSummary(), onboardingProgress, resourceNodes);
            String json = objectMapper.writeValueAsString(metadata);
            return state.withMetadataJson(json, now);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to update player game state metadata", ex);
        }
    }
}
