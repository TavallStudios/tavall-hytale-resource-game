package com.tavall.hytale.resourcegame;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.tavall.hytale.resourcegame.cache.JacksonCacheCodec;
import com.tavall.hytale.resourcegame.cache.SemanticCacheFactory;
import com.tavall.hytale.resourcegame.clock.KingdomClockService;
import com.tavall.hytale.resourcegame.config.CacheConfig;
import com.tavall.hytale.resourcegame.config.CastleAssetConfig;
import com.tavall.hytale.resourcegame.config.DatabaseConfig;
import com.tavall.hytale.resourcegame.config.KingdomClockConfig;
import com.tavall.hytale.resourcegame.config.PopulationDisplayConfig;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.PlayerProfile;
import com.tavall.hytale.resourcegame.interior.InteriorLayoutService;
import com.tavall.hytale.resourcegame.interior.InteriorStructureService;
import com.tavall.hytale.resourcegame.persistence.InMemoryPlayerGameStateStore;
import com.tavall.hytale.resourcegame.persistence.InMemoryPlayerProfileStore;
import com.tavall.hytale.resourcegame.persistence.PlayerGameStateStore;
import com.tavall.hytale.resourcegame.persistence.PlayerGameStateRepository;
import com.tavall.hytale.resourcegame.persistence.PlayerProfileStore;
import com.tavall.hytale.resourcegame.persistence.PlayerProfileRepository;
import com.tavall.hytale.resourcegame.persistence.PostgresConnectionProvider;
import com.tavall.hytale.resourcegame.population.PromotionCost;
import com.tavall.hytale.resourcegame.services.CastleInteractionService;
import com.tavall.hytale.resourcegame.services.CastlePromptLaneService;
import com.tavall.hytale.resourcegame.services.CastleProximityPromptService;
import com.tavall.hytale.resourcegame.services.CastleSpawnService;
import com.tavall.hytale.resourcegame.services.DebugCommandService;
import com.tavall.hytale.resourcegame.services.InteriorWorldService;
import com.tavall.hytale.resourcegame.services.IpHashService;
import com.tavall.hytale.resourcegame.services.InteriorInstanceService;
import com.tavall.hytale.resourcegame.services.JsonMapperProvider;
import com.tavall.hytale.resourcegame.services.PlayerDataService;
import com.tavall.hytale.resourcegame.services.PlayerGameStateService;
import com.tavall.hytale.resourcegame.services.PlayerProfileService;
import com.tavall.hytale.resourcegame.services.PlayerSessionStore;
import com.tavall.hytale.resourcegame.services.PlayerTeleportService;
import com.tavall.hytale.resourcegame.services.PopulationDisplayService;
import com.tavall.hytale.resourcegame.services.PopulationService;
import com.tavall.hytale.resourcegame.services.ResourceService;
import com.tavall.hytale.resourcegame.tasks.AsyncTask;
import com.tavall.hytale.resourcegame.ui.CastleCitizensPage;
import com.tavall.hytale.resourcegame.ui.CastleInfoPage;
import com.tavall.hytale.resourcegame.ui.CastleMainPage;
import com.tavall.hytale.resourcegame.ui.CastleResourcesPage;
import com.tavall.hytale.resourcegame.ui.CastleTroopsPage;
import com.tavall.hytale.resourcegame.ui.CastleUpgradesPage;
import com.tavall.hytale.resourcegame.ui.DebugNavigatorPage;
import com.tavall.hytale.resourcegame.ui.InteriorMainPage;
import com.tavall.hytale.resourcegame.ui.UiActionService;
import com.tavall.hytale.resourcegame.ui.UiNavigator;
import com.tavall.hytale.resourcegame.ui.UiPageRegistry;
import com.tavall.hytale.resourcegame.ui.UiPageType;
import com.tavall.hytale.resourcegame.world.CastleEntityRegistry;
import com.tavall.hytale.resourcegame.world.CastlePromptLaneLayoutService;
import com.tavall.hytale.resourcegame.world.CastlePromptLaneStructureService;
import org.tavall.abstractcache.semantic.SemanticCache;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.logging.Level;

public class ResourceGamePlugin extends JavaPlugin {
    private PlayerDataService playerDataService;
    private CastleInteractionService castleInteractionService;
    private CastleProximityPromptService castleProximityPromptService;
    private DebugCommandService debugCommandService;

    public ResourceGamePlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        CacheConfig cacheConfig = CacheConfig.fromEnv();
        DatabaseConfig databaseConfig = DatabaseConfig.fromEnv();
        KingdomClockConfig clockConfig = KingdomClockConfig.fromEnv();

        JsonMapperProvider mapperProvider = new JsonMapperProvider();
        SemanticCacheFactory cacheFactory = new SemanticCacheFactory(cacheConfig);
        SemanticCache profileCache = cacheFactory.build("resource-game-profile");
        SemanticCache gameStateCache = cacheFactory.build("resource-game-game-state");

        PlayerProfileStore profileStore;
        PlayerGameStateStore gameStateStore;
        if (databaseConfig.jdbcUrl() == null || databaseConfig.jdbcUrl().isBlank()) {
            getLogger().at(Level.WARNING).log("Postgres config not found. Falling back to in-memory profile and game-state stores.");
            profileStore = new InMemoryPlayerProfileStore();
            gameStateStore = new InMemoryPlayerGameStateStore();
        } else {
            PostgresConnectionProvider connectionProvider = new PostgresConnectionProvider(databaseConfig);
            profileStore = new PlayerProfileRepository(connectionProvider);
            gameStateStore = new PlayerGameStateRepository(connectionProvider);
        }

        PlayerSessionStore sessionStore = new PlayerSessionStore();
        PlayerProfileService profileService = new PlayerProfileService(
                profileStore,
                profileCache,
                new JacksonCacheCodec<>(mapperProvider.mapper(), PlayerProfile.class, "player-profile")
        );
        PlayerGameStateService gameStateService = new PlayerGameStateService(
                gameStateStore,
                gameStateCache,
                new JacksonCacheCodec<>(mapperProvider.mapper(), PlayerGameState.class, "player-game-state"),
                mapperProvider.mapper()
        );

        CastleEntityRegistry castleEntityRegistry = new CastleEntityRegistry();
        CastleAssetConfig castleAssetConfig = CastleAssetConfig.defaults();
        PopulationDisplayConfig populationDisplayConfig = PopulationDisplayConfig.defaults();

        CastleSpawnService castleSpawnService = new CastleSpawnService(castleAssetConfig, castleEntityRegistry);
        PopulationDisplayService populationDisplayService = new PopulationDisplayService(populationDisplayConfig);
        ResourceService resourceService = new ResourceService(sessionStore, gameStateService);
        PlayerTeleportService playerTeleportService = new PlayerTeleportService();
        CastlePromptLaneService castlePromptLaneService = new CastlePromptLaneService(
                new CastlePromptLaneLayoutService(),
                new CastlePromptLaneStructureService(),
                playerTeleportService
        );
        UiPageRegistry pageRegistry = new UiPageRegistry();
        UiNavigator uiNavigator = new UiNavigator(pageRegistry);
        PopulationService populationService = new PopulationService(
                sessionStore,
                gameStateService,
                resourceService,
                populationDisplayService,
                PromotionCost.defaultCost()
        );

        InteriorWorldService interiorWorldService = new InteriorWorldService(
                sessionStore,
                gameStateService,
                new InteriorInstanceService(),
                new InteriorLayoutService(),
                new InteriorStructureService(),
                playerTeleportService,
                populationDisplayService,
                uiNavigator
        );
        UiActionService uiActionService = new UiActionService(uiNavigator, interiorWorldService, populationService, sessionStore);
        registerUiPages(pageRegistry, uiActionService);

        KingdomClockService clockService = new KingdomClockService(clockConfig);
        this.playerDataService = new PlayerDataService(
                profileService,
                gameStateService,
                sessionStore,
                castleSpawnService,
                new IpHashService(),
                clockService
        );
        this.castleInteractionService = new CastleInteractionService(
                castleEntityRegistry,
                sessionStore,
                uiNavigator,
                castleAssetConfig
        );
        this.castleProximityPromptService = new CastleProximityPromptService(castleInteractionService);
        this.debugCommandService = new DebugCommandService(
                sessionStore,
                uiNavigator,
                populationService,
                resourceService,
                interiorWorldService,
                castleSpawnService,
                castlePromptLaneService,
                playerDataService
        );

        getLogger().atInfo().log("Kingdom clock initialized. Daytime: %s", clockService.snapshot().isDay());
    }

    @Override
    protected void start() {
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, playerDataService::handlePlayerReady);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, playerDataService::handlePlayerDisconnect);
        getEventRegistry().registerGlobal(PlayerInteractEvent.class, castleInteractionService::handleInteract);
        castleProximityPromptService.start();

        List<AbstractAsyncCommand> commands = debugCommandService.commands();
        for (AbstractAsyncCommand command : commands) {
            getCommandRegistry().registerCommand(command);
            getLogger().atInfo().log("Registered command /%s aliases=%s", command.getName(), command.getAliases());
        }
    }

    @Override
    protected void shutdown() {
        if (castleProximityPromptService != null) {
            castleProximityPromptService.shutdown();
        }
        AsyncTask.shutdown();
    }

    private void registerUiPages(UiPageRegistry registry, UiActionService actionService) {
        registry.register(UiPageType.CASTLE_MAIN, (player, context, state) -> new CastleMainPage(player, context, state, actionService));
        registry.register(UiPageType.CASTLE_INFO, (player, context, state) -> new CastleInfoPage(player, context, state, actionService));
        registry.register(UiPageType.CASTLE_CITIZENS, (player, context, state) -> new CastleCitizensPage(player, context, state, actionService));
        registry.register(UiPageType.CASTLE_TROOPS, (player, context, state) -> new CastleTroopsPage(player, context, state, actionService));
        registry.register(UiPageType.CASTLE_RESOURCES, (player, context, state) -> new CastleResourcesPage(player, context, state, actionService));
        registry.register(UiPageType.CASTLE_UPGRADES, (player, context, state) -> new CastleUpgradesPage(player, context, state, actionService));
        registry.register(UiPageType.INTERIOR_MAIN, (player, context, state) -> new InteriorMainPage(player, context, state, actionService));
        registry.register(UiPageType.DEBUG_NAVIGATOR, (player, context, state) -> new DebugNavigatorPage(player, context, state, actionService));
    }
}
