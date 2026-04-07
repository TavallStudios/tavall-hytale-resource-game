package org.tavall.hytale.resourcegame.runtime;

public record WorldPosition(String worldId, double x, double y, double z) {

  public WorldPosition withWorld(String nextWorldId) {
    return new WorldPosition(nextWorldId, x, y, z);
  }
}
