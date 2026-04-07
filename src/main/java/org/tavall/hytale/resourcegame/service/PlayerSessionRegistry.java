package org.tavall.hytale.resourcegame.service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;

/**
 * Active-session registry used by commands and interaction services.
 */
public class PlayerSessionRegistry {

  private final ConcurrentHashMap<UUID, PlayerStateBundle> sessionsByPlayerId = new ConcurrentHashMap<>();

  public Optional<PlayerStateBundle> find(UUID playerId) {
    return Optional.ofNullable(sessionsByPlayerId.get(playerId));
  }

  public void put(PlayerStateBundle bundle) {
    sessionsByPlayerId.put(bundle.playerId(), bundle);
  }

  public Map<UUID, PlayerStateBundle> snapshot() {
    return Map.copyOf(sessionsByPlayerId);
  }
}
