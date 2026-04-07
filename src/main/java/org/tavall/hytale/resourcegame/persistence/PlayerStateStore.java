package org.tavall.hytale.resourcegame.persistence;

import java.util.Optional;
import java.util.UUID;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;

/**
 * Repository contract for player aggregate hydration and persistence.
 */
public interface PlayerStateStore {

  Optional<PlayerStateBundle> load(UUID playerId);

  PlayerStateBundle save(PlayerStateBundle bundle);
}
