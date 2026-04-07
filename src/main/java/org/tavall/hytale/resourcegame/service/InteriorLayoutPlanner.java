package org.tavall.hytale.resourcegame.service;

import org.tavall.hytale.resourcegame.domain.castle.CastleLocation;
import org.tavall.hytale.resourcegame.domain.interior.InteriorLayoutPlan;
import org.tavall.hytale.resourcegame.runtime.WorldPosition;

public class InteriorLayoutPlanner {

  public InteriorLayoutPlan buildDefaultLayout(String worldId, CastleLocation sourceCastleLocation) {
    double centerX = sourceCastleLocation.x();
    double centerY = sourceCastleLocation.y();
    double centerZ = sourceCastleLocation.z();
    return new InteriorLayoutPlan(
        new WorldPosition(worldId, centerX - 3, centerY + 1, centerZ),
        new WorldPosition(worldId, centerX + 3, centerY + 1, centerZ),
        new WorldPosition(worldId, centerX, centerY + 1, centerZ - 6),
        new WorldPosition(worldId, centerX + 8, centerY + 1, centerZ + 4),
        new WorldPosition(worldId, centerX - 8, centerY + 1, centerZ + 4)
    );
  }
}
