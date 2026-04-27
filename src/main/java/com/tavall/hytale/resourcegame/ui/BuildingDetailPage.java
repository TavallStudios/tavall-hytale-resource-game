package com.tavall.hytale.resourcegame.ui;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleBuildingService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiActionService;
import com.tavall.hytale.resourcegame.domain.CastleBuildingData;
import com.tavall.hytale.resourcegame.domain.CastleBuildingSummary;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Detail page for one selected building.
 */
public final class BuildingDetailPage extends BaseUiPage {
    private static final String PAGE_DOCUMENT = "Pages/building-detail.html";

    public BuildingDetailPage(
            Player player,
            UiNavigationContext context,
            PlayerGameState state,
            IUiActionService actionService,
            ICastleBuildingService buildingService
    ) {
        super(player, context, state, actionService, PAGE_DOCUMENT, templateData(player, context, state, buildingService), bindings());
    }

    private static Map<String, ?> templateData(Player player, UiNavigationContext context, PlayerGameState state, ICastleBuildingService buildingService) {
        Optional<CastleBuildingData> buildingOptional = buildingService.findBuilding(state, context.selectedBuildingId());
        if (buildingOptional.isEmpty()) {
            return Map.ofEntries(
                    Map.entry("BuildingTitle", "Building Missing"),
                    Map.entry("AreaText", "No tracked building is selected."),
                    Map.entry("LocationText", "Use /kd buildings select <type|id> or interact with a placed building."),
                    Map.entry("LevelText", "-"),
                    Map.entry("StatusText", "Missing"),
                    Map.entry("EffectText", "-"),
                    Map.entry("NextUpgradeText", "Select a building from the world first."),
                    Map.entry("FeedbackStatus", context.feedbackMessage().isBlank() ? "Awaiting building selection." : context.feedbackMessage())
            );
        }
        CastleBuildingSummary summary = buildingService.summary(player.getUuid(), state, buildingOptional.get(), Instant.now());
        return Map.ofEntries(
                Map.entry("BuildingTitle", summary.buildingData().buildingType().displayName()),
                Map.entry("AreaText", summary.buildingData().areaType().displayName()),
                Map.entry("LocationText", summary.worldName() + " | " + (int) summary.worldX() + ", " + (int) summary.worldY() + ", " + (int) summary.worldZ()),
                Map.entry("LevelText", summary.isUnderConstruction() ? "L" + summary.completedLevel() + " -> L" + summary.displayLevel() : "L" + summary.completedLevel()),
                Map.entry("StatusText", summary.isUnderConstruction() ? summary.constructionStage().name().toLowerCase() + " | " + (int) Math.round(summary.progressRatio() * 100.0D) + "% | " + summary.remainingSeconds() + "s" : summary.statusText()),
                Map.entry("EffectText", effectText(summary)),
                Map.entry("NextUpgradeText", nextUpgradeText(summary)),
                Map.entry("FeedbackStatus", context.feedbackMessage().isBlank() ? "Use the button below to start the next upgrade when resources allow." : context.feedbackMessage())
        );
    }

    private static List<HyUiActionBinding> bindings() {
        return List.of(
                HyUiActionBinding.action("#StartUpgradeButton", UiActions.BUILDING_START_UPGRADE),
                HyUiActionBinding.action("#CancelUpgradeButton", UiActions.BUILDING_CANCEL_UPGRADE),
                HyUiActionBinding.action("#OverviewButton", UiActions.OPEN_BUILDINGS),
                HyUiActionBinding.action("#BackButton", UiActions.OPEN_CASTLE_MAIN)
        );
    }

    private static String effectText(CastleBuildingSummary summary) {
        if (summary.foodPerTickBonus() > 0 || summary.woodPerTickBonus() > 0 || summary.ironPerTickBonus() > 0) {
            return "Passive yield: +" + summary.foodPerTickBonus() + " Food, +" + summary.woodPerTickBonus() + " Wood, +" + summary.ironPerTickBonus() + " Iron per tick.";
        }
        if (summary.constructionSpeedBonus() > 0.0D) {
            return "Construction effect: +" + (int) Math.round(summary.constructionSpeedBonus() * 100.0D) + "% faster future building work.";
        }
        return "Training effect: promotion discount -" + summary.promotionDiscount().foodCost()
                + " Food, -" + summary.promotionDiscount().woodCost()
                + " Wood, -" + summary.promotionDiscount().ironCost()
                + " Iron.";
    }

    private static String nextUpgradeText(CastleBuildingSummary summary) {
        if (summary.isUnderConstruction()) {
            return "Current work order finishes in " + summary.remainingSeconds() + "s.";
        }
        if (summary.nextUpgradeProfile() == null) {
            return "Max level reached.";
        }
        return "Next upgrade cost: "
                + summary.nextUpgradeProfile().foodCost() + " Food, "
                + summary.nextUpgradeProfile().woodCost() + " Wood, "
                + summary.nextUpgradeProfile().ironCost() + " Iron | Build time "
                + summary.nextUpgradeProfile().buildSeconds() + "s before workshop bonuses.";
    }
}
