package org.tavall.hytale.resourcegame.domain.interior;

import java.util.UUID;

public class InteriorSession {

  private final UUID playerId;
  private final UUID castleId;
  private final String worldId;
  private boolean active;

  public InteriorSession(UUID playerId, UUID castleId, String worldId) {
    this.playerId = playerId;
    this.castleId = castleId;
    this.worldId = worldId;
    this.active = true;
  }

  public UUID playerId() {
    return playerId;
  }

  public UUID castleId() {
    return castleId;
  }

  public String worldId() {
    return worldId;
  }

  public boolean active() {
    return active;
  }

  public void deactivate() {
    this.active = false;
  }
}
