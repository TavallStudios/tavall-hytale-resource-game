package com.tavall.hytale.resourcegame.ui;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiActionService;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.ResourceNodeData;
import com.tavall.hytale.resourcegame.domain.ResourceNodeSummary;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * UI for assigning troop counts to a selected resource node.
 */
public final class ResourceNodePage extends BaseUiPage {
    private static final String PAGE_DOCUMENT = "Pages/resource-node-detail.html";

    public ResourceNodePage(
            Player player,
            UiNavigationContext context,
            PlayerGameState state,
            IUiActionService actionService,
            IResourceNodeService resourceNodeService
    ) {
        super(player, context, state, actionService, PAGE_DOCUMENT, templateData(context, state, resourceNodeService), bindings());
    }

    private static Map<String, ?> templateData(UiNavigationContext context, PlayerGameState state, IResourceNodeService resourceNodeService) {
        Optional<ResourceNodeData> nodeOptional = resourceNodeService.findNode(state, context.selectedNodeId());
        if (nodeOptional.isEmpty()) {
            return Map.ofEntries(
                    Map.entry("NodeTitle", "Node not found"),
                    Map.entry("NodeSummary", "Select a node from /kd nodes list or click a node in-world."),
                    Map.entry("AssignedTroops", "0"),
                    Map.entry("AssignedWorkers", "0"),
                    Map.entry("AvailableTroops", String.valueOf(resourceNodeService.availableTroops(state))),
                    Map.entry("GainPerTick", "+0/tick"),
                    Map.entry("PillageReward", "+0"),
                    Map.entry("StockStatus", "0 / 0 (0%)"),
                    Map.entry("RegenStatus", "+0 / tick"),
                    Map.entry("StatusText", "Exhausted"),
                    Map.entry("RouteStatus", "No supply lane"),
                    Map.entry("FeedbackStatus", "No node selected.")
            );
        }
        ResourceNodeData node = nodeOptional.get();
        ResourceNodeSummary summary = resourceNodeService.summary(state, node);
        return Map.ofEntries(
                Map.entry("NodeTitle", node.resourceType() + " Node " + node.nodeId().toString().substring(0, 8)),
                Map.entry("NodeSummary", node.worldName() + " | " + (int) node.x() + ", " + (int) node.y() + ", " + (int) node.z()),
                Map.entry("AssignedTroops", String.valueOf(summary.assignedTroops())),
                Map.entry("AssignedWorkers", String.valueOf(summary.assignedWorkers())),
                Map.entry("AvailableTroops", String.valueOf(summary.availableTroops())),
                Map.entry("GainPerTick", "+" + summary.gainPerTick() + "/tick (" + summary.workerGainPerTick() + " worker, " + summary.troopGainPerTick() + " troop)"),
                Map.entry("PillageReward", "+" + summary.pillageReward()),
                Map.entry("StockStatus", summary.currentStock() + " / " + summary.maxStock() + " (" + summary.stockPercent() + "%)"),
                Map.entry("RegenStatus", "+" + summary.regenerationPerTick() + " / tick"),
                Map.entry("StatusText", summary.stockStatus()),
                Map.entry("RouteStatus", summary.visibleRouteCount() <= 0 ? "No supply lane" : "Supply lane active: " + summary.visibleRouteCount() + " convoy markers"),
                Map.entry("FeedbackStatus", context.feedbackMessage().isBlank() ? "Workers auto-gather here. Troops can be sent or used for a larger manual pillage." : context.feedbackMessage())
        );
    }

    private static List<HyUiActionBinding> bindings() {
        return List.of(
                HyUiActionBinding.action("#AssignOneButton", UiActions.NODE_ASSIGN_ONE),
                HyUiActionBinding.action("#AssignThreeButton", UiActions.NODE_ASSIGN_THREE),
                HyUiActionBinding.action("#AssignFiveButton", UiActions.NODE_ASSIGN_FIVE),
                HyUiActionBinding.action("#AssignAllButton", UiActions.NODE_ASSIGN_ALL),
                HyUiActionBinding.action("#RecallOneButton", UiActions.NODE_RECALL_ONE),
                HyUiActionBinding.action("#RecallAllButton", UiActions.NODE_RECALL_ALL),
                HyUiActionBinding.action("#PillageButton", UiActions.NODE_PILLAGE),
                HyUiActionBinding.action("#BackButton", UiActions.OPEN_RESOURCES)
        );
    }
}
