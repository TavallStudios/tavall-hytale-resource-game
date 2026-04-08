package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.tavall.hytale.resourcegame.commands.KingdomCommand;
import com.tavall.hytale.resourcegame.ui.UiNavigator;

import java.util.List;
import java.util.Objects;

/**
 * Builds debug command instances.
 */
public final class DebugCommandService {
    private final PlayerSessionStore sessionStore;
    private final UiNavigator uiNavigator;
    private final PopulationService populationService;
    private final ResourceService resourceService;
    private final InteriorWorldService interiorWorldService;
    private final CastleSpawnService castleSpawnService;
    private final CastlePromptLaneService castlePromptLaneService;
    private final PlayerDataService playerDataService;

    public DebugCommandService(
            PlayerSessionStore sessionStore,
            UiNavigator uiNavigator,
            PopulationService populationService,
            ResourceService resourceService,
            InteriorWorldService interiorWorldService,
            CastleSpawnService castleSpawnService,
            CastlePromptLaneService castlePromptLaneService,
            PlayerDataService playerDataService
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
