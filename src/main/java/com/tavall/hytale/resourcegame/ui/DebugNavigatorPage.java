package com.tavall.hytale.resourcegame.ui;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInfrastructureHealthService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerGameStateService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiActionService;
import com.tavall.hytale.resourcegame.domain.InfrastructureHealthSnapshot;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;

import java.util.List;
import java.util.Map;

/**
 * Debug navigator for UI pages.
 */
public final class DebugNavigatorPage extends BaseUiPage {
    private static final String PAGE_DOCUMENT = "Pages/debug-navigator.html";

    public DebugNavigatorPage(
            Player player,
            UiNavigationContext context,
            PlayerGameState state,
            IUiActionService actionService,
            IInfrastructureHealthService infrastructureHealthService,
            IPlayerGameStateService gameStateService
    ) {
        super(player, context, state, actionService, PAGE_DOCUMENT, templateData(context, state, infrastructureHealthService, gameStateService), bindings());
    }

    private static Map<String, ?> templateData(
            UiNavigationContext context,
            PlayerGameState state,
            IInfrastructureHealthService infrastructureHealthService,
            IPlayerGameStateService gameStateService
    ) {
        InfrastructureHealthSnapshot healthSnapshot = infrastructureHealthService.snapshot();
        return Map.ofEntries(
                Map.entry("CacheStatus", healthSnapshot.cacheSummary()),
                Map.entry("PersistenceStatus", healthSnapshot.persistenceSummary()),
                Map.entry("InteriorTutorialStatus", tutorialStatus(gameStateService.isInteriorTutorialPending(state))),
                Map.entry("InteriorTourStatus", tutorialStatus(gameStateService.isInteriorTourPending(state))),
                Map.entry("UpgradeTutorialStatus", tutorialStatus(gameStateService.isUpgradeTutorialPending(state))),
                Map.entry(
                        "CommandFeedback",
                        context.feedbackMessage().isBlank()
                        ? "Run /kd commands from here. Buttons queue the exact command text."
                        : context.feedbackMessage()
                )
        );
    }

    private static List<HyUiActionBinding> bindings() {
        return List.of(
                HyUiActionBinding.action("#CastleMainButton", UiActions.OPEN_CASTLE_MAIN),
                HyUiActionBinding.action("#CastleInfoButton", UiActions.OPEN_CASTLE_INFO),
                HyUiActionBinding.action("#CitizensButton", UiActions.OPEN_CITIZENS),
                HyUiActionBinding.action("#TroopsButton", UiActions.OPEN_TROOPS),
                HyUiActionBinding.action("#ResourcesButton", UiActions.OPEN_RESOURCES),
                HyUiActionBinding.action("#UpgradesButton", UiActions.OPEN_UPGRADES),
                HyUiActionBinding.action("#InteriorButton", UiActions.ENTER_INTERIOR),
                HyUiActionBinding.command("#PlaceCastleButton", "/kd place castle"),
                HyUiActionBinding.command("#PlaceFoodNodeButton", "/kd place node food"),
                HyUiActionBinding.command("#PlaceWoodNodeButton", "/kd place node wood"),
                HyUiActionBinding.command("#PlaceIronNodeButton", "/kd place node iron"),
                HyUiActionBinding.command("#ConfirmPlacementButton", "/kd place confirm"),
                HyUiActionBinding.command("#CancelPlacementButton", "/kd place cancel"),
                HyUiActionBinding.command("#MoveNegXButton", "/kd place move -1 0"),
                HyUiActionBinding.command("#MovePosXButton", "/kd place move 1 0"),
                HyUiActionBinding.command("#MoveNegZButton", "/kd place move 0 -1"),
                HyUiActionBinding.command("#MovePosZButton", "/kd place move 0 1"),
                HyUiActionBinding.command("#InteriorRebuildButton", "/kd interior rebuild"),
                HyUiActionBinding.command("#InteriorMoveButton", "/kd interior move"),
                HyUiActionBinding.command("#InteriorExitButton", "/kd interior exit"),
                HyUiActionBinding.command("#SceneRefreshButton", "/kd scene refresh"),
                HyUiActionBinding.command("#NodesClearButton", "/kd nodes clear"),
                HyUiActionBinding.command("#NodesListButton", "/kd nodes list"),
                HyUiActionBinding.command("#BuildingsListButton", "/kd buildings list"),
                HyUiActionBinding.command("#StageFarmsteadButton", "/kd buildings stage farmstead"),
                HyUiActionBinding.command("#StageLumberMillButton", "/kd buildings stage lumber_mill"),
                HyUiActionBinding.command("#StageIronWorksButton", "/kd buildings stage iron_works"),
                HyUiActionBinding.command("#StageBarracksButton", "/kd buildings stage barracks"),
                HyUiActionBinding.command("#StageWorkshopButton", "/kd buildings stage workshop"),
                HyUiActionBinding.command("#FocusButton", "/kd focus"),
                HyUiActionBinding.command("#InteractButton", "/kd interact"),
                HyUiActionBinding.command("#HologramTestButton", "/kd hologram stack Test hologram|Second line"),
                HyUiActionBinding.command("#TutorialResetButton", "/kd tutorial reset"),
                HyUiActionBinding.action("#CloseButton", UiActions.CLOSE)
        );
    }

    private static String tutorialStatus(boolean pending) {
        return pending ? "pending" : "complete";
    }
}
