package com.tavall.hytale.resourcegame.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiActionService;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.ResourceNodeData;
import com.tavall.hytale.resourcegame.domain.ResourceNodeSummary;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;

import java.util.Optional;

/**
 * UI for assigning troop counts to a selected resource node.
 */
public final class ResourceNodePage extends BaseUiPage {
    private static final String PAGE_DOCUMENT = "Pages/resource-node-detail.ui";

    private final IResourceNodeService resourceNodeService;

    public ResourceNodePage(
            Player player,
            UiNavigationContext context,
            PlayerGameState state,
            IUiActionService actionService,
            IResourceNodeService resourceNodeService
    ) {
        super(player, context, state, actionService);
        this.resourceNodeService = resourceNodeService;
    }

    @Override
    public void build(Ref<EntityStore> entityRef, UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder, Store<EntityStore> entityStore) {
        uiCommandBuilder.append(PAGE_DOCUMENT);
        Optional<ResourceNodeData> nodeOptional = resourceNodeService.findNode(state(), context().selectedNodeId());
        if (nodeOptional.isEmpty()) {
            uiCommandBuilder.set("#NodeTitle.Text", "Node not found");
            uiCommandBuilder.set("#NodeSummary.Text", "Select a node from /kd nodes list or click a node in-world.");
            uiCommandBuilder.set("#AssignedTroops.Text", "0");
            uiCommandBuilder.set("#AvailableTroops.Text", String.valueOf(resourceNodeService.availableTroops(state())));
            uiCommandBuilder.set("#GainPerTick.Text", "+0/tick");
            uiCommandBuilder.set("#FeedbackStatus.Text", "No node selected.");
        } else {
            ResourceNodeData node = nodeOptional.get();
            ResourceNodeSummary summary = resourceNodeService.summary(state(), node);
            uiCommandBuilder.set("#NodeTitle.Text", node.resourceType() + " Node " + node.nodeId().toString().substring(0, 8));
            uiCommandBuilder.set("#NodeSummary.Text", node.worldName() + " | " + (int) node.x() + ", " + (int) node.y() + ", " + (int) node.z());
            uiCommandBuilder.set("#AssignedTroops.Text", String.valueOf(summary.assignedTroops()));
            uiCommandBuilder.set("#AvailableTroops.Text", String.valueOf(summary.availableTroops()));
            uiCommandBuilder.set("#GainPerTick.Text", "+" + summary.gainPerTick() + "/tick");
            uiCommandBuilder.set("#FeedbackStatus.Text", context().feedbackMessage().isBlank() ? "Send troops here to pull in extra " + node.resourceType().name().toLowerCase() + "." : context().feedbackMessage());
        }

        bind(uiEventBuilder, "#AssignOneButton", UiActions.NODE_ASSIGN_ONE);
        bind(uiEventBuilder, "#AssignThreeButton", UiActions.NODE_ASSIGN_THREE);
        bind(uiEventBuilder, "#AssignFiveButton", UiActions.NODE_ASSIGN_FIVE);
        bind(uiEventBuilder, "#AssignAllButton", UiActions.NODE_ASSIGN_ALL);
        bind(uiEventBuilder, "#RecallOneButton", UiActions.NODE_RECALL_ONE);
        bind(uiEventBuilder, "#RecallAllButton", UiActions.NODE_RECALL_ALL);
        bind(uiEventBuilder, "#BackButton", UiActions.OPEN_RESOURCES);
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
