package org.tavall.hytale.resourcegame.support;

import java.util.Optional;
import java.util.UUID;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;
import org.tavall.hytale.resourcegame.persistence.PlayerStateStore;

public class EmptyPlayerStateStore implements PlayerStateStore {

  @Override
  public Optional<PlayerStateBundle> load(UUID playerId) {
    return Optional.empty();
  }

  @Override
  public PlayerStateBundle save(PlayerStateBundle bundle) {
    return bundle;
  }
}
