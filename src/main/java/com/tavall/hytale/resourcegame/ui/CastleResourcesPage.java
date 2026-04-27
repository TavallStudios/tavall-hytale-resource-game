package com.tavall.hytale.resourcegame.ui;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiActionService;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;
import com.tavall.hytale.resourcegame.resources.ResourceType;
import com.tavall.hytale.resourcegame.services.CastleEconomyPlanner;

import java.util.List;
import java.util.Map;

/**
 * Resource inventory page.
 */
public final class CastleResourcesPage extends BaseUiPage {
    private static final String PAGE_DOCUMENT = "Pages/castle-resources.html";

    public CastleResourcesPage(
            Player player,
            UiNavigationContext context,
            PlayerGameState state,
            IUiActionService actionService,
            CastleEconomyPlanner economyPlanner
    ) {
        super(player, context, state, actionService, PAGE_DOCUMENT, templateData(state, economyPlanner), bindings());
    }

    private static Map<String, ?> templateData(PlayerGameState state, CastleEconomyPlanner economyPlanner) {
        return Map.ofEntries(
                Map.entry("FoodCount", String.valueOf(state.resources().food())),
                Map.entry("WoodCount", String.valueOf(state.resources().wood())),
                Map.entry("IronCount", String.valueOf(state.resources().iron())),
                Map.entry("FoodNodeStatus", economyPlanner.nodeSummary(state, ResourceType.FOOD)),
                Map.entry("WoodNodeStatus", economyPlanner.nodeSummary(state, ResourceType.WOOD)),
                Map.entry("IronNodeStatus", economyPlanner.nodeSummary(state, ResourceType.IRON))
        );
    }

    private static List<HyUiActionBinding> bindings() {
        return List.of(HyUiActionBinding.action("#BackButton", UiActions.OPEN_CASTLE_MAIN));
    }
}
