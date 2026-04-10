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
 * Main castle UI page.
 */
public final class CastleMainPage extends BaseUiPage {
    private static final String PAGE_DOCUMENT = "Pages/castle-main.ui";

    public CastleMainPage(Player player, UiNavigationContext context, PlayerGameState state, IUiActionService actionService) {
        super(player, context, state, actionService);
    }

    @Override
    public void build(Ref<EntityStore> entityRef, UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder, Store<EntityStore> entityStore) {
        uiCommandBuilder.append(PAGE_DOCUMENT);
        uiCommandBuilder.set("#CitizenCount.Text", String.valueOf(state().populationSummary().citizenCount()));
        uiCommandBuilder.set("#TroopCount.Text", String.valueOf(state().populationSummary().troopCount()));
        uiCommandBuilder.set("#FoodCount.Text", String.valueOf(state().resources().food()));
        uiCommandBuilder.set("#WoodCount.Text", String.valueOf(state().resources().wood()));
        uiCommandBuilder.set("#IronCount.Text", String.valueOf(state().resources().iron()));

        bind(uiEventBuilder, "#EnterInteriorButton", UiActions.ENTER_INTERIOR);
        bind(uiEventBuilder, "#CastleInfoButton", UiActions.OPEN_CASTLE_INFO);
        bind(uiEventBuilder, "#CitizensButton", UiActions.OPEN_CITIZENS);
        bind(uiEventBuilder, "#TroopsButton", UiActions.OPEN_TROOPS);
        bind(uiEventBuilder, "#ResourcesButton", UiActions.OPEN_RESOURCES);
        bind(uiEventBuilder, "#UpgradesButton", UiActions.OPEN_UPGRADES);
        bind(uiEventBuilder, "#CloseButton", UiActions.CLOSE);
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
