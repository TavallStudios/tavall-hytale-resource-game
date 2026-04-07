package org.tavall.hytale.resourcegame.service;

import java.util.Optional;
import java.util.UUID;
import org.tavall.hytale.resourcegame.runtime.HytaleRuntimeGateway;
import org.tavall.hytale.resourcegame.runtime.WorldPosition;
import org.tavall.hytale.resourcegame.service.model.CastleInteractionContext;

/**
 * Detects whether the player is near and looking at their own castle anchor.
 */
public class CastleInteractionService {

  private static final double SELECT_RANGE = 8.0D;

  private final CastleAnchorRegistry castleAnchorRegistry;
  private final HytaleRuntimeGateway runtimeGateway;

  public CastleInteractionService(CastleAnchorRegistry castleAnchorRegistry, HytaleRuntimeGateway runtimeGateway) {
    this.castleAnchorRegistry = castleAnchorRegistry;
    this.runtimeGateway = runtimeGateway;
  }

  public Optional<CastleInteractionContext> inspect(UUID playerId) {
    return castleAnchorRegistry.find(playerId).flatMap(anchor -> {
      WorldPosition playerPosition = runtimeGateway.lookupPlayerPosition(playerId);
      WorldPosition castlePosition = new WorldPosition(
          anchor.castleRecord().location().worldId(),
          anchor.castleRecord().location().x(),
          anchor.castleRecord().location().y(),
          anchor.castleRecord().location().z()
      );
      double distance = runtimeGateway.distance(playerPosition, castlePosition);
      if (distance > SELECT_RANGE) {
        return Optional.empty();
      }
      boolean looking = runtimeGateway.playerLooksAtEntity(playerId, anchor.worldEntityId());
      if (!looking) {
        return Optional.empty();
      }
      return Optional.of(new CastleInteractionContext(playerId, anchor.castleRecord(), anchor.worldEntityId()));
    });
  }
}
