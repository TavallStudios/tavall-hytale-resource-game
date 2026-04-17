package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastlePromptLaneService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleBuildingService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleBuildingVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleSiteVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleSpawnService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleEconomySimulationService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IDebugCommandService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IFocusedWorldOverrideService;
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
import com.tavall.hytale.resourcegame.commands.KingdomInteractionCommandSupport;
import com.tavall.hytale.resourcegame.commands.KingdomBuildingCommandSupport;
import com.tavall.hytale.resourcegame.commands.KingdomCommand;
import com.tavall.hytale.resourcegame.commands.KingdomNodeCommandSupport;
import com.tavall.hytale.resourcegame.commands.KingdomPlacementCommandSupport;
import java.util.List;
import java.util.Objects;

/**
 * Builds debug command instances.
 */
public final class DebugCommandService implements IDebugCommandService, IDependencyInjectableConcrete {
    private final IPlayerSessionStore sessionStore;
    private final IUiNavigator uiNavigator;
    private final IPopulationService populationService;
    private final IResourceService resourceService;
    private final IInteriorWorldService interiorWorldService;
    private final ICastleSpawnService castleSpawnService;
    private final ICastlePromptLaneService castlePromptLaneService;
    private final IFocusedWorldOverrideService focusedWorldOverrideService;
    private final IPlayerDataService playerDataService;
    private final IPlayerGameStateService gameStateService;
    private final IInfrastructureHealthService infrastructureHealthService;
    private final ICastleBuildingService buildingService;
    private final ICastleBuildingVisualService buildingVisualService;
    private final IResourceNodeService resourceNodeService;
    private final IResourceNodeVisualService resourceNodeVisualService;
    private final ICastleSiteVisualService castleSiteVisualService;
    private final ICastleEconomySimulationService castleEconomySimulationService;
    private final IPlayerTeleportService playerTeleportService;
    private final IPlacementModeService placementModeService;
    private final KingdomBuildingCommandSupport buildingCommandSupport;
    private final KingdomNodeCommandSupport nodeCommandSupport;
    private final KingdomPlacementCommandSupport placementCommandSupport;
    private final KingdomInteractionCommandSupport interactionCommandSupport;

    public DebugCommandService(
            IPlayerSessionStore sessionStore,
            IUiNavigator uiNavigator,
            IPopulationService populationService,
            IResourceService resourceService,
            IInteriorWorldService interiorWorldService,
            ICastleSpawnService castleSpawnService,
            ICastlePromptLaneService castlePromptLaneService,
            IFocusedWorldOverrideService focusedWorldOverrideService,
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
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.uiNavigator = Objects.requireNonNull(uiNavigator, "uiNavigator");
        this.populationService = Objects.requireNonNull(populationService, "populationService");
        this.resourceService = Objects.requireNonNull(resourceService, "resourceService");
        this.interiorWorldService = Objects.requireNonNull(interiorWorldService, "interiorWorldService");
        this.castleSpawnService = Objects.requireNonNull(castleSpawnService, "castleSpawnService");
        this.castlePromptLaneService = Objects.requireNonNull(castlePromptLaneService, "castlePromptLaneService");
        this.focusedWorldOverrideService = Objects.requireNonNull(focusedWorldOverrideService, "focusedWorldOverrideService");
        this.playerDataService = Objects.requireNonNull(playerDataService, "playerDataService");
        this.gameStateService = Objects.requireNonNull(gameStateService, "gameStateService");
        this.infrastructureHealthService = Objects.requireNonNull(infrastructureHealthService, "infrastructureHealthService");
        this.buildingService = Objects.requireNonNull(buildingService, "buildingService");
        this.buildingVisualService = Objects.requireNonNull(buildingVisualService, "buildingVisualService");
        this.resourceNodeService = Objects.requireNonNull(resourceNodeService, "resourceNodeService");
        this.resourceNodeVisualService = Objects.requireNonNull(resourceNodeVisualService, "resourceNodeVisualService");
        this.castleSiteVisualService = Objects.requireNonNull(castleSiteVisualService, "castleSiteVisualService");
        this.castleEconomySimulationService = Objects.requireNonNull(castleEconomySimulationService, "castleEconomySimulationService");
        this.playerTeleportService = Objects.requireNonNull(playerTeleportService, "playerTeleportService");
        this.placementModeService = Objects.requireNonNull(placementModeService, "placementModeService");
        this.buildingCommandSupport = Objects.requireNonNull(buildingCommandSupport, "buildingCommandSupport");
        this.nodeCommandSupport = Objects.requireNonNull(nodeCommandSupport, "nodeCommandSupport");
        this.placementCommandSupport = Objects.requireNonNull(placementCommandSupport, "placementCommandSupport");
        this.interactionCommandSupport = Objects.requireNonNull(interactionCommandSupport, "interactionCommandSupport");
    }

    public List<AbstractAsyncCommand> commands() {
        AbstractAsyncCommand kingdom = new KingdomCommand(
                "kingdom",
                sessionStore,
                uiNavigator,
                populationService,
                resourceService,
                interiorWorldService,
                castleSpawnService,
                castlePromptLaneService,
                focusedWorldOverrideService,
                playerDataService,
                gameStateService,
                infrastructureHealthService,
                buildingService,
                buildingVisualService,
                resourceNodeService,
                resourceNodeVisualService,
                castleSiteVisualService,
                castleEconomySimulationService,
                playerTeleportService,
                placementModeService,
                buildingCommandSupport,
                nodeCommandSupport,
                placementCommandSupport,
                interactionCommandSupport
        );
        return List.of(kingdom);
 }
}
