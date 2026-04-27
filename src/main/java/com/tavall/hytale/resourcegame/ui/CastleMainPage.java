package com.tavall.hytale.resourcegame.ui;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiActionService;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;
import com.tavall.hytale.resourcegame.services.CastleEconomyPlanner;

import java.util.List;
import java.util.Map;

/**
 * Main castle UI page.
 */
public final class CastleMainPage extends BaseUiPage {
    private static final String PAGE_DOCUMENT = "Pages/castle-main.html";

    public CastleMainPage(
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
                Map.entry("CitizenCount", String.valueOf(state.populationSummary().citizenCount())),
                Map.entry("TroopCount", String.valueOf(state.populationSummary().troopCount())),
                Map.entry("MightCount", String.valueOf(state.populationSummary().might())),
                Map.entry("FoodCount", String.valueOf(state.resources().food())),
                Map.entry("WoodCount", String.valueOf(state.resources().wood())),
                Map.entry("IronCount", String.valueOf(state.resources().iron())),
                Map.entry("Subtitle", economyPlanner.workforceSummary(state))
        );
    }

    private static List<HyUiActionBinding> bindings() {
        return List.of(
                HyUiActionBinding.action("#EnterInteriorButton", UiActions.ENTER_INTERIOR),
                HyUiActionBinding.action("#CastleInfoButton", UiActions.OPEN_CASTLE_INFO),
                HyUiActionBinding.action("#CitizensButton", UiActions.OPEN_CITIZENS),
                HyUiActionBinding.action("#TroopsButton", UiActions.OPEN_TROOPS),
                HyUiActionBinding.action("#ResourcesButton", UiActions.OPEN_RESOURCES),
                HyUiActionBinding.action("#UpgradesButton", UiActions.OPEN_UPGRADES),
                HyUiActionBinding.action("#BuildingsButton", UiActions.OPEN_BUILDINGS),
                HyUiActionBinding.action("#AttackButton", UiActions.CASTLE_ATTACK_PLACEHOLDER),
                HyUiActionBinding.action("#FriendButton", UiActions.CASTLE_FRIEND_PLACEHOLDER),
                HyUiActionBinding.action("#GuildButton", UiActions.CASTLE_GUILD_PLACEHOLDER),
                HyUiActionBinding.action("#CloseButton", UiActions.CLOSE)
        );
    }
}
