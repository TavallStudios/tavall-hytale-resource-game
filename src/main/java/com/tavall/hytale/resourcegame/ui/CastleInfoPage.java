package com.tavall.hytale.resourcegame.ui;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiActionService;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;

import java.util.List;
import java.util.Map;

/**
 * Castle info placeholder page.
 */
public final class CastleInfoPage extends BaseUiPage {
    private static final String PAGE_DOCUMENT = "Pages/castle-info.html";

    public CastleInfoPage(Player player, UiNavigationContext context, PlayerGameState state, IUiActionService actionService) {
        super(player, context, state, actionService, PAGE_DOCUMENT, templateData(context, state), bindings());
    }

    private static Map<String, ?> templateData(UiNavigationContext context, PlayerGameState state) {
        return Map.ofEntries(
                Map.entry("CastleId", state.castleId() == null ? "Unassigned" : state.castleId().toString()),
                Map.entry("WorldName", state.castleLocation() == null ? "Unknown" : state.castleLocation().worldName()),
                Map.entry("OwnerName", context.playerName()),
                Map.entry("TroopCount", String.valueOf(state.populationSummary().troopCount())),
                Map.entry("MightCount", String.valueOf(state.populationSummary().might())),
                Map.entry("FeedbackStatus", context.feedbackMessage().isBlank() ? "Right-click the focused castle in-world to reopen this command surface." : context.feedbackMessage())
        );
    }

    private static List<HyUiActionBinding> bindings() {
        return List.of(
                HyUiActionBinding.action("#AttackButton", UiActions.CASTLE_ATTACK_PLACEHOLDER),
                HyUiActionBinding.action("#FriendButton", UiActions.CASTLE_FRIEND_PLACEHOLDER),
                HyUiActionBinding.action("#GuildButton", UiActions.CASTLE_GUILD_PLACEHOLDER),
                HyUiActionBinding.action("#BackButton", UiActions.OPEN_CASTLE_MAIN)
        );
    }
}
