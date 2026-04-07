package org.tavall.hytale.resourcegame.runtime;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.tavall.hytale.resourcegame.domain.ui.UiScreen;

/**
 * Local runtime stub that mimics core Hytale interactions for development and tests.
 */
public class InMemoryHytaleRuntimeGateway implements HytaleRuntimeGateway {

  private final ConcurrentHashMap<UUID, RuntimeEntity> entitiesById = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<UUID, WorldPosition> playerPositionById = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<UUID, UUID> playerLookTargetById = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<UUID, UiScreen> openedUiByPlayerId = new ConcurrentHashMap<>();

  @Override
  public UUID spawnEntity(HytaleAssetId assetId, WorldPosition position, String displayLabel) {
    UUID entityId = UUID.randomUUID();
    entitiesById.put(entityId, new RuntimeEntity(entityId, assetId, position, displayLabel));
    return entityId;
  }

  @Override
  public void updateEntityLabel(UUID entityId, String displayLabel) {
    RuntimeEntity entity = entitiesById.get(entityId);
    if (entity == null) {
      return;
    }
    entitiesById.put(entityId, new RuntimeEntity(entity.entityId(), entity.assetId(), entity.position(), displayLabel));
  }

  @Override
  public void removeEntity(UUID entityId) {
    entitiesById.remove(entityId);
  }

  @Override
  public double distance(WorldPosition first, WorldPosition second) {
    if (!first.worldId().equals(second.worldId())) {
      return Double.MAX_VALUE;
    }
    double deltaX = first.x() - second.x();
    double deltaY = first.y() - second.y();
    double deltaZ = first.z() - second.z();
    return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
  }

  @Override
  public WorldPosition lookupPlayerPosition(UUID playerId) {
    return playerPositionById.get(playerId);
  }

  @Override
  public boolean playerLooksAtEntity(UUID playerId, UUID entityId) {
    return entityId.equals(playerLookTargetById.get(playerId));
  }

  @Override
  public void openUi(UUID playerId, UiScreen screen) {
    openedUiByPlayerId.put(playerId, screen);
  }

  @Override
  public void movePlayer(UUID playerId, WorldPosition nextPosition) {
    playerPositionById.put(playerId, nextPosition);
  }

  @Override
  public String currentWorld(UUID playerId) {
    WorldPosition position = playerPositionById.get(playerId);
    return position == null ? null : position.worldId();
  }

  public void setPlayerLookTarget(UUID playerId, UUID entityId) {
    playerLookTargetById.put(playerId, entityId);
  }

  public Optional<UiScreen> lastOpenedUi(UUID playerId) {
    return Optional.ofNullable(openedUiByPlayerId.get(playerId));
  }

  public Optional<RuntimeEntity> findEntity(UUID entityId) {
    return Optional.ofNullable(entitiesById.get(entityId));
  }

  public Map<UUID, RuntimeEntity> entitySnapshot() {
    return Map.copyOf(entitiesById);
  }
}
