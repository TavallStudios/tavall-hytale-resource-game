package com.tavall.hytale.resourcegame.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiActionService;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;

/**
 * Interior overview page.
 */
public final class InteriorMainPage extends BaseUiPage {
    private static final String PAGE_DOCUMENT = "Pages/interior-main.ui";

    public InteriorMainPage(Player player, UiNavigationContext context, PlayerGameState state, IUiActionService actionService) {
        super(player, context, state, actionService);
    }

    @Override
    public void build(Ref<EntityStore> entityRef, UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder, Store<EntityStore> entityStore) {
        uiCommandBuilder.append(PAGE_DOCUMENT);
        uiCommandBuilder.set(
                "#TutorialStatus.Text",
                context().feedbackMessage().isBlank()
                        ? actionService().interiorTutorialMessage(state())
                        : context().feedbackMessage()
        );
        bind(uiEventBuilder, "#ExitInteriorButton", UiActions.EXIT_INTERIOR);
        bind(uiEventBuilder, "#BackButton", UiActions.OPEN_CASTLE_MAIN);
    }

    private void bind(UIEventBuilder uiEventBuilder, String selector, String action) {
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                selector,
                EventData.of(UiActionEventData.KEY_ACTION, action),
                false
        );
    }
}
