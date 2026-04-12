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
 * Citizen-to-troop upgrade page.
 */
public final class CastleUpgradesPage extends BaseUiPage {
    private static final String PAGE_DOCUMENT = "Pages/castle-upgrades.ui";

    public CastleUpgradesPage(Player player, UiNavigationContext context, PlayerGameState state, IUiActionService actionService) {
        super(player, context, state, actionService);
    }

    @Override
    public void build(Ref<EntityStore> entityRef, UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder, Store<EntityStore> entityStore) {
        UpgradeActionState promoteState = actionService().promoteActionState(state());
        UpgradeActionState demoteState = actionService().demoteActionState(state());
        uiCommandBuilder.append(PAGE_DOCUMENT);
        uiCommandBuilder.set("#CitizenCount.Text", String.valueOf(state().populationSummary().citizenCount()));
        uiCommandBuilder.set("#TroopCount.Text", String.valueOf(state().populationSummary().troopCount()));
        uiCommandBuilder.set("#FoodCount.Text", String.valueOf(state().resources().food()));
        uiCommandBuilder.set("#WoodCount.Text", String.valueOf(state().resources().wood()));
        uiCommandBuilder.set("#IronCount.Text", String.valueOf(state().resources().iron()));
        uiCommandBuilder.set("#PromotionCost.Text", actionService().promotionCostSummary());
        uiCommandBuilder.set("#PromoteStatus.Text", promoteState.message());
        uiCommandBuilder.set("#DemoteStatus.Text", demoteState.message());
        uiCommandBuilder.set("#TutorialStatus.Text", actionService().upgradeTutorialMessage(state()));
        uiCommandBuilder.set("#FeedbackStatus.Text", context().feedbackMessage().isBlank() ? "Awaiting action." : context().feedbackMessage());
        bind(uiEventBuilder, "#PromoteButton", UiActions.PROMOTE);
        bind(uiEventBuilder, "#DemoteButton", UiActions.DEMOTE);
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
