package org.tavall.hytale.resourcegame.cache;

import java.util.concurrent.TimeUnit;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;

public class PlayerHotStateCache extends AbstractCache<PlayerStateBundle> {

  public PlayerHotStateCache(long ttlMinutes) {
    super(ttlMinutes, TimeUnit.MINUTES);
  }
}
