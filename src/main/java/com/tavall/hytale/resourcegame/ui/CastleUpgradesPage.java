package com.tavall.hytale.resourcegame.ui;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiActionService;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;

import java.util.List;
import java.util.Map;

/**
 * Citizen-to-troop upgrade page.
 */
public final class CastleUpgradesPage extends BaseUiPage {
    private static final String PAGE_DOCUMENT = "Pages/castle-upgrades.html";

    public CastleUpgradesPage(Player player, UiNavigationContext context, PlayerGameState state, IUiActionService actionService) {
        super(player, context, state, actionService, PAGE_DOCUMENT, templateData(context, state, actionService), bindings());
    }

    private static Map<String, ?> templateData(UiNavigationContext context, PlayerGameState state, IUiActionService actionService) {
        UpgradeActionState promoteState = actionService.promoteActionState(state);
        UpgradeActionState demoteState = actionService.demoteActionState(state);
        return Map.ofEntries(
                Map.entry("CitizenCount", String.valueOf(state.populationSummary().citizenCount())),
                Map.entry("TroopCount", String.valueOf(state.populationSummary().troopCount())),
                Map.entry("FoodCount", String.valueOf(state.resources().food())),
                Map.entry("WoodCount", String.valueOf(state.resources().wood())),
                Map.entry("IronCount", String.valueOf(state.resources().iron())),
                Map.entry("PromotionCost", actionService.promotionCostSummary(state)),
                Map.entry("PromoteStatus", promoteState.message()),
                Map.entry("DemoteStatus", demoteState.message()),
                Map.entry("TutorialStatus", actionService.upgradeTutorialMessage(state)),
                Map.entry("FeedbackStatus", context.feedbackMessage().isBlank() ? "Awaiting action." : context.feedbackMessage())
        );
    }

    private static List<HyUiActionBinding> bindings() {
        return List.of(
                HyUiActionBinding.action("#PromoteButton", UiActions.PROMOTE),
                HyUiActionBinding.action("#DemoteButton", UiActions.DEMOTE),
                HyUiActionBinding.action("#BackButton", UiActions.OPEN_CASTLE_MAIN)
        );
    }
}
