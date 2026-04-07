package org.tavall.hytale.resourcegame.service;

import java.util.List;
import org.tavall.hytale.resourcegame.domain.interior.InteriorLayoutPlan;
import org.tavall.hytale.resourcegame.domain.ui.UiScreen;
import org.tavall.hytale.resourcegame.domain.ui.UiSection;
import org.tavall.hytale.resourcegame.runtime.HytaleAssetId;

public class InteriorOverviewUiAssembler {

  public UiScreen build(InteriorLayoutPlan layoutPlan) {
    UiSection layout = new UiSection(
        "Interior Layout",
        List.of(
            "Citizen anchor: " + layoutPlan.citizenAnchorPosition(),
            "Troop anchor: " + layoutPlan.troopAnchorPosition(),
            "Future upgrade zone: " + layoutPlan.futureUpgradeZone(),
            "Future station zone: " + layoutPlan.futureStationZone()
        ),
        List.of(),
        false
    );
    return new UiScreen("interior-overview", "Interior Overview", HytaleAssetId.UI_CASTLE_PANEL, List.of(layout));
  }
}
