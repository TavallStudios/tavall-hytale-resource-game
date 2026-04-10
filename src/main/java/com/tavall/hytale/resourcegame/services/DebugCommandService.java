package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastlePromptLaneService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleSpawnService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IDebugCommandService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInteriorWorldService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerDataService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerSessionStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPopulationService;
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

    public DebugCommandService(
            IPlayerSessionStore sessionStore,
            IUiNavigator uiNavigator,
            IPopulationService populationService,
            IResourceService resourceService,
            IInteriorWorldService interiorWorldService,
            ICastleSpawnService castleSpawnService,
            ICastlePromptLaneService castlePromptLaneService,
            IPlayerDataService playerDataService
    ) {
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.uiNavigator = Objects.requireNonNull(uiNavigator, "uiNavigator");
        this.populationService = Objects.requireNonNull(populationService, "populationService");
        this.resourceService = Objects.requireNonNull(resourceService, "resourceService");
        this.interiorWorldService = Objects.requireNonNull(interiorWorldService, "interiorWorldService");
        this.castleSpawnService = Objects.requireNonNull(castleSpawnService, "castleSpawnService");
        this.castlePromptLaneService = Objects.requireNonNull(castlePromptLaneService, "castlePromptLaneService");
        this.playerDataService = Objects.requireNonNull(playerDataService, "playerDataService");
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
                playerDataService
        );
        return List.of(kingdom);
    }
}
