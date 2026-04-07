package org.tavall.hytale.resourcegame.service;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.tavall.hytale.resourcegame.cache.CacheDomain;
import org.tavall.hytale.resourcegame.cache.CacheSource;
import org.tavall.hytale.resourcegame.cache.CacheType;
import org.tavall.hytale.resourcegame.cache.PlayerHotStateCache;
import org.tavall.hytale.resourcegame.concurrent.AsyncTask;
import org.tavall.hytale.resourcegame.domain.player.PlayerJoinRequest;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;
import org.tavall.hytale.resourcegame.persistence.PlayerStateStore;

/**
 * Redis-first + Postgres fallback player state pipeline with hot in-memory cache.
 */
public class PlayerStateGateway {

  private final PlayerHotStateCache hotStateCache;
  private final PlayerStateStore redisStore;
  private final PlayerStateStore postgresStore;
  private final PlayerStateFactory stateFactory;
  private final PopulationMetadataCodec populationMetadataCodec;

  public PlayerStateGateway(
      PlayerHotStateCache hotStateCache,
      PlayerStateStore redisStore,
      PlayerStateStore postgresStore,
      PlayerStateFactory stateFactory,
      PopulationMetadataCodec populationMetadataCodec
  ) {
    this.hotStateCache = hotStateCache;
    this.redisStore = redisStore;
    this.postgresStore = postgresStore;
    this.stateFactory = stateFactory;
    this.populationMetadataCodec = populationMetadataCodec;
  }

  /**
   * Hydrates state off the game loop using memory, then Redis, then Postgres fallback.
   */
  public CompletableFuture<PlayerStateBundle> hydrate(PlayerJoinRequest joinRequest) {
    return AsyncTask.supply("hydrate-player-state", () -> hydrateSync(joinRequest));
  }

  /**
   * Persists state off the game loop to Postgres and Redis, then refreshes hot cache.
   */
  public CompletableFuture<PlayerStateBundle> persist(PlayerStateBundle bundle) {
    return AsyncTask.supply("persist-player-state", () -> persistSync(bundle));
  }

  public Optional<PlayerStateBundle> fromHotCache(PlayerJoinRequest joinRequest) {
    return Optional.ofNullable(hotStateCache.getIfPresent(
        joinRequest.playerId().toString(),
        CacheDomain.PLAYER,
        CacheType.HOT_PLAYER_STATE,
        CacheSource.MEMORY
    ));
  }

  private PlayerStateBundle hydrateSync(PlayerJoinRequest joinRequest) {
    Optional<PlayerStateBundle> hotState = fromHotCache(joinRequest);
    if (hotState.isPresent()) {
      return hotState.get();
    }
    Optional<PlayerStateBundle> redisState = redisStore.load(joinRequest.playerId());
    if (redisState.isPresent()) {
      putHot(redisState.get());
      return redisState.get();
    }
    Optional<PlayerStateBundle> postgresState = postgresStore.load(joinRequest.playerId());
    if (postgresState.isPresent()) {
      PlayerStateBundle refreshed = updateProfileEnvelope(postgresState.get(), joinRequest);
      refreshed.gameState().assignPopulationMetadataJson(
          populationMetadataCodec.encode(refreshed.populationRoster()),
          Instant.now()
      );
      redisStore.save(refreshed);
      putHot(refreshed);
      return refreshed;
    }
    PlayerStateBundle firstJoin = stateFactory.createFirstJoin(joinRequest, Instant.now());
    firstJoin.gameState().assignPopulationMetadataJson(populationMetadataCodec.encode(firstJoin.populationRoster()), Instant.now());
    return persistSync(firstJoin);
  }

  private PlayerStateBundle persistSync(PlayerStateBundle bundle) {
    bundle.gameState().assignPopulationMetadataJson(populationMetadataCodec.encode(bundle.populationRoster()), Instant.now());
    PlayerStateBundle stored = postgresStore.save(bundle);
    redisStore.save(stored);
    putHot(stored);
    return stored;
  }

  private void putHot(PlayerStateBundle bundle) {
    hotStateCache.put(
        bundle.playerId().toString(),
        CacheDomain.PLAYER,
        CacheType.HOT_PLAYER_STATE,
        CacheSource.MEMORY,
        bundle,
        20L * 60L * 1000L
    );
  }

  private PlayerStateBundle updateProfileEnvelope(PlayerStateBundle bundle, PlayerJoinRequest joinRequest) {
    bundle.profile().refreshIdentity(
        joinRequest.playerName(),
        joinRequest.timezone(),
        joinRequest.transformedIp(),
        Instant.now()
    );
    return bundle;
  }
}
