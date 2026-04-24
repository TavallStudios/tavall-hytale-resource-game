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
        uiCommandBuilder.set("#InteriorTourStatus.Text", tutorialStatus(gameStateService.isInteriorTourPending(state())));
        uiCommandBuilder.set("#UpgradeTutorialStatus.Text", tutorialStatus(gameStateService.isUpgradeTutorialPending(state())));
        uiCommandBuilder.set(
                "#CommandFeedback.Text",
                context().feedbackMessage().isBlank()
                        ? "Run /kd commands from here. Buttons queue the exact command text."
                        : context().feedbackMessage()
        );
        bind(uiEventBuilder, "#CastleMainButton", UiActions.OPEN_CASTLE_MAIN);
        bind(uiEventBuilder, "#CastleInfoButton", UiActions.OPEN_CASTLE_INFO);
        bind(uiEventBuilder, "#CitizensButton", UiActions.OPEN_CITIZENS);
        bind(uiEventBuilder, "#TroopsButton", UiActions.OPEN_TROOPS);
        bind(uiEventBuilder, "#ResourcesButton", UiActions.OPEN_RESOURCES);
        bind(uiEventBuilder, "#UpgradesButton", UiActions.OPEN_UPGRADES);
        bind(uiEventBuilder, "#InteriorButton", UiActions.ENTER_INTERIOR);
        bindCommand(uiEventBuilder, "#PlaceCastleButton", "/kd place castle");
        bindCommand(uiEventBuilder, "#PlaceFoodNodeButton", "/kd place node food");
        bindCommand(uiEventBuilder, "#PlaceWoodNodeButton", "/kd place node wood");
        bindCommand(uiEventBuilder, "#PlaceIronNodeButton", "/kd place node iron");
        bindCommand(uiEventBuilder, "#ConfirmPlacementButton", "/kd place confirm");
        bindCommand(uiEventBuilder, "#CancelPlacementButton", "/kd place cancel");
        bindCommand(uiEventBuilder, "#MoveNegXButton", "/kd place move -1 0");
        bindCommand(uiEventBuilder, "#MovePosXButton", "/kd place move 1 0");
        bindCommand(uiEventBuilder, "#MoveNegZButton", "/kd place move 0 -1");
        bindCommand(uiEventBuilder, "#MovePosZButton", "/kd place move 0 1");
        bindCommand(uiEventBuilder, "#InteriorRebuildButton", "/kd interior rebuild");
        bindCommand(uiEventBuilder, "#InteriorMoveButton", "/kd interior move");
        bindCommand(uiEventBuilder, "#InteriorExitButton", "/kd interior exit");
        bindCommand(uiEventBuilder, "#SceneRefreshButton", "/kd scene refresh");
        bindCommand(uiEventBuilder, "#NodesClearButton", "/kd nodes clear");
        bindCommand(uiEventBuilder, "#NodesListButton", "/kd nodes list");
        bindCommand(uiEventBuilder, "#BuildingsListButton", "/kd buildings list");
        bindCommand(uiEventBuilder, "#StageFarmsteadButton", "/kd buildings stage farmstead");
        bindCommand(uiEventBuilder, "#StageLumberMillButton", "/kd buildings stage lumber_mill");
        bindCommand(uiEventBuilder, "#StageIronWorksButton", "/kd buildings stage iron_works");
        bindCommand(uiEventBuilder, "#StageBarracksButton", "/kd buildings stage barracks");
        bindCommand(uiEventBuilder, "#StageWorkshopButton", "/kd buildings stage workshop");
        bindCommand(uiEventBuilder, "#FocusButton", "/kd focus");
        bindCommand(uiEventBuilder, "#InteractButton", "/kd interact");
        bindCommand(uiEventBuilder, "#HologramTestButton", "/kd hologram stack Test hologram|Second line");
        bindCommand(uiEventBuilder, "#TutorialResetButton", "/kd tutorial reset");
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

    private void bindCommand(UIEventBuilder uiEventBuilder, String selector, String commandLine) {
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                selector,
                new EventData()
                        .put(UiActionEventData.KEY_ACTION, UiActions.RUN_COMMAND)
                        .put(UiActionEventData.KEY_PAYLOAD, commandLine),
                false
        );
    }

    private String tutorialStatus(boolean pending) {
        return pending ? "pending" : "complete";
    }
}
