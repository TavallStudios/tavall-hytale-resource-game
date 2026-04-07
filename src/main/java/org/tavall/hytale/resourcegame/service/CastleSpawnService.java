package org.tavall.hytale.resourcegame.service;

import java.util.UUID;
import org.tavall.hytale.resourcegame.domain.castle.CastleLocation;
import org.tavall.hytale.resourcegame.domain.castle.CastleRecord;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;
import org.tavall.hytale.resourcegame.runtime.HytaleRuntimeGateway;
import org.tavall.hytale.resourcegame.runtime.WorldPosition;

/**
 * Spawns castle placeholders and keeps runtime entity anchors mapped for interaction.
 */
public class CastleSpawnService {

  private final CastleVisualCatalog visualCatalog;
  private final HytaleRuntimeGateway runtimeGateway;
  private final CastleAnchorRegistry castleAnchorRegistry;

  public CastleSpawnService(
      CastleVisualCatalog visualCatalog,
      HytaleRuntimeGateway runtimeGateway,
      CastleAnchorRegistry castleAnchorRegistry
  ) {
    this.visualCatalog = visualCatalog;
    this.runtimeGateway = runtimeGateway;
    this.castleAnchorRegistry = castleAnchorRegistry;
  }

  public CastleRecord ensureCastleSpawned(PlayerStateBundle bundle, WorldPosition preferredPosition) {
    if (bundle.gameState().castleId() != null && bundle.gameState().castleLocation() != null) {
      CastleRecord existing = new CastleRecord(
          bundle.gameState().castleId(),
          bundle.profile().internalPlayerId(),
          bundle.gameState().castleLocation(),
          visualCatalog.initialCastleAsset()
      );
      ensureRuntimeAnchor(bundle.playerId(), existing);
      return existing;
    }
    UUID castleId = UUID.randomUUID();
    CastleLocation location = new CastleLocation(
        preferredPosition.worldId(),
        preferredPosition.x(),
        preferredPosition.y(),
        preferredPosition.z()
    );
    bundle.gameState().assignCastle(castleId, location, java.time.Instant.now());
    CastleRecord created = new CastleRecord(castleId, bundle.profile().internalPlayerId(), location, visualCatalog.initialCastleAsset());
    ensureRuntimeAnchor(bundle.playerId(), created);
    return created;
  }

  private void ensureRuntimeAnchor(UUID playerId, CastleRecord castleRecord) {
    castleAnchorRegistry.find(playerId).ifPresentOrElse(existing -> {
      if (existing.castleRecord().castleId().equals(castleRecord.castleId())) {
        return;
      }
      runtimeGateway.removeEntity(existing.worldEntityId());
      spawnAnchor(playerId, castleRecord);
    }, () -> spawnAnchor(playerId, castleRecord));
  }

  private void spawnAnchor(UUID playerId, CastleRecord castleRecord) {
    WorldPosition position = new WorldPosition(
        castleRecord.location().worldId(),
        castleRecord.location().x(),
        castleRecord.location().y(),
        castleRecord.location().z()
    );
    UUID entityId = runtimeGateway.spawnEntity(castleRecord.visualAsset(), position, "Your Castle");
    castleAnchorRegistry.put(playerId, castleRecord, entityId);
  }
}
