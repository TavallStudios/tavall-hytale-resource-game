package com.tavall.hytale.resourcegame.ui;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInteriorWorldService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerGameStateService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerSessionStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPopulationService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiActionService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiNavigator;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;
import com.tavall.hytale.resourcegame.domain.ResourceNodeData;
import com.tavall.hytale.resourcegame.services.PlayerSession;
import com.tavall.hytale.resourcegame.tasks.AsyncTask;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Routes UI actions to services.
 */
public final class UiActionService implements IUiActionService, IDependencyInjectableConcrete {
    private final IUiNavigator uiNavigator;
    private final IInteriorWorldService interiorWorldService;
    private final IPopulationService populationService;
    private final IPlayerSessionStore sessionStore;
    private final IPlayerGameStateService gameStateService;
    private final IResourceNodeService resourceNodeService;
    private final IResourceNodeVisualService resourceNodeVisualService;

    public UiActionService(
            IUiNavigator uiNavigator,
            IInteriorWorldService interiorWorldService,
            IPopulationService populationService,
            IPlayerSessionStore sessionStore,
            IPlayerGameStateService gameStateService,
            IResourceNodeService resourceNodeService,
            IResourceNodeVisualService resourceNodeVisualService
    ) {
        this.uiNavigator = Objects.requireNonNull(uiNavigator, "uiNavigator");
        this.interiorWorldService = Objects.requireNonNull(interiorWorldService, "interiorWorldService");
        this.populationService = Objects.requireNonNull(populationService, "populationService");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.gameStateService = Objects.requireNonNull(gameStateService, "gameStateService");
        this.resourceNodeService = Objects.requireNonNull(resourceNodeService, "resourceNodeService");
        this.resourceNodeVisualService = Objects.requireNonNull(resourceNodeVisualService, "resourceNodeVisualService");
    }

    public void handle(Player player, UiNavigationContext context, UiActionEventData eventData) {
        if (eventData == null || eventData.action() == null) {
            return;
        }
        String action = eventData.action();
        UUID playerId = context.playerId();
        PlayerSession session = sessionStore.get(playerId);
        PlayerGameState state = session == null ? null : session.gameState();
        if (UiActions.ENTER_INTERIOR.equals(action)) {
            interiorWorldService.enterInterior(player);
            return;
        }
        if (UiActions.EXIT_INTERIOR.equals(action)) {
            interiorWorldService.exitInterior(player);
            return;
        }
        if (UiActions.OPEN_CASTLE_INFO.equals(action) && state != null) {
            uiNavigator.open(UiPageType.CASTLE_INFO, player, context, state);
            return;
        }
        if (UiActions.OPEN_CITIZENS.equals(action) && state != null) {
            uiNavigator.open(UiPageType.CASTLE_CITIZENS, player, context, state);
            return;
        }
        if (UiActions.OPEN_TROOPS.equals(action) && state != null) {
            uiNavigator.open(UiPageType.CASTLE_TROOPS, player, context, state);
            return;
        }
        if (UiActions.OPEN_RESOURCES.equals(action) && state != null) {
            UiNavigationContext targetContext = context.selectedNodeId() == null
                    ? context
                    : context.clearFeedback().withSelectedNodeId(null);
            uiNavigator.open(UiPageType.CASTLE_RESOURCES, player, targetContext, state);
            return;
        }
        if (UiActions.OPEN_UPGRADES.equals(action) && state != null) {
            uiNavigator.open(UiPageType.CASTLE_UPGRADES, player, context, state);
            return;
        }
        if (UiActions.OPEN_CASTLE_MAIN.equals(action) && state != null) {
            uiNavigator.open(UiPageType.CASTLE_MAIN, player, context, state);
            return;
        }
        if (UiActions.OPEN_DEBUG.equals(action) && state != null) {
            uiNavigator.open(UiPageType.DEBUG_NAVIGATOR, player, context, state);
            return;
        }
        if (UiActions.PROMOTE.equals(action)) {
            boolean promoted = populationService.promoteCitizen(playerId);
            PlayerSession updatedSession = sessionStore.get(playerId);
            if (updatedSession != null) {
                if (promoted) {
                    PlayerGameState updatedState = markUpgradeTutorialSeen(playerId, updatedSession.gameState());
                    updatedSession.updateGameState(updatedState);
                }
                String feedback = promoted
                        ? "Promotion complete."
                        : populationService.promoteActionState(updatedSession.gameState()).message();
                uiNavigator.open(UiPageType.CASTLE_UPGRADES, player, context.withFeedback(feedback), updatedSession.gameState());
            }
            return;
        }
        if (UiActions.DEMOTE.equals(action)) {
            boolean demoted = populationService.demoteTroop(playerId);
            PlayerSession updatedSession = sessionStore.get(playerId);
            if (updatedSession != null) {
                if (demoted) {
                    PlayerGameState updatedState = markUpgradeTutorialSeen(playerId, updatedSession.gameState());
                    updatedSession.updateGameState(updatedState);
                }
                String feedback = demoted
                        ? "Demotion complete."
                        : populationService.demoteActionState(updatedSession.gameState()).message();
                uiNavigator.open(UiPageType.CASTLE_UPGRADES, player, context.withFeedback(feedback), updatedSession.gameState());
            }
            return;
        }
        if (context.selectedNodeId() != null) {
            handleNodeAction(player, context, action, playerId, context.selectedNodeId());
        }
    }

    public UpgradeActionState promoteActionState(PlayerGameState state) {
        return populationService.promoteActionState(state);
    }

    public UpgradeActionState demoteActionState(PlayerGameState state) {
        return populationService.demoteActionState(state);
    }

    public String promotionCostSummary() {
        return populationService.promotionCostSummary();
    }

    public String upgradeTutorialMessage(PlayerGameState state) {
        if (gameStateService.isUpgradeTutorialPending(state)) {
            return "Step 1: confirm citizens and troops. Step 2: check the Food, Wood, and Iron cost. Step 3: promote once the route is ready.";
        }
        return "Tutorial complete: use this page to convert citizens when resources allow.";
    }

    public String interiorTutorialMessage(PlayerGameState state) {
        if (gameStateService.isInteriorTutorialPending(state) || gameStateService.isInteriorTourPending(state)) {
            return "Step 1: follow the tour markers. Step 2: inspect the citizen and troop anchors. Step 3: leave through the exit lane when you are done.";
        }
        return "Interior tutorial complete: citizen and troop anchors stay here while the upgrade pipeline grows.";
    }

    private PlayerGameState markUpgradeTutorialSeen(UUID playerId, PlayerGameState state) {
        Instant now = Instant.now();
        PlayerGameState updated = gameStateService.markUpgradeTutorialSeen(state, now);
        if (updated != state) {
            gameStateService.cacheState(playerId, updated);
            AsyncTask.runAsync(() -> gameStateService.persistState(updated, now));
        }
        return updated;
    }

    private void handleNodeAction(Player player, UiNavigationContext context, String action, UUID playerId, UUID nodeId) {
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return;
        }
        Optional<ResourceNodeData> nodeOptional = resourceNodeService.findNode(session.gameState(), nodeId);
        if (nodeOptional.isEmpty()) {
            uiNavigator.open(UiPageType.CASTLE_RESOURCES, player, context.withFeedback("Node no longer exists.").withSelectedNodeId(null), session.gameState());
            return;
        }

        Instant now = Instant.now();
        PlayerGameState updatedState = switch (action) {
            case UiActions.NODE_ASSIGN_ONE -> resourceNodeService.addTroops(playerId, nodeId, 1, now);
            case UiActions.NODE_ASSIGN_THREE -> resourceNodeService.addTroops(playerId, nodeId, 3, now);
            case UiActions.NODE_ASSIGN_FIVE -> resourceNodeService.addTroops(playerId, nodeId, 5, now);
            case UiActions.NODE_ASSIGN_ALL -> resourceNodeService.assignTroops(
                    playerId,
                    nodeId,
                    nodeOptional.get().assignedTroops() + resourceNodeService.availableTroops(session.gameState()),
                    now
            );
            case UiActions.NODE_RECALL_ONE -> resourceNodeService.addTroops(playerId, nodeId, -1, now);
            case UiActions.NODE_RECALL_ALL -> resourceNodeService.assignTroops(playerId, nodeId, 0, now);
            default -> null;
        };
        if (updatedState == null) {
            return;
        }
        resourceNodeVisualService.refreshNodes(playerId, updatedState);
        String feedback = describeNodeFeedback(action, updatedState, nodeId);
        uiNavigator.open(
                UiPageType.RESOURCE_NODE_DETAIL,
                player,
                context.withFeedback(feedback).withSelectedNodeId(nodeId),
                updatedState
        );
    }

    private String describeNodeFeedback(String action, PlayerGameState updatedState, UUID nodeId) {
        Optional<ResourceNodeData> updatedNode = resourceNodeService.findNode(updatedState, nodeId);
        if (updatedNode.isEmpty()) {
            return "Node updated.";
        }
        String verb = switch (action) {
            case UiActions.NODE_ASSIGN_ONE, UiActions.NODE_ASSIGN_THREE, UiActions.NODE_ASSIGN_FIVE, UiActions.NODE_ASSIGN_ALL -> "Troops sent.";
            case UiActions.NODE_RECALL_ONE, UiActions.NODE_RECALL_ALL -> "Troops recalled.";
            default -> "Node updated.";
        };
        return verb + " Assigned now: " + updatedNode.get().assignedTroops() + ".";
    }
}
