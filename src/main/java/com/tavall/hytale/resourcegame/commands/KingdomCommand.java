package com.tavall.hytale.resourcegame.commands;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastlePromptLaneService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleSpawnService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInfrastructureHealthService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInteriorWorldService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerDataService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerGameStateService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerSessionStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPopulationService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiNavigator;
import com.tavall.hytale.resourcegame.domain.InfrastructureHealthSnapshot;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.ResourceNodeData;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;
import com.tavall.hytale.resourcegame.resources.ResourceType;
import com.tavall.hytale.resourcegame.services.PlayerSession;
import com.tavall.hytale.resourcegame.ui.UiPageType;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;

/**
 * Debug command entry for the kingdom prototype.
 */
public final class KingdomCommand extends AbstractAsyncCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final IPlayerSessionStore sessionStore;
    private final IUiNavigator uiNavigator;
    private final IPopulationService populationService;
    private final IResourceService resourceService;
    private final IInteriorWorldService interiorWorldService;
    private final ICastleSpawnService castleSpawnService;
    private final ICastlePromptLaneService castlePromptLaneService;
    private final IPlayerDataService playerDataService;
    private final IPlayerGameStateService gameStateService;
    private final IInfrastructureHealthService infrastructureHealthService;
    private final IResourceNodeService resourceNodeService;
    private final IResourceNodeVisualService resourceNodeVisualService;

    public KingdomCommand(
            String name,
            IPlayerSessionStore sessionStore,
            IUiNavigator uiNavigator,
            IPopulationService populationService,
            IResourceService resourceService,
            IInteriorWorldService interiorWorldService,
            ICastleSpawnService castleSpawnService,
            ICastlePromptLaneService castlePromptLaneService,
            IPlayerDataService playerDataService,
            IPlayerGameStateService gameStateService,
            IInfrastructureHealthService infrastructureHealthService,
            IResourceNodeService resourceNodeService,
            IResourceNodeVisualService resourceNodeVisualService
    ) {
        super(name, "Kingdom debug command");
        this.sessionStore = sessionStore;
        this.uiNavigator = uiNavigator;
        this.populationService = populationService;
        this.resourceService = resourceService;
        this.interiorWorldService = interiorWorldService;
        this.castleSpawnService = castleSpawnService;
        this.castlePromptLaneService = castlePromptLaneService;
        this.playerDataService = playerDataService;
        this.gameStateService = gameStateService;
        this.infrastructureHealthService = infrastructureHealthService;
        this.resourceNodeService = resourceNodeService;
        this.resourceNodeVisualService = resourceNodeVisualService;
        addAliases("kd");
        setPermissionGroup(GameMode.Adventure);
        setAllowsExtraArguments(true);
    }

    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext context) {
        if (!(context.sender() instanceof Player player)) {
            context.sendMessage(Message.raw("Player-only command.").color("red"));
            return CompletableFuture.completedFuture(null);
        }

        List<String> tokens = CommandTokens.tokens(context);
        Executor executor = resolveCommandExecutor(player);
        return playerDataService.ensureSession(player)
                .thenCompose(ignored -> CompletableFuture.runAsync(() -> {
                    if (tokens.isEmpty()) {
                        sendHelp(context);
                        return;
                    }

                    String root = tokens.getFirst().toLowerCase(Locale.ROOT);
                    PlayerSession session = sessionStore.get(player.getUuid());
                    if (session == null) {
                        LOGGER.at(Level.WARNING).log("Command %s rejected for %s because the session is not ready after bootstrap.", tokens, player.getDisplayName());
                        context.sendMessage(Message.raw("Player session not ready.").color("red"));
                        return;
                    }

                    LOGGER.at(Level.INFO).log("Handling kingdom command %s for %s.", tokens, player.getDisplayName());

                    switch (root) {
                        case "ui" -> handleUi(context, player, tokens, session);
                        case "data" -> handleData(context, session);
                        case "castle" -> handleCastle(context, player, tokens, session);
                        case "interior" -> handleInterior(player, tokens);
                        case "citizens" -> handleCitizens(context, tokens, player.getUuid());
                        case "troops" -> handleTroops(context, tokens, player.getUuid());
                        case "resources" -> handleResources(context, tokens, player.getUuid());
                        case "nodes" -> handleNodes(context, player, tokens, session);
                        case "debug" -> sendHelp(context);
                        default -> sendHelp(context);
                    }
                }, executor));
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    private void handleUi(CommandContext context, Player player, List<String> tokens, PlayerSession session) {
        UiNavigationContext navContext = new UiNavigationContext(player.getUuid(), player.getDisplayName());
        PlayerGameState state = session.gameState();
        if (tokens.size() == 1) {
            uiNavigator.open(UiPageType.DEBUG_NAVIGATOR, player, navContext, state);
            return;
        }
        UiPageType type = parseUiType(tokens.get(1));
        if (type == null) {
            context.sendMessage(Message.raw("Unknown UI type.").color("red"));
            return;
        }
        uiNavigator.open(type, player, navContext, state);
    }

    private void handleData(CommandContext context, PlayerSession session) {
        PlayerGameState state = session.gameState();
        InfrastructureHealthSnapshot healthSnapshot = infrastructureHealthService.snapshot();
        if (state.castleLocation() != null) {
            context.sendMessage(Message.raw(
                    "Castle: "
                            + state.castleLocation().worldName()
                            + " "
                            + state.castleLocation().x()
                            + " "
                            + state.castleLocation().y()
                            + " "
                            + state.castleLocation().z()
            ).color("yellow"));
        }
        context.sendMessage(Message.raw("Citizens: " + state.populationSummary().citizenCount()).color("yellow"));
        context.sendMessage(Message.raw("Troops: " + state.populationSummary().troopCount()).color("yellow"));
        context.sendMessage(Message.raw("Food: " + state.resources().food()).color("yellow"));
        context.sendMessage(Message.raw("Wood: " + state.resources().wood()).color("yellow"));
        context.sendMessage(Message.raw("Iron: " + state.resources().iron()).color("yellow"));
        context.sendMessage(Message.raw("Nodes: " + resourceNodeService.listNodes(state).size()).color("yellow"));
        context.sendMessage(Message.raw("Assigned troops: " + resourceNodeService.assignedTroops(state)).color("yellow"));
        context.sendMessage(Message.raw("Reserve troops: " + resourceNodeService.availableTroops(state)).color("yellow"));
        context.sendMessage(Message.raw("Cache: " + healthSnapshot.cacheSummary()).color("yellow"));
        context.sendMessage(Message.raw("Persistence: " + healthSnapshot.persistenceSummary()).color("yellow"));
        context.sendMessage(Message.raw("Interior tutorial: " + tutorialStatus(gameStateService.isInteriorTutorialPending(state))).color("yellow"));
        context.sendMessage(Message.raw("Interior tour: " + tutorialStatus(gameStateService.isInteriorTourPending(state))).color("yellow"));
        context.sendMessage(Message.raw("Upgrade tutorial: " + tutorialStatus(gameStateService.isUpgradeTutorialPending(state))).color("yellow"));
    }

    private void handleCastle(CommandContext context, Player player, List<String> tokens, PlayerSession session) {
        if (tokens.size() > 1 && "align".equalsIgnoreCase(tokens.get(1))) {
            PlayerGameState state = session.gameState();
            if (state.castleLocation() == null) {
                context.sendMessage(Message.raw("Castle data not available.").color("red"));
                return;
            }
            castlePromptLaneService.alignPlayer(player, state.castleLocation());
            context.sendMessage(Message.raw("Aligned player with castle prompt lane.").color("green"));
            return;
        }
        castleSpawnService.ensureCastleSpawned(player, session.gameState().castleLocation());
        context.sendMessage(Message.raw("Castle spawn requested.").color("green"));
    }

    private void handleInterior(Player player, List<String> tokens) {
        if (tokens.size() > 1 && "exit".equalsIgnoreCase(tokens.get(1))) {
            interiorWorldService.exitInterior(player);
        } else {
            interiorWorldService.enterInterior(player);
        }
    }

    private void handleCitizens(CommandContext context, List<String> tokens, UUID playerId) {
        if (tokens.size() < 3) {
            context.sendMessage(Message.raw("Usage: /kd citizens add|set <amount>").color("yellow"));
            return;
        }
        String action = tokens.get(1);
        OptionalInt amount = parseAmount(tokens.get(2));
        if (amount.isEmpty()) {
            context.sendMessage(Message.raw("Amount must be a whole number.").color("red"));
            return;
        }
        if ("add".equalsIgnoreCase(action)) {
            populationService.addCitizens(playerId, amount.getAsInt());
        } else if ("set".equalsIgnoreCase(action)) {
            populationService.setCitizens(playerId, amount.getAsInt());
        } else {
            context.sendMessage(Message.raw("Unknown action.").color("red"));
        }
    }

    private void handleTroops(CommandContext context, List<String> tokens, UUID playerId) {
        if (tokens.size() < 3) {
            context.sendMessage(Message.raw("Usage: /kd troops add|set <amount>").color("yellow"));
            return;
        }
        String action = tokens.get(1);
        OptionalInt amount = parseAmount(tokens.get(2));
        if (amount.isEmpty()) {
            context.sendMessage(Message.raw("Amount must be a whole number.").color("red"));
            return;
        }
        if ("add".equalsIgnoreCase(action)) {
            populationService.addTroops(playerId, amount.getAsInt());
        } else if ("set".equalsIgnoreCase(action)) {
            populationService.setTroops(playerId, amount.getAsInt());
        } else {
            context.sendMessage(Message.raw("Unknown action.").color("red"));
        }
    }

    private void handleResources(CommandContext context, List<String> tokens, UUID playerId) {
        if (tokens.size() < 4) {
            context.sendMessage(Message.raw("Usage: /kd resources add|set <type> <amount>").color("yellow"));
            return;
        }
        String action = tokens.get(1);
        ResourceType type = parseResource(tokens.get(2));
        if (type == null) {
            context.sendMessage(Message.raw("Unknown resource type.").color("red"));
            return;
        }
        OptionalInt amount = parseAmount(tokens.get(3));
        if (amount.isEmpty()) {
            context.sendMessage(Message.raw("Amount must be a whole number.").color("red"));
            return;
        }
        if ("add".equalsIgnoreCase(action)) {
            resourceService.addResource(playerId, type, amount.getAsInt());
        } else if ("set".equalsIgnoreCase(action)) {
            resourceService.setResource(playerId, type, amount.getAsInt());
        } else {
            context.sendMessage(Message.raw("Unknown action.").color("red"));
        }
    }

    private void handleNodes(CommandContext context, Player player, List<String> tokens, PlayerSession session) {
        if (tokens.size() < 2) {
            context.sendMessage(Message.raw("Usage: /kd nodes place|list|select|assign|add|stock|recall|remove|clear").color("yellow"));
            return;
        }
        String action = tokens.get(1).toLowerCase(Locale.ROOT);
        switch (action) {
            case "place" -> handleNodePlacement(context, player, tokens);
            case "list" -> handleNodeList(context, session.gameState());
            case "select" -> handleNodeSelect(context, player, tokens, session);
            case "assign" -> handleNodeAssign(context, tokens, session, false);
            case "add" -> handleNodeAssign(context, tokens, session, true);
            case "stock" -> handleNodeStock(context, tokens, session);
            case "recall" -> handleNodeRecall(context, tokens, session);
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
        var position = player.getTransformComponent().getPosition();
        PlayerGameState updatedState = resourceNodeService.placeNode(
                player.getUuid(),
                resourceType,
                player.getWorld().getName(),
                position,
                java.time.Instant.now()
        );
        if (updatedState == null) {
            context.sendMessage(Message.raw("Unable to place node.").color("red"));
            return;
        }
        resourceNodeVisualService.refreshNodes(player.getUuid(), updatedState);
        ResourceNodeData latestNode = resourceNodeService.listNodes(updatedState).getLast();
        context.sendMessage(Message.raw("Placed " + resourceType + " node #" + resourceNodeService.listNodes(updatedState).size() + " " + latestNode.nodeId().toString().substring(0, 8)).color("green"));
    }

    private void handleNodeList(CommandContext context, PlayerGameState state) {
        List<ResourceNodeData> nodes = resourceNodeService.listNodes(state);
        if (nodes.isEmpty()) {
            context.sendMessage(Message.raw("No placed nodes. Use /kd nodes place <type>.").color("yellow"));
            return;
        }
        for (int index = 0; index < nodes.size(); index++) {
            context.sendMessage(Message.raw(resourceNodeService.summaryLine(state, nodes.get(index), index + 1)).color("yellow"));
        }
    }

    private void handleNodeSelect(CommandContext context, Player player, List<String> tokens, PlayerSession session) {
        if (tokens.size() < 3) {
            context.sendMessage(Message.raw("Usage: /kd nodes select <index|node_id_prefix>").color("yellow"));
            return;
        }
        Optional<ResourceNodeData> node = resourceNodeService.resolveNode(session.gameState(), tokens.get(2));
        if (node.isEmpty()) {
            context.sendMessage(Message.raw("Node not found.").color("red"));
            return;
        }
        uiNavigator.open(
                UiPageType.RESOURCE_NODE_DETAIL,
                player,
                new UiNavigationContext(player.getUuid(), player.getDisplayName()).withSelectedNodeId(node.get().nodeId()),
                session.gameState()
        );
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
                ? resourceNodeService.addTroops(session.playerId(), node.get().nodeId(), amount.getAsInt(), java.time.Instant.now())
                : resourceNodeService.assignTroops(session.playerId(), node.get().nodeId(), amount.getAsInt(), java.time.Instant.now());
        resourceNodeVisualService.refreshNodes(session.playerId(), updatedState);
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
            updatedState = resourceNodeService.assignTroops(session.playerId(), node.get().nodeId(), 0, java.time.Instant.now());
        } else if (tokens.size() >= 4) {
            OptionalInt amount = parseAmount(tokens.get(3));
            if (amount.isEmpty()) {
                context.sendMessage(Message.raw("Amount must be a whole number.").color("red"));
                return;
            }
            updatedState = resourceNodeService.addTroops(session.playerId(), node.get().nodeId(), -amount.getAsInt(), java.time.Instant.now());
        } else {
            updatedState = resourceNodeService.addTroops(session.playerId(), node.get().nodeId(), -1, java.time.Instant.now());
        }
        resourceNodeVisualService.refreshNodes(session.playerId(), updatedState);
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
        PlayerGameState updatedState = resourceNodeService.setStock(session.playerId(), node.get().nodeId(), amount.getAsInt(), java.time.Instant.now());
        resourceNodeVisualService.refreshNodes(session.playerId(), updatedState);
        sendNodeSummary(context, updatedState, node.get().nodeId());
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
        PlayerGameState updatedState = resourceNodeService.removeNode(session.playerId(), node.get().nodeId(), java.time.Instant.now());
        resourceNodeVisualService.refreshNodes(session.playerId(), updatedState);
        context.sendMessage(Message.raw("Removed node " + node.get().nodeId().toString().substring(0, 8) + ".").color("green"));
    }

    private void handleNodeClear(CommandContext context, PlayerSession session) {
        PlayerGameState updatedState = resourceNodeService.clearNodes(session.playerId(), java.time.Instant.now());
        resourceNodeVisualService.refreshNodes(session.playerId(), updatedState);
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

    private void sendHelp(CommandContext context) {
        context.sendMessage(Message.raw("/kingdom, /kd help:").color("yellow"));
        context.sendMessage(Message.raw("/kd ui [ui_type]").color("yellow"));
        context.sendMessage(Message.raw("/kd data").color("yellow"));
        context.sendMessage(Message.raw("/kd castle [align]").color("yellow"));
        context.sendMessage(Message.raw("/kd interior [exit]").color("yellow"));
        context.sendMessage(Message.raw("/kd citizens add|set <amount>").color("yellow"));
        context.sendMessage(Message.raw("/kd troops add|set <amount>").color("yellow"));
        context.sendMessage(Message.raw("/kd resources add|set <type> <amount>").color("yellow"));
        context.sendMessage(Message.raw("/kd nodes place|list|select|assign|add|stock|recall|remove|clear").color("yellow"));
    }

    private Executor resolveCommandExecutor(Player player) {
        if (player.getWorld() != null) {
            return player.getWorld();
        }
        if (player.getPlayerRef() != null) {
            var ref = player.getPlayerRef().getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                if (store.getExternalData() instanceof EntityStore entityStore) {
                    World world = entityStore.getWorld();
                    if (world != null) {
                        return world;
                    }
                }
            }
        }
        return Runnable::run;
    }

    private UiPageType parseUiType(String token) {
        String normalized = token.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "castle", "main" -> UiPageType.CASTLE_MAIN;
            case "info" -> UiPageType.CASTLE_INFO;
            case "citizens" -> UiPageType.CASTLE_CITIZENS;
            case "troops" -> UiPageType.CASTLE_TROOPS;
            case "resources" -> UiPageType.CASTLE_RESOURCES;
            case "upgrades" -> UiPageType.CASTLE_UPGRADES;
            case "interior" -> UiPageType.INTERIOR_MAIN;
            case "debug" -> UiPageType.DEBUG_NAVIGATOR;
            default -> null;
        };
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

    private String tutorialStatus(boolean pending) {
        return pending ? "pending" : "complete";
    }
}
