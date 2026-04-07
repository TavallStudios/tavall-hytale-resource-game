package org.tavall.hytale.resourcegame.service;

import java.util.concurrent.CompletableFuture;
import org.tavall.hytale.resourcegame.domain.player.PlayerJoinRequest;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;

/**
 * First-entry orchestrator for hydration and temporary castle placement.
 */
public class PlayerInitializationService {

  private final PlayerStateGateway playerStateGateway;
  private final CastleSpawnService castleSpawnService;
  private final PlayerSessionRegistry playerSessionRegistry;

  public PlayerInitializationService(
      PlayerStateGateway playerStateGateway,
      CastleSpawnService castleSpawnService,
      PlayerSessionRegistry playerSessionRegistry
  ) {
    this.playerStateGateway = playerStateGateway;
    this.castleSpawnService = castleSpawnService;
    this.playerSessionRegistry = playerSessionRegistry;
  }

  /**
   * Hydrates or creates player state, then ensures first-pass castle placement at current location.
   */
  public CompletableFuture<PlayerStateBundle> initialize(PlayerJoinRequest joinRequest) {
    return playerStateGateway.hydrate(joinRequest)
        .thenCompose(bundle -> {
          castleSpawnService.ensureCastleSpawned(bundle, joinRequest.joinPosition());
          playerSessionRegistry.put(bundle);
          return playerStateGateway.persist(bundle);
        })
        .thenApply(bundle -> {
          playerSessionRegistry.put(bundle);
          return bundle;
        });
  }
}
