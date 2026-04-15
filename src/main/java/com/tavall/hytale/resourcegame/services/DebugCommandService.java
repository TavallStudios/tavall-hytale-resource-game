package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastlePromptLaneService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleSpawnService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IDebugCommandService;
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
import com.tavall.hytale.resourcegame.commands.KingdomCommand;
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
    private final IPlayerDataService playerDataService;
    private final IPlayerGameStateService gameStateService;
    private final IInfrastructureHealthService infrastructureHealthService;
    private final IResourceNodeService resourceNodeService;
    private final IResourceNodeVisualService resourceNodeVisualService;

    public DebugCommandService(
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
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.uiNavigator = Objects.requireNonNull(uiNavigator, "uiNavigator");
        this.populationService = Objects.requireNonNull(populationService, "populationService");
        this.resourceService = Objects.requireNonNull(resourceService, "resourceService");
        this.interiorWorldService = Objects.requireNonNull(interiorWorldService, "interiorWorldService");
        this.castleSpawnService = Objects.requireNonNull(castleSpawnService, "castleSpawnService");
        this.castlePromptLaneService = Objects.requireNonNull(castlePromptLaneService, "castlePromptLaneService");
        this.playerDataService = Objects.requireNonNull(playerDataService, "playerDataService");
        this.gameStateService = Objects.requireNonNull(gameStateService, "gameStateService");
        this.infrastructureHealthService = Objects.requireNonNull(infrastructureHealthService, "infrastructureHealthService");
        this.resourceNodeService = Objects.requireNonNull(resourceNodeService, "resourceNodeService");
        this.resourceNodeVisualService = Objects.requireNonNull(resourceNodeVisualService, "resourceNodeVisualService");
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
                playerDataService,
                gameStateService,
                infrastructureHealthService,
                resourceNodeService,
                resourceNodeVisualService
        );
        return List.of(kingdom);
    }
}
