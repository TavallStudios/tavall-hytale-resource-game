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
import com.tavall.hytale.resourcegame.domain.CastleEconomySnapshot;
import com.tavall.hytale.resourcegame.domain.CitizenJobType;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;
import com.tavall.hytale.resourcegame.services.CastleEconomyPlanner;

/**
 * Citizens placeholder page.
 */
public final class CastleCitizensPage extends BaseUiPage {
    private static final String PAGE_DOCUMENT = "Pages/castle-citizens.ui";
    private final CastleEconomyPlanner economyPlanner;

    public CastleCitizensPage(
            Player player,
            UiNavigationContext context,
            PlayerGameState state,
            IUiActionService actionService,
            CastleEconomyPlanner economyPlanner
    ) {
        super(player, context, state, actionService);
        this.economyPlanner = economyPlanner;
    }

    @Override
    public void build(Ref<EntityStore> entityRef, UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder, Store<EntityStore> entityStore) {
        uiCommandBuilder.append(PAGE_DOCUMENT);
        uiCommandBuilder.set("#CitizenCount.Text", String.valueOf(state().populationSummary().citizenCount()));
        CastleEconomySnapshot snapshot = economyPlanner.snapshot(state());
        uiCommandBuilder.set("#IdleCount.Text", String.valueOf(snapshot.jobCount(CitizenJobType.IDLE)));
        uiCommandBuilder.set("#GathererCount.Text", String.valueOf(snapshot.jobCount(CitizenJobType.GATHERER)));
        uiCommandBuilder.set("#BuilderCount.Text", String.valueOf(snapshot.jobCount(CitizenJobType.BUILDER)));
        uiCommandBuilder.set("#TraineeCount.Text", String.valueOf(snapshot.jobCount(CitizenJobType.TRAINEE)));
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
