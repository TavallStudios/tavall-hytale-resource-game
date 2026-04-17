package com.tavall.hytale.resourcegame.commands;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IFocusedWorldInteractionService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IFocusedWorldOverrideService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlacementModeService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerTeleportService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodePromptLaneService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiNavigator;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.ResourceNodeData;
import com.tavall.hytale.resourcegame.domain.ResourceNodePillageResult;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;
import com.tavall.hytale.resourcegame.resources.ResourceType;
import com.tavall.hytale.resourcegame.services.PlayerSession;
import com.tavall.hytale.resourcegame.ui.UiPageType;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Handles `/kd nodes ...` command flows.
 */
public final class KingdomNodeCommandSupport implements IDependencyInjectableConcrete {
    private final IResourceNodeService resourceNodeService;
    private final IResourceNodeVisualService resourceNodeVisualService;
    private final IUiNavigator uiNavigator;
    private final IPlayerTeleportService playerTeleportService;
    private final IPlacementModeService placementModeService;
    private final IResourceNodePromptLaneService promptLaneService;
    private final IFocusedWorldInteractionService focusedWorldInteractionService;
    private final IFocusedWorldOverrideService focusedWorldOverrideService;

    public KingdomNodeCommandSupport(
            IResourceNodeService resourceNodeService,
            IResourceNodeVisualService resourceNodeVisualService,
            IUiNavigator uiNavigator,
            IPlayerTeleportService playerTeleportService,
            IPlacementModeService placementModeService,
            IResourceNodePromptLaneService promptLaneService,
            IFocusedWorldInteractionService focusedWorldInteractionService,
            IFocusedWorldOverrideService focusedWorldOverrideService
    ) {
        this.resourceNodeService = Objects.requireNonNull(resourceNodeService, "resourceNodeService");
        this.resourceNodeVisualService = Objects.requireNonNull(resourceNodeVisualService, "resourceNodeVisualService");
        this.uiNavigator = Objects.requireNonNull(uiNavigator, "uiNavigator");
        this.playerTeleportService = Objects.requireNonNull(playerTeleportService, "playerTeleportService");
        this.placementModeService = Objects.requireNonNull(placementModeService, "placementModeService");
        this.promptLaneService = Objects.requireNonNull(promptLaneService, "promptLaneService");
        this.focusedWorldInteractionService = Objects.requireNonNull(focusedWorldInteractionService, "focusedWorldInteractionService");
        this.focusedWorldOverrideService = Objects.requireNonNull(focusedWorldOverrideService, "focusedWorldOverrideService");
    }

    public void handle(CommandContext context, Player player, List<String> tokens, PlayerSession session) {
        if (tokens.size() < 2) {
            context.sendMessage(Message.raw("Usage: /kd nodes place|list|status|select|align|assign|add|pillage|stock|recall|goto|remove|clear").color("yellow"));
            return;
        }
        String action = tokens.get(1).toLowerCase(Locale.ROOT);
        switch (action) {
            case "place" -> handleNodePlacement(context, player, tokens);
            case "list" -> handleNodeList(context, session.gameState());
            case "status" -> handleNodeStatus(context, player, tokens, session);
            case "select" -> handleNodeSelect(context, player, tokens, session);
            case "align" -> handleNodeAlign(context, player, tokens, session);
            case "assign" -> handleNodeAssign(context, tokens, session, false);
            case "add" -> handleNodeAssign(context, tokens, session, true);
            case "pillage" -> handleNodePillage(context, player, tokens, session);
            case "stock" -> handleNodeStock(context, tokens, session);
            case "recall" -> handleNodeRecall(context, tokens, session);
            case "goto" -> handleNodeGoto(context, player, tokens, session);
            case "remove" -> handleNodeRemove(context, tokens, session);
            case "clear" -> handleNodeClear(context, session);
            default -> context.sendMessage(Message.raw("Unknown nodes action.").color("red"));
        }
    }

    private void handleNodePlacement(CommandContext context, Player player, List<String> tokens) {
        if (tokens.size() < 3) {
            context.sendMessage(Message.raw("Usage: /kd nodes place <food|wood|iron>").color("yellow"));
            return;
        }
        ResourceType resourceType = parseResource(tokens.get(2));
        if (resourceType == null) {
            context.sendMessage(Message.raw("Unknown resource type.").color("red"));
            return;
        }
        placementModeService.armNodePlacement(player, resourceType);
        Vector3i currentBlock = currentStandingBlock(player);
        if (currentBlock == null) {
            context.sendMessage(Message.raw("Unable to resolve current standing block.").color("red"));
            return;
        }
        var result = placementModeService.confirmPlacement(player, currentBlock);
        context.sendMessage(Message.raw(result.message()).color(result.success() ? "green" : "red"));
    }

    private void handleNodeList(CommandContext context, PlayerGameState state) {
        List<ResourceNodeData> nodes = resourceNodeService.listNodes(state);
        if (nodes.isEmpty()) {
            context.sendMessage(Message.raw("No placed nodes. Use /kd place node <type>.").color("yellow"));
            return;
        }
        for (int index = 0; index < nodes.size(); index++) {
            context.sendMessage(Message.raw(resourceNodeService.summaryLine(state, nodes.get(index), index + 1)).color("yellow"));
        }
    }

    private void handleNodeSelect(CommandContext context, Player player, List<String> tokens, PlayerSession session) {
        if (tokens.size() < 3) {
            context.sendMessage(Message.raw("Usage: /kd nodes select <index|node_id_prefix|focus>").color("yellow"));
            return;
        }
        Optional<ResourceNodeData> node = resolveNode(context, player, session, tokens.get(2));
        if (node.isEmpty()) {
            return;
        }
        uiNavigator.open(
                UiPageType.RESOURCE_NODE_DETAIL,
                player,
                new UiNavigationContext(player.getUuid(), player.getDisplayName()).withSelectedNodeId(node.get().nodeId()),
                session.gameState()
        );
    }

    private void handleNodeStatus(CommandContext context, Player player, List<String> tokens, PlayerSession session) {
        if (tokens.size() < 3) {
            context.sendMessage(Message.raw("Usage: /kd nodes status <index|node_id_prefix|focus>").color("yellow"));
            return;
        }
        Optional<ResourceNodeData> node = resolveNode(context, player, session, tokens.get(2));
        if (node.isEmpty()) {
            return;
        }
        sendNodeSummary(context, session.gameState(), node.get().nodeId());
    }

    private void handleNodeAlign(CommandContext context, Player player, List<String> tokens, PlayerSession session) {
        if (tokens.size() < 3) {
            context.sendMessage(Message.raw("Usage: /kd nodes align <index|node_id_prefix|focus>").color("yellow"));
            return;
        }
        Optional<ResourceNodeData> node = resolveNode(context, player, session, tokens.get(2));
        if (node.isEmpty()) {
            return;
        }
        promptLaneService.alignPlayer(player, node.get());
        focusedWorldOverrideService.markNode(session.playerId(), node.get().nodeId());
        context.sendMessage(Message.raw("Aligned player with node prompt lane for " + node.get().resourceType() + ".").color("green"));
    }

    private void handleNodeAssign(CommandContext context, List<String> tokens, PlayerSession session, boolean deltaMode) {
        if (tokens.size() < 4) {
            context.sendMessage(Message.raw(deltaMode ? "Usage: /kd nodes add <index|node_id_prefix> <amount>" : "Usage: /kd nodes assign <index|node_id_prefix> <amount>").color("yellow"));
            return;
        }
        Optional<ResourceNodeData> node = resourceNodeService.resolveNode(session.gameState(), tokens.get(2));
        if (node.isEmpty()) {
            context.sendMessage(Message.raw("Node not found.").color("red"));
            return;
        }
        OptionalInt amount = parseAmount(tokens.get(3));
        if (amount.isEmpty()) {
            context.sendMessage(Message.raw("Amount must be a whole number.").color("red"));
            return;
        }
        PlayerGameState updatedState = deltaMode
                ? resourceNodeService.addTroops(session.playerId(), node.get().nodeId(), amount.getAsInt(), Instant.now())
                : resourceNodeService.assignTroops(session.playerId(), node.get().nodeId(), amount.getAsInt(), Instant.now());
        resourceNodeVisualService.refreshNodes(session.playerId(), updatedState);
        uiNavigator.refreshTrackedPage(session.playerId(), updatedState);
        sendNodeSummary(context, updatedState, node.get().nodeId());
    }

    private void handleNodeRecall(CommandContext context, List<String> tokens, PlayerSession session) {
        if (tokens.size() < 3) {
            context.sendMessage(Message.raw("Usage: /kd nodes recall <index|node_id_prefix> [amount|all]").color("yellow"));
            return;
        }
        Optional<ResourceNodeData> node = resourceNodeService.resolveNode(session.gameState(), tokens.get(2));
        if (node.isEmpty()) {
            context.sendMessage(Message.raw("Node not found.").color("red"));
            return;
        }
        PlayerGameState updatedState;
        if (tokens.size() >= 4 && "all".equalsIgnoreCase(tokens.get(3))) {
            updatedState = resourceNodeService.assignTroops(session.playerId(), node.get().nodeId(), 0, Instant.now());
        } else if (tokens.size() >= 4) {
            OptionalInt amount = parseAmount(tokens.get(3));
            if (amount.isEmpty()) {
                context.sendMessage(Message.raw("Amount must be a whole number.").color("red"));
                return;
            }
            updatedState = resourceNodeService.addTroops(session.playerId(), node.get().nodeId(), -amount.getAsInt(), Instant.now());
        } else {
            updatedState = resourceNodeService.addTroops(session.playerId(), node.get().nodeId(), -1, Instant.now());
        }
        resourceNodeVisualService.refreshNodes(session.playerId(), updatedState);
        uiNavigator.refreshTrackedPage(session.playerId(), updatedState);
        sendNodeSummary(context, updatedState, node.get().nodeId());
    }

    private void handleNodeStock(CommandContext context, List<String> tokens, PlayerSession session) {
        if (tokens.size() < 4) {
            context.sendMessage(Message.raw("Usage: /kd nodes stock <index|node_id_prefix> <amount>").color("yellow"));
            return;
        }
        Optional<ResourceNodeData> node = resourceNodeService.resolveNode(session.gameState(), tokens.get(2));
        if (node.isEmpty()) {
            context.sendMessage(Message.raw("Node not found.").color("red"));
            return;
        }
        OptionalInt amount = parseAmount(tokens.get(3));
        if (amount.isEmpty()) {
            context.sendMessage(Message.raw("Amount must be a whole number.").color("red"));
            return;
        }
        PlayerGameState updatedState = resourceNodeService.setStock(session.playerId(), node.get().nodeId(), amount.getAsInt(), Instant.now());
        resourceNodeVisualService.refreshNodes(session.playerId(), updatedState);
        uiNavigator.refreshTrackedPage(session.playerId(), updatedState);
        sendNodeSummary(context, updatedState, node.get().nodeId());
    }

    private void handleNodePillage(CommandContext context, Player player, List<String> tokens, PlayerSession session) {
        if (tokens.size() < 3) {
            context.sendMessage(Message.raw("Usage: /kd nodes pillage <index|node_id_prefix|focus>").color("yellow"));
            return;
        }
        Optional<ResourceNodeData> node = resolveNode(context, player, session, tokens.get(2));
        if (node.isEmpty()) {
            return;
        }
        ResourceNodePillageResult pillageResult = resourceNodeService.pillageNode(session.playerId(), node.get().nodeId(), Instant.now());
        PlayerGameState updatedState = pillageResult.state();
        if (updatedState == null) {
            context.sendMessage(Message.raw(pillageResult.message()).color("red"));
            return;
        }
        resourceNodeVisualService.refreshNodes(session.playerId(), updatedState);
        uiNavigator.refreshTrackedPage(session.playerId(), updatedState);
        context.sendMessage(Message.raw(pillageResult.message()).color(pillageResult.changed() ? "green" : "yellow"));
        sendNodeSummary(context, updatedState, node.get().nodeId());
    }

    private void handleNodeGoto(CommandContext context, Player player, List<String> tokens, PlayerSession session) {
        if (tokens.size() < 3) {
            context.sendMessage(Message.raw("Usage: /kd nodes goto <index|node_id_prefix>").color("yellow"));
            return;
        }
        Optional<ResourceNodeData> node = resourceNodeService.resolveNode(session.gameState(), tokens.get(2));
        if (node.isEmpty()) {
            context.sendMessage(Message.raw("Node not found.").color("red"));
            return;
        }
        playerTeleportService.teleport(player, playerTeleportService.standingPosition(player, node.get().location().standingBaseVector()));
        context.sendMessage(Message.raw("Teleported to node " + node.get().nodeId().toString().substring(0, 8) + ".").color("green"));
    }

    private void handleNodeRemove(CommandContext context, List<String> tokens, PlayerSession session) {
        if (tokens.size() < 3) {
            context.sendMessage(Message.raw("Usage: /kd nodes remove <index|node_id_prefix>").color("yellow"));
            return;
        }
        Optional<ResourceNodeData> node = resourceNodeService.resolveNode(session.gameState(), tokens.get(2));
        if (node.isEmpty()) {
            context.sendMessage(Message.raw("Node not found.").color("red"));
            return;
        }
        PlayerGameState updatedState = resourceNodeService.removeNode(session.playerId(), node.get().nodeId(), Instant.now());
        resourceNodeVisualService.refreshNodes(session.playerId(), updatedState);
        uiNavigator.refreshTrackedPage(session.playerId(), updatedState);
        context.sendMessage(Message.raw("Removed node " + node.get().nodeId().toString().substring(0, 8) + ".").color("green"));
    }

    private void handleNodeClear(CommandContext context, PlayerSession session) {
        PlayerGameState updatedState = resourceNodeService.clearNodes(session.playerId(), Instant.now());
        resourceNodeVisualService.refreshNodes(session.playerId(), updatedState);
        uiNavigator.refreshTrackedPage(session.playerId(), updatedState);
        context.sendMessage(Message.raw("Cleared all placed nodes.").color("green"));
    }

    private void sendNodeSummary(CommandContext context, PlayerGameState state, UUID nodeId) {
        Optional<ResourceNodeData> node = resourceNodeService.findNode(state, nodeId);
        if (node.isEmpty()) {
            context.sendMessage(Message.raw("Node updated.").color("green"));
            return;
        }
        int index = resourceNodeService.listNodes(state).indexOf(node.get()) + 1;
        context.sendMessage(Message.raw(resourceNodeService.summaryLine(state, node.get(), index)).color("green"));
    }

    private ResourceType parseResource(String token) {
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "food" -> ResourceType.FOOD;
            case "wood" -> ResourceType.WOOD;
            case "iron" -> ResourceType.IRON;
            default -> null;
        };
    }

    private OptionalInt parseAmount(String token) {
        try {
            return OptionalInt.of(Integer.parseInt(token));
        } catch (NumberFormatException ex) {
            return OptionalInt.empty();
        }
    }

    private Optional<ResourceNodeData> resolveNode(CommandContext context, Player player, PlayerSession session, String token) {
        if ("focus".equalsIgnoreCase(token)) {
            Optional<UUID> focusedNodeId = focusedWorldInteractionService.focusedNodeId(player);
            if (focusedNodeId.isEmpty()) {
                context.sendMessage(Message.raw("No focused node in front of you.").color("red"));
                return Optional.empty();
            }
            return resourceNodeService.findNode(session.gameState(), focusedNodeId.get());
        }
        Optional<ResourceNodeData> node = resourceNodeService.resolveNode(session.gameState(), token);
        if (node.isEmpty()) {
            context.sendMessage(Message.raw("Node not found.").color("red"));
        }
        return node;
    }

    private Vector3i currentStandingBlock(Player player) {
        TransformComponent transform = player.getTransformComponent();
        if (transform == null || transform.getPosition() == null) {
            return null;
        }
        Vector3d position = transform.getPosition();
        return new Vector3i(
                (int) Math.floor(position.getX()),
                (int) Math.floor(position.getY()) - 1,
                (int) Math.floor(position.getZ())
        );
    }
}
