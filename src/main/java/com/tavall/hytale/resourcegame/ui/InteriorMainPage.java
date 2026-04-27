package com.tavall.hytale.resourcegame.ui;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiActionService;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;

import java.util.List;
import java.util.Map;

/**
 * Interior overview page.
 */
public final class InteriorMainPage extends BaseUiPage {
    private static final String PAGE_DOCUMENT = "Pages/interior-main.html";

    public InteriorMainPage(Player player, UiNavigationContext context, PlayerGameState state, IUiActionService actionService) {
        super(player, context, state, actionService, PAGE_DOCUMENT, templateData(context, state, actionService), bindings());
    }

    private static Map<String, ?> templateData(UiNavigationContext context, PlayerGameState state, IUiActionService actionService) {
        return Map.of(
                "TutorialStatus",
                context.feedbackMessage().isBlank()
                        ? actionService.interiorTutorialMessage(state)
                        : context.feedbackMessage()
        );
    }

    private static List<HyUiActionBinding> bindings() {
        return List.of(
                HyUiActionBinding.action("#ExitInteriorButton", UiActions.EXIT_INTERIOR),
                HyUiActionBinding.action("#BuildingsButton", UiActions.OPEN_BUILDINGS),
                HyUiActionBinding.action("#BackButton", UiActions.OPEN_CASTLE_MAIN)
        );
    }
}
