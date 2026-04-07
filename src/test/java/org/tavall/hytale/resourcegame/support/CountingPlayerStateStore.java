package org.tavall.hytale.resourcegame.support;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;
import org.tavall.hytale.resourcegame.persistence.PlayerStateStore;

public class CountingPlayerStateStore implements PlayerStateStore {

  private final PlayerStateStore delegate;
  private final AtomicInteger loadCalls = new AtomicInteger();
  private final AtomicInteger saveCalls = new AtomicInteger();

  public CountingPlayerStateStore(PlayerStateStore delegate) {
    this.delegate = delegate;
  }

  @Override
  public Optional<PlayerStateBundle> load(UUID playerId) {
    loadCalls.incrementAndGet();
    return delegate.load(playerId);
  }

  @Override
  public PlayerStateBundle save(PlayerStateBundle bundle) {
    saveCalls.incrementAndGet();
    return delegate.save(bundle);
  }

  public int loadCalls() {
    return loadCalls.get();
  }

  public int saveCalls() {
    return saveCalls.get();
  }
}
