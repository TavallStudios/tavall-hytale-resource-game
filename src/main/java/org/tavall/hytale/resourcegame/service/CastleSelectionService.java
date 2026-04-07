package org.tavall.hytale.resourcegame.service;

import java.util.Optional;
import java.util.UUID;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;
import org.tavall.hytale.resourcegame.domain.ui.UiScreen;
import org.tavall.hytale.resourcegame.runtime.HytaleRuntimeGateway;

/**
 * Opens the castle UI when near/look conditions are satisfied.
 */
public class CastleSelectionService {

  private final CastleInteractionService castleInteractionService;
  private final CastleUiAssembler castleUiAssembler;
  private final HytaleRuntimeGateway runtimeGateway;

  public CastleSelectionService(
      CastleInteractionService castleInteractionService,
      CastleUiAssembler castleUiAssembler,
      HytaleRuntimeGateway runtimeGateway
  ) {
    this.castleInteractionService = castleInteractionService;
    this.castleUiAssembler = castleUiAssembler;
    this.runtimeGateway = runtimeGateway;
  }

  public boolean openIfSelectable(UUID playerId, PlayerStateBundle bundle) {
    Optional<?> context = castleInteractionService.inspect(playerId);
    if (context.isEmpty()) {
      return false;
    }
    UiScreen screen = castleUiAssembler.build(bundle);
    runtimeGateway.openUi(playerId, screen);
    return true;
  }
}
