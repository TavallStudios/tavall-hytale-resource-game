package org.tavall.hytale.resourcegame.support;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;
import org.tavall.hytale.resourcegame.persistence.PlayerStateStore;

public class LatchPlayerStateStore implements PlayerStateStore {

  private final CountDownLatch latch = new CountDownLatch(1);

  @Override
  public Optional<PlayerStateBundle> load(UUID playerId) {
    return Optional.empty();
  }

  @Override
  public PlayerStateBundle save(PlayerStateBundle bundle) {
    try {
      latch.await(5, TimeUnit.SECONDS);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting", exception);
    }
    return bundle;
  }

  public void release() {
    latch.countDown();
  }
}
