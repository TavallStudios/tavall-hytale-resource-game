package org.tavall.hytale.resourcegame.service;

import java.util.List;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;
import org.tavall.hytale.resourcegame.domain.resource.ResourceType;
import org.tavall.hytale.resourcegame.domain.ui.UiScreen;
import org.tavall.hytale.resourcegame.domain.ui.UiSection;
import org.tavall.hytale.resourcegame.runtime.HytaleAssetId;

public class ResourceSummaryUiAssembler {

  public UiScreen build(PlayerStateBundle bundle) {
    UiSection resources = new UiSection(
        "Resources",
        List.of(
            "Food: " + bundle.gameState().resources().get(ResourceType.FOOD),
            "Wood: " + bundle.gameState().resources().get(ResourceType.WOOD),
            "Iron: " + bundle.gameState().resources().get(ResourceType.IRON)
        ),
        List.of(),
        false
    );
    return new UiScreen("resource-summary", "Resource Summary", HytaleAssetId.UI_CASTLE_PANEL, List.of(resources));
  }
}
