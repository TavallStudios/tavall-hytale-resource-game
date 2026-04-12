package com.tavall.hytale.resourcegame.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInfrastructureHealthService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerGameStateService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiActionService;
import com.tavall.hytale.resourcegame.domain.InfrastructureHealthSnapshot;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;

/**
 * Debug navigator for UI pages.
 */
public final class DebugNavigatorPage extends BaseUiPage {
    private static final String PAGE_DOCUMENT = "Pages/debug-navigator.ui";
    private final IInfrastructureHealthService infrastructureHealthService;
    private final IPlayerGameStateService gameStateService;

    public DebugNavigatorPage(
            Player player,
            UiNavigationContext context,
            PlayerGameState state,
            IUiActionService actionService,
            IInfrastructureHealthService infrastructureHealthService,
            IPlayerGameStateService gameStateService
    ) {
        super(player, context, state, actionService);
        this.infrastructureHealthService = infrastructureHealthService;
        this.gameStateService = gameStateService;
    }

    @Override
    public void build(Ref<EntityStore> entityRef, UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder, Store<EntityStore> entityStore) {
        InfrastructureHealthSnapshot healthSnapshot = infrastructureHealthService.snapshot();
        uiCommandBuilder.append(PAGE_DOCUMENT);
        uiCommandBuilder.set("#CacheStatus.Text", healthSnapshot.cacheSummary());
        uiCommandBuilder.set("#PersistenceStatus.Text", healthSnapshot.persistenceSummary());
        uiCommandBuilder.set("#InteriorTutorialStatus.Text", tutorialStatus(gameStateService.isInteriorTutorialPending(state())));
        uiCommandBuilder.set("#UpgradeTutorialStatus.Text", tutorialStatus(gameStateService.isUpgradeTutorialPending(state())));
        bind(uiEventBuilder, "#CastleMainButton", UiActions.OPEN_CASTLE_MAIN);
        bind(uiEventBuilder, "#CastleInfoButton", UiActions.OPEN_CASTLE_INFO);
        bind(uiEventBuilder, "#CitizensButton", UiActions.OPEN_CITIZENS);
        bind(uiEventBuilder, "#TroopsButton", UiActions.OPEN_TROOPS);
        bind(uiEventBuilder, "#ResourcesButton", UiActions.OPEN_RESOURCES);
        bind(uiEventBuilder, "#UpgradesButton", UiActions.OPEN_UPGRADES);
        bind(uiEventBuilder, "#InteriorButton", UiActions.ENTER_INTERIOR);
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

    private String tutorialStatus(boolean pending) {
        return pending ? "pending" : "complete";
    }
}
