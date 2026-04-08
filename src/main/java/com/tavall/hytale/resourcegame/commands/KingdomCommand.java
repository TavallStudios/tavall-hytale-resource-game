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
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;
import com.tavall.hytale.resourcegame.resources.ResourceType;
import com.tavall.hytale.resourcegame.services.CastleSpawnService;
import com.tavall.hytale.resourcegame.services.CastlePromptLaneService;
import com.tavall.hytale.resourcegame.services.InteriorWorldService;
import com.tavall.hytale.resourcegame.services.PlayerDataService;
import com.tavall.hytale.resourcegame.services.PlayerSession;
import com.tavall.hytale.resourcegame.services.PlayerSessionStore;
import com.tavall.hytale.resourcegame.services.PopulationService;
import com.tavall.hytale.resourcegame.services.ResourceService;
import com.tavall.hytale.resourcegame.ui.UiPageType;
import com.tavall.hytale.resourcegame.ui.UiNavigator;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;

/**
 * Debug command entry for the kingdom prototype.
 */
public final class KingdomCommand extends AbstractAsyncCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final PlayerSessionStore sessionStore;
    private final UiNavigator uiNavigator;
    private final PopulationService populationService;
    private final ResourceService resourceService;
    private final InteriorWorldService interiorWorldService;
    private final CastleSpawnService castleSpawnService;
    private final CastlePromptLaneService castlePromptLaneService;
    private final PlayerDataService playerDataService;

    public KingdomCommand(
            String name,
            PlayerSessionStore sessionStore,
            UiNavigator uiNavigator,
            PopulationService populationService,
            ResourceService resourceService,
            InteriorWorldService interiorWorldService,
            CastleSpawnService castleSpawnService,
            CastlePromptLaneService castlePromptLaneService,
            PlayerDataService playerDataService
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
        int amount = parseInt(tokens.get(2));
        if ("add".equalsIgnoreCase(action)) {
            populationService.addCitizens(playerId, amount);
        } else if ("set".equalsIgnoreCase(action)) {
            populationService.setCitizens(playerId, amount);
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
        int amount = parseInt(tokens.get(2));
        if ("add".equalsIgnoreCase(action)) {
            populationService.addTroops(playerId, amount);
        } else if ("set".equalsIgnoreCase(action)) {
            populationService.setTroops(playerId, amount);
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
        int amount = parseInt(tokens.get(3));
        if (type == null) {
            context.sendMessage(Message.raw("Unknown resource type.").color("red"));
            return;
        }
        if ("add".equalsIgnoreCase(action)) {
            resourceService.addResource(playerId, type, amount);
        } else if ("set".equalsIgnoreCase(action)) {
            resourceService.setResource(playerId, type, amount);
        } else {
            context.sendMessage(Message.raw("Unknown action.").color("red"));
        }
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

    private int parseInt(String token) {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
