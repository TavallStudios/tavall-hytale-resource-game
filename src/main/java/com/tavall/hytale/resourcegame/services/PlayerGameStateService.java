package com.tavall.hytale.resourcegame.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tavall.hytale.resourcegame.cache.CacheKeyFactory;
import com.tavall.hytale.resourcegame.cache.JacksonCacheCodec;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerGameStateService;
import com.tavall.hytale.resourcegame.domain.AgingState;
import com.tavall.hytale.resourcegame.domain.AccountProgression;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.CitizenMetaData;
import com.tavall.hytale.resourcegame.domain.CitizenJobType;
import com.tavall.hytale.resourcegame.domain.CastleBuildingData;
import com.tavall.hytale.resourcegame.domain.DebugModeState;
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
    private static final String DEFAULT_CASTLE_ASSET_TYPE = "stone_column_castle";

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
        try {
            Optional<ICacheValue<PlayerGameState>> cached = cache.get(CacheKeyFactory.playerGameStateKey(playerId.toString()), codec);
            return cached.map(ICacheValue::getValue);
        } catch (Exception ex) {
            LOGGER.warning(() -> "Player game-state cache read failed for " + playerId + ". Falling back to persistence. " + ex.getMessage());
            return Optional.empty();
        }
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
                state = state.withCastleLocation(
                        spawnLocation,
                        state.castleId() == null ? UUID.randomUUID() : state.castleId(),
                        defaultCastleAssetType(state),
                        now
                );
            }
            if (state.castleAssetType() == null || state.castleAssetType().isBlank()) {
                state = state.withCastleAssetType(DEFAULT_CASTLE_ASSET_TYPE, now);
            }
            state = refreshAging(hydrateMetadata(state, now), now);
            PlayerGameState persisted = persistState(state, now);
            cacheState(playerId, persisted);
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
        try {
            cache.put(CacheKeyFactory.playerGameStateKey(playerId.toString()), state, STATE_TTL, codec);
        } catch (Exception ex) {
            LOGGER.warning(() -> "Player game-state cache write failed for " + playerId + ". " + ex.getMessage());
        }
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

    @Override
    public PlayerGameState resetOnboardingProgress(PlayerGameState state, Instant now) {
        return rewriteMetadata(state, OnboardingProgress.defaults(), now);
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
                DEFAULT_CASTLE_ASSET_TYPE,
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
        GameStateMetadata metadata = GameStateMetadata.fromPopulation(
                state.populationSummary(),
                progress,
                existingMetadata.accountProgression(),
                existingMetadata.debugModeState(),
                existingMetadata.resourceNodes(),
                existingMetadata.castleBuildings(),
                existingMetadata.interiorInstanceIndex()
        );
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
            return rewriteMetadata(
                    hydrated,
                    metadata.onboardingProgress(),
                    metadata.accountProgression(),
                    metadata.debugModeState(),
                    metadata.resourceNodes(),
                    metadata.castleBuildings(),
                    metadata.interiorInstanceIndex(),
                    now
            );
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
            return GameStateMetadata.fromPopulation(state.populationSummary(), OnboardingProgress.defaults(), List.of(), List.of(), 0);
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
                    decoded.accountProgression(),
                    decoded.debugModeState(),
                    decoded.resourceNodes(),
                    decoded.castleBuildings(),
                    decoded.interiorInstanceIndex()
            );
        } catch (Exception ex) {
            LOGGER.warning(() -> "Failed to decode game state metadata. Falling back to defaults. " + ex.getMessage());
            return GameStateMetadata.fromPopulation(state.populationSummary(), OnboardingProgress.defaults(), List.of(), List.of(), 0);
        }
    }

    private PlayerGameState rewriteMetadata(PlayerGameState state, OnboardingProgress onboardingProgress, Instant now) {
        GameStateMetadata metadata = metadataOf(state, now);
        return rewriteMetadata(
                state,
                onboardingProgress,
                metadata.accountProgression(),
                metadata.debugModeState(),
                metadata.resourceNodes(),
                metadata.castleBuildings(),
                metadata.interiorInstanceIndex(),
                now
        );
    }

    private PlayerGameState rewriteMetadata(
            PlayerGameState state,
            OnboardingProgress onboardingProgress,
            AccountProgression accountProgression,
            DebugModeState debugModeState,
            List<ResourceNodeData> resourceNodes,
            List<CastleBuildingData> castleBuildings,
            int interiorInstanceIndex,
            Instant now
    ) {
        try {
            GameStateMetadata metadata = GameStateMetadata.fromPopulation(
                    state.populationSummary(),
                    onboardingProgress,
                    accountProgression,
                    debugModeState,
                    resourceNodes,
                    castleBuildings,
                    interiorInstanceIndex
            );
            String json = objectMapper.writeValueAsString(metadata);
            return state.withMetadataJson(json, now);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to update player game state metadata", ex);
        }
    }

    private String defaultCastleAssetType(PlayerGameState state) {
        if (state.castleAssetType() != null && !state.castleAssetType().isBlank()) {
            return state.castleAssetType();
        }
        return DEFAULT_CASTLE_ASSET_TYPE;
    }

    @Override
    public int interiorInstanceIndex(PlayerGameState state) {
        if (state == null) {
            return 0;
        }
        GameStateMetadata metadata = metadataOf(state, resolveNow(state));
        return Math.max(0, metadata.interiorInstanceIndex());
    }

    @Override
    public PlayerGameState bumpInteriorInstanceIndex(PlayerGameState state, Instant now) {
        if (state == null) {
            return null;
        }
        Instant effectiveNow = now == null ? Instant.now() : now;
        GameStateMetadata metadata = metadataOf(state, effectiveNow);
        int nextIndex = metadata.interiorInstanceIndex() + 1;
        return rewriteMetadata(
                state,
                metadata.onboardingProgress(),
                metadata.accountProgression(),
                metadata.debugModeState(),
                metadata.resourceNodes(),
                metadata.castleBuildings(),
                nextIndex,
                effectiveNow
        );
    }

    @Override
    public AccountProgression accountProgression(PlayerGameState state) {
        if (state == null) {
            return AccountProgression.defaults();
        }
        return metadataOf(state, resolveNow(state)).accountProgression();
    }

    @Override
    public PlayerGameState setAccountLevel(PlayerGameState state, int level, Instant now) {
        if (state == null) {
            return null;
        }
        Instant effectiveNow = now == null ? Instant.now() : now;
        GameStateMetadata metadata = metadataOf(state, effectiveNow);
        return rewriteMetadata(
                state,
                metadata.onboardingProgress(),
                metadata.accountProgression().withLevel(level),
                metadata.debugModeState(),
                metadata.resourceNodes(),
                metadata.castleBuildings(),
                metadata.interiorInstanceIndex(),
                effectiveNow
        );
    }

    @Override
    public PlayerGameState addAccountExperience(PlayerGameState state, int experience, Instant now) {
        if (state == null) {
            return null;
        }
        Instant effectiveNow = now == null ? Instant.now() : now;
        GameStateMetadata metadata = metadataOf(state, effectiveNow);
        return rewriteMetadata(
                state,
                metadata.onboardingProgress(),
                metadata.accountProgression().withAddedExperience(experience),
                metadata.debugModeState(),
                metadata.resourceNodes(),
                metadata.castleBuildings(),
                metadata.interiorInstanceIndex(),
                effectiveNow
        );
    }

    @Override
    public DebugModeState debugModeState(PlayerGameState state) {
        if (state == null) {
            return DebugModeState.disabled();
        }
        return metadataOf(state, resolveNow(state)).debugModeState();
    }

    @Override
    public PlayerGameState setDebugMode(PlayerGameState state, DebugModeState debugModeState, Instant now) {
        if (state == null) {
            return null;
        }
        Instant effectiveNow = now == null ? Instant.now() : now;
        GameStateMetadata metadata = metadataOf(state, effectiveNow);
        return rewriteMetadata(
                state,
                metadata.onboardingProgress(),
                metadata.accountProgression(),
                debugModeState == null ? DebugModeState.disabled() : debugModeState,
                metadata.resourceNodes(),
                metadata.castleBuildings(),
                metadata.interiorInstanceIndex(),
                effectiveNow
        );
    }

    private Instant resolveNow(PlayerGameState state) {
        if (state == null) {
            return Instant.now();
        }
        return state.updatedAt() == null ? Instant.now() : state.updatedAt();
    }
}
