package org.tavall.hytale.resourcegame.service;

import java.util.List;
import java.util.Map;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;
import org.tavall.hytale.resourcegame.domain.resource.ResourceType;
import org.tavall.hytale.resourcegame.domain.ui.UiAction;
import org.tavall.hytale.resourcegame.domain.ui.UiScreen;
import org.tavall.hytale.resourcegame.domain.ui.UiSection;
import org.tavall.hytale.resourcegame.runtime.HytaleAssetId;

public class CitizenTroopUpgradeUiAssembler {

  private final PopulationContinuumService populationContinuumService;

  public CitizenTroopUpgradeUiAssembler(PopulationContinuumService populationContinuumService) {
    this.populationContinuumService = populationContinuumService;
  }

  public UiScreen build(PlayerStateBundle bundle) {
    int citizens = bundle.gameState().citizenCount();
    int troops = bundle.gameState().troopCount();
    UiSection summary = new UiSection(
        "Population",
        List.of(
            "Citizens: " + citizens,
            "Troops: " + troops,
            "Continuum mode: units can move in both directions"
        ),
        List.of(buildPromoteAction(bundle), buildDemoteAction(bundle)),
        false
    );
    UiSection resources = new UiSection(
        "Resources",
        List.of(
            "Food: " + bundle.gameState().resources().get(ResourceType.FOOD),
            "Wood: " + bundle.gameState().resources().get(ResourceType.WOOD),
            "Iron: " + bundle.gameState().resources().get(ResourceType.IRON),
            "Promotion costs: " + formatCosts(populationContinuumService.troopPromotionCost())
        ),
        List.of(),
        false
    );
    UiSection future = new UiSection(
        "Future Branching",
        List.of(
            "Branching military specializations (placeholder)",
            "Unit specialization progression lanes (placeholder)",
            "Production impact overlays (placeholder)"
        ),
        List.of(new UiAction("future-branching", "Future Paths", false, "Placeholder - not implemented yet")),
        true
    );
    return new UiScreen(
        "citizen-troop-upgrade",
        "Citizen -> Troop Upgrade",
        HytaleAssetId.UI_CITIZEN_TROOP_PANEL,
        List.of(summary, resources, future)
    );
  }

  private UiAction buildPromoteAction(PlayerStateBundle bundle) {
    if (bundle.gameState().citizenCount() <= 0) {
      return new UiAction("promote", "Promote 1", false, "Need at least one citizen");
    }
    for (Map.Entry<ResourceType, Integer> cost : populationContinuumService.troopPromotionCost().entrySet()) {
      if (!bundle.gameState().resources().hasAtLeast(cost.getKey(), cost.getValue())) {
        return new UiAction("promote", "Promote 1", false, "Missing " + cost.getKey().name().toLowerCase());
      }
    }
    return new UiAction("promote", "Promote 1", true, null);
  }

  private UiAction buildDemoteAction(PlayerStateBundle bundle) {
    if (bundle.gameState().troopCount() <= 0) {
      return new UiAction("demote", "Demote 1", false, "Need at least one troop");
    }
    return new UiAction("demote", "Demote 1", true, null);
  }

  private String formatCosts(Map<ResourceType, Integer> costs) {
    return "Food " + costs.get(ResourceType.FOOD)
        + ", Wood " + costs.get(ResourceType.WOOD)
        + ", Iron " + costs.get(ResourceType.IRON);
  }
}
