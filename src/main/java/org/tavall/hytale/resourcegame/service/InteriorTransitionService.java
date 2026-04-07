package org.tavall.hytale.resourcegame.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.tavall.hytale.resourcegame.domain.interior.InteriorLayoutPlan;
import org.tavall.hytale.resourcegame.domain.interior.InteriorSession;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;
import org.tavall.hytale.resourcegame.runtime.HytaleRuntimeGateway;
import org.tavall.hytale.resourcegame.runtime.WorldPosition;

/**
 * Handles same-process interior entry and exit while preserving clean session metadata.
 */
public class InteriorTransitionService {

  private final HytaleRuntimeGateway runtimeGateway;
  private final InteriorLayoutPlanner layoutPlanner;
  private final ConcurrentHashMap<UUID, InteriorSession> sessionsByPlayerId = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<UUID, InteriorLayoutPlan> layoutByPlayerId = new ConcurrentHashMap<>();

  public InteriorTransitionService(HytaleRuntimeGateway runtimeGateway, InteriorLayoutPlanner layoutPlanner) {
    this.runtimeGateway = runtimeGateway;
    this.layoutPlanner = layoutPlanner;
  }

  public InteriorSession enterInterior(PlayerStateBundle bundle) {
    if (bundle.gameState().castleId() == null || bundle.gameState().castleLocation() == null) {
      throw new IllegalStateException("Cannot enter interior before castle placement");
    }
    String worldId = "interior-" + bundle.gameState().castleId();
    InteriorSession session = new InteriorSession(bundle.playerId(), bundle.gameState().castleId(), worldId);
    InteriorLayoutPlan layoutPlan = layoutPlanner.buildDefaultLayout(worldId, bundle.gameState().castleLocation());
    sessionsByPlayerId.put(bundle.playerId(), session);
    layoutByPlayerId.put(bundle.playerId(), layoutPlan);
    runtimeGateway.movePlayer(bundle.playerId(), layoutPlan.returnPortalPosition());
    bundle.gameState().assignInteriorWorldId(worldId, Instant.now());
    return session;
  }

  public void leaveInterior(PlayerStateBundle bundle) {
    InteriorSession session = sessionsByPlayerId.remove(bundle.playerId());
    if (session != null) {
      session.deactivate();
    }
    layoutByPlayerId.remove(bundle.playerId());
    WorldPosition castlePosition = new WorldPosition(
        bundle.gameState().castleLocation().worldId(),
        bundle.gameState().castleLocation().x(),
        bundle.gameState().castleLocation().y(),
        bundle.gameState().castleLocation().z()
    );
    runtimeGateway.movePlayer(bundle.playerId(), castlePosition);
    bundle.gameState().assignInteriorWorldId(null, Instant.now());
  }

  public Optional<InteriorSession> findSession(UUID playerId) {
    return Optional.ofNullable(sessionsByPlayerId.get(playerId));
  }

  public Optional<InteriorLayoutPlan> findLayout(UUID playerId) {
    return Optional.ofNullable(layoutByPlayerId.get(playerId));
  }

  public Map<UUID, InteriorSession> snapshotSessions() {
    return Map.copyOf(sessionsByPlayerId);
  }
}
