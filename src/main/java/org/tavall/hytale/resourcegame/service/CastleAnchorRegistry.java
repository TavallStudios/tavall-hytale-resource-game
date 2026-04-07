package org.tavall.hytale.resourcegame.service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.tavall.hytale.resourcegame.domain.castle.CastleRecord;

/**
 * In-memory index mapping player IDs to spawned castle world entities.
 */
public class CastleAnchorRegistry {

  private final ConcurrentHashMap<UUID, CastleAnchorPresence> anchorsByPlayerId = new ConcurrentHashMap<>();

  public Optional<CastleAnchorPresence> find(UUID playerId) {
    return Optional.ofNullable(anchorsByPlayerId.get(playerId));
  }

  public void put(UUID playerId, CastleRecord castleRecord, UUID worldEntityId) {
    anchorsByPlayerId.put(playerId, new CastleAnchorPresence(castleRecord, worldEntityId));
  }
}
