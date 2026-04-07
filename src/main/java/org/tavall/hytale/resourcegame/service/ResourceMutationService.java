package org.tavall.hytale.resourcegame.service;

import java.time.Instant;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;
import org.tavall.hytale.resourcegame.domain.resource.ResourceType;

public class ResourceMutationService {

  public void add(PlayerStateBundle bundle, ResourceType type, int amount) {
    bundle.gameState().resources().add(type, amount);
    bundle.gameState().touchUpdatedAt(Instant.now());
  }

  public void set(PlayerStateBundle bundle, ResourceType type, int amount) {
    bundle.gameState().resources().set(type, amount);
    bundle.gameState().touchUpdatedAt(Instant.now());
  }
}
