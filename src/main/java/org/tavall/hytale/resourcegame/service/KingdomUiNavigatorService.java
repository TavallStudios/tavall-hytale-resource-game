package org.tavall.hytale.resourcegame.service;

import java.util.List;
import org.tavall.hytale.resourcegame.domain.ui.UiAction;
import org.tavall.hytale.resourcegame.domain.ui.UiScreen;
import org.tavall.hytale.resourcegame.domain.ui.UiSection;
import org.tavall.hytale.resourcegame.runtime.HytaleAssetId;

public class KingdomUiNavigatorService {

  public UiScreen buildNavigator() {
    UiSection section = new UiSection(
        "Debug UI Navigator",
        List.of(
            "Castle Main UI",
            "Citizen/Troop Upgrade UI",
            "Resource Summary UI",
            "Interior Layout Overview UI"
        ),
        List.of(
            new UiAction("ui:castle-main", "Castle Main", true, null),
            new UiAction("ui:upgrade", "Citizen/Troop Upgrade", true, null),
            new UiAction("ui:resource-summary", "Resource Summary", true, null),
            new UiAction("ui:interior-overview", "Interior Overview", true, null)
        ),
        false
    );
    return new UiScreen("debug-ui-navigator", "Kingdom Debug Navigator", HytaleAssetId.UI_CASTLE_PANEL, List.of(section));
  }
}
