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
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleEconomySimulationService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleBuildingService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleBuildingVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastlePromptLaneService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleSiteVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleSpawnService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInfrastructureHealthService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInteriorWorldService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlacementModeService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerDataService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerGameStateService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerSessionStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerTeleportService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPopulationService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiNavigator;
import com.tavall.hytale.resourcegame.domain.InfrastructureHealthSnapshot;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;
import com.tavall.hytale.resourcegame.resources.ResourceType;
import com.tavall.hytale.resourcegame.services.CastleEconomySimulationService;
import com.tavall.hytale.resourcegame.services.PlayerSession;
import com.tavall.hytale.resourcegame.tasks.AsyncTask;
import com.tavall.hytale.resourcegame.ui.UiPageType;

import java.time.Instant;
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
    private final ICastleBuildingService buildingService;
    private final ICastleBuildingVisualService buildingVisualService;
    private final IResourceNodeVisualService resourceNodeVisualService;
    private final ICastleSiteVisualService castleSiteVisualService;
    private final ICastleEconomySimulationService castleEconomySimulationService;
    private final IPlayerTeleportService playerTeleportService;
    private final KingdomBuildingCommandSupport buildingCommandSupport;
    private final KingdomNodeCommandSupport nodeCommandSupport;
    private final KingdomPlacementCommandSupport placementCommandSupport;
    private final KingdomInteractionCommandSupport interactionCommandSupport;

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
            ICastleBuildingService buildingService,
            ICastleBuildingVisualService buildingVisualService,
            IResourceNodeService resourceNodeService,
            IResourceNodeVisualService resourceNodeVisualService,
            ICastleSiteVisualService castleSiteVisualService,
            ICastleEconomySimulationService castleEconomySimulationService,
            IPlayerTeleportService playerTeleportService,
            IPlacementModeService placementModeService,
            KingdomBuildingCommandSupport buildingCommandSupport,
            KingdomNodeCommandSupport nodeCommandSupport,
            KingdomPlacementCommandSupport placementCommandSupport,
            KingdomInteractionCommandSupport interactionCommandSupport
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
        this.buildingService = buildingService;
        this.buildingVisualService = buildingVisualService;
        this.resourceNodeVisualService = resourceNodeVisualService;
        this.castleSiteVisualService = castleSiteVisualService;
        this.castleEconomySimulationService = castleEconomySimulationService;
        this.playerTeleportService = playerTeleportService;
        this.buildingCommandSupport = buildingCommandSupport;
        this.nodeCommandSupport = nodeCommandSupport;
        this.placementCommandSupport = placementCommandSupport;
        this.interactionCommandSupport = interactionCommandSupport;
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

                    switch (root) {
                        case "ui" -> handleUi(context, player, tokens, session);
                        case "data" -> handleData(context, player, session);
                        case "castle" -> handleCastle(context, player, tokens, session);
                        case "interior" -> handleInterior(player, tokens);
                        case "citizens" -> handleCitizens(context, tokens, player.getUuid());
                        case "troops" -> handleTroops(context, tokens, player.getUuid());
                        case "resources" -> handleResources(context, tokens, player.getUuid());
                        case "buildings" -> buildingCommandSupport.handle(context, player, tokens, session);
                        case "nodes" -> nodeCommandSupport.handle(context, player, tokens, session);
                        case "place" -> placementCommandSupport.handle(context, player, tokens);
                        case "focus" -> interactionCommandSupport.handleFocus(context, player);
                        case "interact" -> interactionCommandSupport.handleInteract(context, player);
                        case "scan" -> handleScan(context, player);
                        case "scene" -> handleScene(context, player, tokens, session);
                        case "bootstrap" -> handleBootstrap(context, player, session);
                        case "tick" -> handleTick(context, tokens);
                        case "tutorial" -> handleTutorial(context, tokens, session);
                        case "debug" -> sendHelp(context);
                        default -> sendHelp(context);
                    }
                }, executor))
                .exceptionally(throwable -> {
                    Throwable rootCause = rootCause(throwable);
                    LOGGER.at(Level.SEVERE).withCause(rootCause).log(
                            "Failed to execute /%s for %s (%s).",
                            getName(),
                            player.getDisplayName(),
                            player.getUuid()
                    );
                    executor.execute(() -> context.sendMessage(
                            Message.raw("Kingdom data failed to load: " + safeMessage(rootCause)).color("red")
                    ));
                    return null;
                });
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

    private void handleData(CommandContext context, Player player, PlayerSession session) {
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
        context.sendMessage(Message.raw("Buildings: " + buildingService.listBuildings(state).size()).color("yellow"));
        context.sendMessage(Message.raw("Cache: " + healthSnapshot.cacheSummary()).color("yellow"));
        context.sendMessage(Message.raw("Persistence: " + healthSnapshot.persistenceSummary()).color("yellow"));
        context.sendMessage(Message.raw("Interior tutorial: " + tutorialStatus(gameStateService.isInteriorTutorialPending(state))).color("yellow"));
        context.sendMessage(Message.raw("Interior tour: " + tutorialStatus(gameStateService.isInteriorTourPending(state))).color("yellow"));
        context.sendMessage(Message.raw("Upgrade tutorial: " + tutorialStatus(gameStateService.isUpgradeTutorialPending(state))).color("yellow"));
        placementCommandSupport.handle(context, player, List.of("place", "status"));
    }

    private void handleCastle(CommandContext context, Player player, List<String> tokens, PlayerSession session) {
        PlayerGameState state = session.gameState();
        if (tokens.size() == 1) {
            castleSpawnService.ensureCastleSpawned(player, state.castleLocation());
            context.sendMessage(Message.raw("Castle spawn requested.").color("green"));
            return;
        }
        String action = tokens.get(1).toLowerCase(Locale.ROOT);
        switch (action) {
            case "align" -> {
                if (state.castleLocation() == null) {
                    context.sendMessage(Message.raw("Castle data not available.").color("red"));
                    return;
                }
                castlePromptLaneService.alignPlayer(player, state.castleLocation());
                context.sendMessage(Message.raw("Aligned player with castle prompt lane.").color("green"));
            }
            case "move" -> placementCommandSupport.handle(context, player, List.of("place", "castle"));
            case "open" -> {
                uiNavigator.open(UiPageType.CASTLE_MAIN, player, new UiNavigationContext(player.getUuid(), player.getDisplayName()), state);
                context.sendMessage(Message.raw("Opened castle UI.").color("green"));
            }
            case "goto" -> {
                if (state.castleLocation() == null) {
                    context.sendMessage(Message.raw("Castle data not available.").color("red"));
                    return;
                }
                playerTeleportService.teleport(player, playerTeleportService.standingPosition(player, state.castleLocation().standingBaseVector()));
                context.sendMessage(Message.raw("Teleported to castle.").color("green"));
            }
            default -> context.sendMessage(Message.raw("Usage: /kd castle [align|move|open|goto]").color("yellow"));
        }
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
            context.sendMessage(Message.raw("Citizens added: " + amount.getAsInt()).color("green"));
        } else if ("set".equalsIgnoreCase(action)) {
            populationService.setCitizens(playerId, amount.getAsInt());
            context.sendMessage(Message.raw("Citizens set: " + amount.getAsInt()).color("green"));
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
            context.sendMessage(Message.raw("Troops added: " + amount.getAsInt()).color("green"));
        } else if ("set".equalsIgnoreCase(action)) {
            populationService.setTroops(playerId, amount.getAsInt());
            context.sendMessage(Message.raw("Troops set: " + amount.getAsInt()).color("green"));
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
            context.sendMessage(Message.raw(type + " added: " + amount.getAsInt()).color("green"));
        } else if ("set".equalsIgnoreCase(action)) {
            resourceService.setResource(playerId, type, amount.getAsInt());
            context.sendMessage(Message.raw(type + " set: " + amount.getAsInt()).color("green"));
        } else {
            context.sendMessage(Message.raw("Unknown action.").color("red"));
        }
    }

    private void handleScene(CommandContext context, Player player, List<String> tokens, PlayerSession session) {
        if (tokens.size() > 1 && !"refresh".equalsIgnoreCase(tokens.get(1))) {
            context.sendMessage(Message.raw("Usage: /kd scene refresh").color("yellow"));
            return;
        }
        PlayerGameState state = session.gameState();
        castleSpawnService.ensureCastleSpawned(player, state.castleLocation());
        castleSiteVisualService.refreshSite(session.playerId(), state);
        buildingVisualService.refreshBuildings(session.playerId(), state);
        resourceNodeVisualService.refreshNodes(session.playerId(), state);
        context.sendMessage(Message.raw("Scene refreshed.").color("green"));
    }

    private void handleScan(CommandContext context, Player player) {
        interactionCommandSupport.handleScan(context, player);
        placementCommandSupport.handle(context, player, List.of("place", "status"));
    }

    private void handleBootstrap(CommandContext context, Player player, PlayerSession session) {
        PlayerGameState state = session.gameState();
        castleSpawnService.ensureCastleSpawned(player, state.castleLocation());
        castleSiteVisualService.refreshSite(session.playerId(), state);
        buildingVisualService.refreshBuildings(session.playerId(), state);
        resourceNodeVisualService.refreshNodes(session.playerId(), state);
        context.sendMessage(Message.raw("Bootstrap refresh complete.").color("green"));
    }

    private void handleTick(CommandContext context, List<String> tokens) {
        if (tokens.size() > 1 && !"run".equalsIgnoreCase(tokens.get(1))) {
            context.sendMessage(Message.raw("Usage: /kd tick run [count]").color("yellow"));
            return;
        }
        int tickCount = 1;
        if (tokens.size() > 2) {
            OptionalInt parsed = parseAmount(tokens.get(2));
            if (parsed.isEmpty() || parsed.getAsInt() <= 0) {
                context.sendMessage(Message.raw("Tick count must be a positive whole number.").color("red"));
                return;
            }
            tickCount = Math.min(parsed.getAsInt(), 16);
        }
        Instant start = Instant.now();
        for (int index = 0; index < tickCount; index++) {
            castleEconomySimulationService.runTick(start.plusSeconds(index * CastleEconomySimulationService.TICK_INTERVAL_SECONDS));
        }
        context.sendMessage(Message.raw("Ran " + tickCount + " economy tick(s).").color("green"));
    }

    private void handleTutorial(CommandContext context, List<String> tokens, PlayerSession session) {
        if (tokens.size() > 1 && !"reset".equalsIgnoreCase(tokens.get(1))) {
            context.sendMessage(Message.raw("Usage: /kd tutorial reset").color("yellow"));
            return;
        }
        Instant now = Instant.now();
        PlayerGameState updatedState = gameStateService.resetOnboardingProgress(session.gameState(), now);
        session.updateGameState(updatedState);
        gameStateService.cacheState(session.playerId(), updatedState);
        AsyncTask.runAsync(() -> gameStateService.persistState(updatedState, now));
        context.sendMessage(Message.raw("Tutorial onboarding reset.").color("green"));
    }

    private void sendHelp(CommandContext context) {
        context.sendMessage(Message.raw("/kingdom, /kd help:").color("yellow"));
        context.sendMessage(Message.raw("/kd ui [ui_type]").color("yellow"));
        context.sendMessage(Message.raw("/kd data").color("yellow"));
        context.sendMessage(Message.raw("/kd castle [align|move|open|goto]").color("yellow"));
        context.sendMessage(Message.raw("/kd interior [exit]").color("yellow"));
        context.sendMessage(Message.raw("/kd citizens add|set <amount>").color("yellow"));
        context.sendMessage(Message.raw("/kd troops add|set <amount>").color("yellow"));
        context.sendMessage(Message.raw("/kd resources add|set <type> <amount>").color("yellow"));
        context.sendMessage(Message.raw("/kd buildings place|stage|list|status|select|align|goto|upgrade|finish|clear").color("yellow"));
        context.sendMessage(Message.raw("/kd nodes place|list|status|select|align|assign|add|stock|recall|goto|remove|clear").color("yellow"));
        context.sendMessage(Message.raw("/kd place castle|node <type>|confirm [here]|cancel|status|preview").color("yellow"));
        context.sendMessage(Message.raw("/kd focus").color("yellow"));
        context.sendMessage(Message.raw("/kd interact").color("yellow"));
        context.sendMessage(Message.raw("/kd scan").color("yellow"));
        context.sendMessage(Message.raw("/kd bootstrap").color("yellow"));
        context.sendMessage(Message.raw("/kd scene refresh").color("yellow"));
        context.sendMessage(Message.raw("/kd tick run [count]").color("yellow"));
        context.sendMessage(Message.raw("/kd tutorial reset").color("yellow"));
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
            case "buildings", "building" -> UiPageType.CASTLE_BUILDINGS;
            case "buildingdetail", "building_detail" -> UiPageType.BUILDING_DETAIL;
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

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private String safeMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "unexpected error";
        }
        return throwable.getMessage();
    }
}
