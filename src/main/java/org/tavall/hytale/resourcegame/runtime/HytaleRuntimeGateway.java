package org.tavall.hytale.resourcegame.runtime;

import java.util.UUID;
import org.tavall.hytale.resourcegame.domain.ui.UiScreen;

/**
 * Boundary adapter for spawning entities and opening UIs with the Hytale runtime.
 */
public interface HytaleRuntimeGateway {

  UUID spawnEntity(HytaleAssetId assetId, WorldPosition position, String displayLabel);

  void updateEntityLabel(UUID entityId, String displayLabel);

  void removeEntity(UUID entityId);

  double distance(WorldPosition first, WorldPosition second);

  WorldPosition lookupPlayerPosition(UUID playerId);

  boolean playerLooksAtEntity(UUID playerId, UUID entityId);

  void openUi(UUID playerId, UiScreen screen);

  void movePlayer(UUID playerId, WorldPosition nextPosition);

  String currentWorld(UUID playerId);
}
