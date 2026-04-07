package org.tavall.hytale.resourcegame.command;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import org.tavall.hytale.resourcegame.domain.interior.InteriorLayoutPlan;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;
import org.tavall.hytale.resourcegame.domain.resource.ResourceType;
import org.tavall.hytale.resourcegame.domain.ui.UiScreen;
import org.tavall.hytale.resourcegame.runtime.HytaleRuntimeGateway;
import org.tavall.hytale.resourcegame.service.CastleSelectionService;
import org.tavall.hytale.resourcegame.service.CastleUiAssembler;
import org.tavall.hytale.resourcegame.service.CitizenTroopDisplayService;
import org.tavall.hytale.resourcegame.service.CitizenTroopUpgradeUiAssembler;
import org.tavall.hytale.resourcegame.service.InteriorOverviewUiAssembler;
import org.tavall.hytale.resourcegame.service.InteriorTransitionService;
import org.tavall.hytale.resourcegame.service.KingdomUiNavigatorService;
import org.tavall.hytale.resourcegame.service.PlayerSessionRegistry;
import org.tavall.hytale.resourcegame.service.PlayerStateGateway;
import org.tavall.hytale.resourcegame.service.PopulationContinuumService;
import org.tavall.hytale.resourcegame.service.ResourceMutationService;
import org.tavall.hytale.resourcegame.service.ResourceSummaryUiAssembler;

/**
 * Debug/dev command router for the prototype kingdom command surface.
 */
public class KingdomCommandRouter {

  private final PlayerSessionRegistry playerSessionRegistry;
  private final PlayerStateGateway playerStateGateway;
  private final CastleSelectionService castleSelectionService;
  private final CastleUiAssembler castleUiAssembler;
  private final KingdomUiNavigatorService kingdomUiNavigatorService;
  private final CitizenTroopUpgradeUiAssembler citizenTroopUpgradeUiAssembler;
  private final ResourceSummaryUiAssembler resourceSummaryUiAssembler;
  private final InteriorOverviewUiAssembler interiorOverviewUiAssembler;
  private final InteriorTransitionService interiorTransitionService;
  private final CitizenTroopDisplayService citizenTroopDisplayService;
  private final PopulationContinuumService populationContinuumService;
  private final ResourceMutationService resourceMutationService;
  private final HytaleRuntimeGateway runtimeGateway;

  public KingdomCommandRouter(
      PlayerSessionRegistry playerSessionRegistry,
      PlayerStateGateway playerStateGateway,
      CastleSelectionService castleSelectionService,
      CastleUiAssembler castleUiAssembler,
      KingdomUiNavigatorService kingdomUiNavigatorService,
      CitizenTroopUpgradeUiAssembler citizenTroopUpgradeUiAssembler,
      ResourceSummaryUiAssembler resourceSummaryUiAssembler,
      InteriorOverviewUiAssembler interiorOverviewUiAssembler,
      InteriorTransitionService interiorTransitionService,
      CitizenTroopDisplayService citizenTroopDisplayService,
      PopulationContinuumService populationContinuumService,
      ResourceMutationService resourceMutationService,
      HytaleRuntimeGateway runtimeGateway
  ) {
    this.playerSessionRegistry = playerSessionRegistry;
    this.playerStateGateway = playerStateGateway;
    this.castleSelectionService = castleSelectionService;
    this.castleUiAssembler = castleUiAssembler;
    this.kingdomUiNavigatorService = kingdomUiNavigatorService;
    this.citizenTroopUpgradeUiAssembler = citizenTroopUpgradeUiAssembler;
    this.resourceSummaryUiAssembler = resourceSummaryUiAssembler;
    this.interiorOverviewUiAssembler = interiorOverviewUiAssembler;
    this.interiorTransitionService = interiorTransitionService;
    this.citizenTroopDisplayService = citizenTroopDisplayService;
    this.populationContinuumService = populationContinuumService;
    this.resourceMutationService = resourceMutationService;
    this.runtimeGateway = runtimeGateway;
  }

  /**
   * Routes `/kingdom` and `/kd` debug commands.
   */
  public CommandResult route(CommandContext context, String rawCommand) {
    String[] parts = tokenize(rawCommand);
    if (parts.length == 0) {
      return new CommandResult(false, "Empty command");
    }
    String root = parts[0].toLowerCase(Locale.ROOT);
    if ("/kingdom".equals(root)) {
      openUi(context.playerId(), kingdomUiNavigatorService.buildNavigator());
      return new CommandResult(true, "Opened kingdom debug navigator");
    }
    if (!"/kd".equals(root)) {
      return new CommandResult(false, "Unknown command root");
    }
    if (parts.length == 1) {
      openUi(context.playerId(), kingdomUiNavigatorService.buildNavigator());
      return new CommandResult(true, "Opened kingdom debug navigator");
    }
    return switch (parts[1].toLowerCase(Locale.ROOT)) {
      case "ui" -> handleUi(context, parts);
      case "data" -> handleData(context);
      case "castle" -> handleCastle(context);
      case "interior" -> handleInterior(context);
      case "citizens" -> handleCitizens(context, parts);
      case "troops" -> handleTroops(context, parts);
      case "resources" -> handleResources(context, parts);
      case "debug" -> handleDebug(parts);
      default -> new CommandResult(false, "Unknown /kd command. Use /kd debug help");
    };
  }

  private CommandResult handleUi(CommandContext context, String[] parts) {
    PlayerStateBundle bundle = findBundle(context).orElse(null);
    if (bundle == null) {
      return new CommandResult(false, "Player session not initialized");
    }
    if (parts.length == 2) {
      openUi(context.playerId(), kingdomUiNavigatorService.buildNavigator());
      return new CommandResult(true, "Opened debug UI navigator");
    }
    String type = parts[2].toLowerCase(Locale.ROOT);
    return switch (type) {
      case "castle", "castle-main" -> {
        openUi(context.playerId(), castleUiAssembler.build(bundle));
        yield new CommandResult(true, "Opened castle UI");
      }
      case "upgrade", "citizens", "troops" -> {
        openUi(context.playerId(), citizenTroopUpgradeUiAssembler.build(bundle));
        yield new CommandResult(true, "Opened citizen-to-troop UI");
      }
      case "resources" -> {
        openUi(context.playerId(), resourceSummaryUiAssembler.build(bundle));
        yield new CommandResult(true, "Opened resource summary UI");
      }
      case "interior-overview" -> {
        Optional<InteriorLayoutPlan> layout = interiorTransitionService.findLayout(context.playerId());
        if (layout.isEmpty()) {
          yield new CommandResult(false, "No interior session active");
        }
        openUi(context.playerId(), interiorOverviewUiAssembler.build(layout.get()));
        yield new CommandResult(true, "Opened interior overview UI");
      }
      default -> new CommandResult(false, "Unknown UI type");
    };
  }

  private CommandResult handleData(CommandContext context) {
    PlayerStateBundle bundle = findBundle(context).orElse(null);
    if (bundle == null) {
      return new CommandResult(false, "Player session not initialized");
    }
    String summary = "profileId=" + bundle.profile().internalPlayerId()
        + ", castleId=" + bundle.gameState().castleId()
        + ", citizens=" + bundle.gameState().citizenCount()
        + ", troops=" + bundle.gameState().troopCount()
        + ", food=" + bundle.gameState().resources().get(ResourceType.FOOD)
        + ", wood=" + bundle.gameState().resources().get(ResourceType.WOOD)
        + ", iron=" + bundle.gameState().resources().get(ResourceType.IRON)
        + ", world=" + runtimeGateway.currentWorld(context.playerId());
    return new CommandResult(true, summary);
  }

  private CommandResult handleCastle(CommandContext context) {
    PlayerStateBundle bundle = findBundle(context).orElse(null);
    if (bundle == null) {
      return new CommandResult(false, "Player session not initialized");
    }
    boolean opened = castleSelectionService.openIfSelectable(context.playerId(), bundle);
    if (!opened) {
      openUi(context.playerId(), castleUiAssembler.build(bundle));
      return new CommandResult(true, "Opened castle UI (forced debug mode)");
    }
    return new CommandResult(true, "Opened castle UI via near/look interaction");
  }

  private CommandResult handleInterior(CommandContext context) {
    PlayerStateBundle bundle = findBundle(context).orElse(null);
    if (bundle == null) {
      return new CommandResult(false, "Player session not initialized");
    }
    if (interiorTransitionService.findSession(context.playerId()).isPresent()) {
      interiorTransitionService.leaveInterior(bundle);
      playerStateGateway.persist(bundle);
      return new CommandResult(true, "Returned to castle exterior");
    }
    interiorTransitionService.enterInterior(bundle);
    interiorTransitionService.findLayout(context.playerId())
        .ifPresent(layout -> citizenTroopDisplayService.ensureAnchors(bundle, layout));
    playerStateGateway.persist(bundle);
    return new CommandResult(true, "Entered interior");
  }

  private CommandResult handleCitizens(CommandContext context, String[] parts) {
    if (parts.length < 4) {
      return new CommandResult(false, "Usage: /kd citizens <add|set> <amount>");
    }
    PlayerStateBundle bundle = findBundle(context).orElse(null);
    if (bundle == null) {
      return new CommandResult(false, "Player session not initialized");
    }
    int amount = parseAmount(parts[3]);
    if (amount == Integer.MIN_VALUE) {
      return new CommandResult(false, "Invalid amount");
    }
    if ("add".equalsIgnoreCase(parts[2])) {
      populationContinuumService.addCitizens(bundle, amount);
      persistAndRefreshDisplays(bundle);
      return new CommandResult(true, "Citizens increased by " + amount);
    }
    if ("set".equalsIgnoreCase(parts[2])) {
      populationContinuumService.setCitizens(bundle, amount);
      persistAndRefreshDisplays(bundle);
      return new CommandResult(true, "Citizens set to " + amount);
    }
    return new CommandResult(false, "Usage: /kd citizens <add|set> <amount>");
  }

  private CommandResult handleTroops(CommandContext context, String[] parts) {
    if (parts.length < 4) {
      return new CommandResult(false, "Usage: /kd troops <add|set> <amount>");
    }
    PlayerStateBundle bundle = findBundle(context).orElse(null);
    if (bundle == null) {
      return new CommandResult(false, "Player session not initialized");
    }
    int amount = parseAmount(parts[3]);
    if (amount == Integer.MIN_VALUE) {
      return new CommandResult(false, "Invalid amount");
    }
    if ("add".equalsIgnoreCase(parts[2])) {
      populationContinuumService.addTroops(bundle, amount);
      persistAndRefreshDisplays(bundle);
      return new CommandResult(true, "Troops increased by " + amount);
    }
    if ("set".equalsIgnoreCase(parts[2])) {
      populationContinuumService.setTroops(bundle, amount);
      persistAndRefreshDisplays(bundle);
      return new CommandResult(true, "Troops set to " + amount);
    }
    return new CommandResult(false, "Usage: /kd troops <add|set> <amount>");
  }

  private CommandResult handleResources(CommandContext context, String[] parts) {
    if (parts.length < 5) {
      return new CommandResult(false, "Usage: /kd resources <add|set> <type> <amount>");
    }
    PlayerStateBundle bundle = findBundle(context).orElse(null);
    if (bundle == null) {
      return new CommandResult(false, "Player session not initialized");
    }
    ResourceType type;
    try {
      type = ResourceType.valueOf(parts[3].toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      return new CommandResult(false, "Unknown resource type");
    }
    int amount = parseAmount(parts[4]);
    if (amount == Integer.MIN_VALUE) {
      return new CommandResult(false, "Invalid amount");
    }
    if ("add".equalsIgnoreCase(parts[2])) {
      resourceMutationService.add(bundle, type, amount);
      playerStateGateway.persist(bundle);
      return new CommandResult(true, "Added " + amount + " " + type.name().toLowerCase());
    }
    if ("set".equalsIgnoreCase(parts[2])) {
      resourceMutationService.set(bundle, type, amount);
      playerStateGateway.persist(bundle);
      return new CommandResult(true, "Set " + type.name().toLowerCase() + " to " + amount);
    }
    return new CommandResult(false, "Usage: /kd resources <add|set> <type> <amount>");
  }

  private CommandResult handleDebug(String[] parts) {
    if (parts.length >= 3 && "help".equalsIgnoreCase(parts[2])) {
      return new CommandResult(true,
          "/kingdom | /kd | /kd ui [ui_type] | /kd data | /kd castle | /kd interior | "
              + "/kd citizens add|set <amount> | /kd troops add|set <amount> | "
              + "/kd resources add|set <type> <amount> | /kd debug help");
    }
    return new CommandResult(false, "Use /kd debug help");
  }

  private void persistAndRefreshDisplays(PlayerStateBundle bundle) {
    citizenTroopDisplayService.updateLabels(bundle.playerId(), bundle);
    playerStateGateway.persist(bundle);
  }

  private Optional<PlayerStateBundle> findBundle(CommandContext context) {
    return playerSessionRegistry.find(context.playerId());
  }

  private int parseAmount(String rawValue) {
    try {
      return Integer.parseInt(rawValue);
    } catch (NumberFormatException exception) {
      return Integer.MIN_VALUE;
    }
  }

  private String[] tokenize(String rawCommand) {
    return Arrays.stream(rawCommand.trim().split("\\s+"))
        .filter(token -> !token.isBlank())
        .toArray(String[]::new);
  }

  private void openUi(java.util.UUID playerId, UiScreen uiScreen) {
    runtimeGateway.openUi(playerId, uiScreen);
  }
}
