package org.tavall.hytale.resourcegame.service;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.tavall.hytale.resourcegame.domain.interior.InteriorLayoutPlan;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;
import org.tavall.hytale.resourcegame.runtime.HytaleAssetId;
import org.tavall.hytale.resourcegame.runtime.HytaleRuntimeGateway;

/**
 * Spawns and updates interior anchor entities that show citizen/troop counts above their heads.
 */
public class CitizenTroopDisplayService {

  private final HytaleRuntimeGateway runtimeGateway;
  private final ConcurrentHashMap<UUID, InteriorDisplayState> displayByPlayerId = new ConcurrentHashMap<>();

  public CitizenTroopDisplayService(HytaleRuntimeGateway runtimeGateway) {
    this.runtimeGateway = runtimeGateway;
  }

  public InteriorDisplayState ensureAnchors(PlayerStateBundle bundle, InteriorLayoutPlan layoutPlan) {
    InteriorDisplayState existing = displayByPlayerId.get(bundle.playerId());
    if (existing != null) {
      updateLabels(bundle.playerId(), bundle);
      return existing;
    }
    UUID citizenAnchor = runtimeGateway.spawnEntity(
        HytaleAssetId.DISPLAY_CITIZEN_ANCHOR,
        layoutPlan.citizenAnchorPosition(),
        "Citizens: " + bundle.gameState().citizenCount()
    );
    UUID troopAnchor = runtimeGateway.spawnEntity(
        HytaleAssetId.DISPLAY_TROOP_ANCHOR,
        layoutPlan.troopAnchorPosition(),
        "Troops: " + bundle.gameState().troopCount()
    );
    InteriorDisplayState state = new InteriorDisplayState(citizenAnchor, troopAnchor, new HashSet<>(), new HashSet<>());
    displayByPlayerId.put(bundle.playerId(), state);
    return state;
  }

  public void registerDuplicate(UUID playerId, DisplayAnchorType anchorType, UUID duplicateEntityId) {
    InteriorDisplayState state = displayByPlayerId.get(playerId);
    if (state == null) {
      return;
    }
    if (anchorType == DisplayAnchorType.CITIZEN) {
      state.citizenCopies().add(duplicateEntityId);
      return;
    }
    state.troopCopies().add(duplicateEntityId);
  }

  public void updateLabels(UUID playerId, PlayerStateBundle bundle) {
    InteriorDisplayState state = displayByPlayerId.get(playerId);
    if (state == null) {
      return;
    }
    runtimeGateway.updateEntityLabel(state.citizenAnchorEntityId(), "Citizens: " + bundle.gameState().citizenCount());
    runtimeGateway.updateEntityLabel(state.troopAnchorEntityId(), "Troops: " + bundle.gameState().troopCount());
  }

  public Optional<InteriorDisplayState> find(UUID playerId) {
    return Optional.ofNullable(displayByPlayerId.get(playerId));
  }
}
