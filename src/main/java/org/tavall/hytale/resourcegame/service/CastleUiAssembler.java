package org.tavall.hytale.resourcegame.service;

import java.util.List;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;
import org.tavall.hytale.resourcegame.domain.ui.UiAction;
import org.tavall.hytale.resourcegame.domain.ui.UiScreen;
import org.tavall.hytale.resourcegame.domain.ui.UiSection;
import org.tavall.hytale.resourcegame.runtime.HytaleAssetId;

public class CastleUiAssembler {

  /**
   * Builds the main castle interaction screen with placeholder sections and future markers.
   */
  public UiScreen build(PlayerStateBundle stateBundle) {
    UiSection entry = new UiSection(
        "Castle",
        List.of("Your keep is secure.", "Use this panel to move from citizen logistics to army growth."),
        List.of(
            new UiAction("enter-interior", "Enter Interior", true, null),
            new UiAction("view-info", "View Castle Info", true, null)
        ),
        false
    );
    UiSection systems = new UiSection(
        "Kingdom Systems",
        List.of(
            "Citizens: " + stateBundle.gameState().citizenCount(),
            "Troops: " + stateBundle.gameState().troopCount(),
            "Resources available in starter phase."
        ),
        List.of(
            new UiAction("open-citizens", "Citizens", true, null),
            new UiAction("open-troops", "Troops", true, null),
            new UiAction("open-resources", "Resources", true, null)
        ),
        false
    );
    UiSection placeholders = new UiSection(
        "Future",
        List.of(
            "Council decisions (placeholder)",
            "Castle visual upgrade tree (placeholder)",
            "Specialization branches (placeholder)"
        ),
        List.of(new UiAction("future-placeholder", "Future Options", false, "Placeholder - not implemented yet")),
        true
    );
    return new UiScreen(
        "castle-main",
        "Castle Control",
        HytaleAssetId.UI_CASTLE_PANEL,
        List.of(entry, systems, placeholders)
    );
  }
}
