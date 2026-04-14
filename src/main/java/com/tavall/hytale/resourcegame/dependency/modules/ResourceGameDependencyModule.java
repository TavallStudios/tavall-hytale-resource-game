package com.tavall.hytale.resourcegame.dependency.modules;

import com.tavall.hytale.resourcegame.ResourceGamePlugin;
import com.tavall.hytale.resourcegame.cache.JacksonCacheCodec;
import com.tavall.hytale.resourcegame.cache.SemanticCacheFactory;
import com.tavall.hytale.resourcegame.clock.KingdomClockService;
import com.tavall.hytale.resourcegame.config.CacheConfig;
import com.tavall.hytale.resourcegame.config.CastleAssetConfig;
import com.tavall.hytale.resourcegame.config.DatabaseConfig;
import com.tavall.hytale.resourcegame.config.KingdomClockConfig;
import com.tavall.hytale.resourcegame.config.PopulationDisplayConfig;
import com.tavall.hytale.resourcegame.dependency.DependencyLoaderAccess;
import com.tavall.hytale.resourcegame.dependency.IDependencyModule;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleInteractionService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastlePromptLaneService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleProximityPromptService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleSiteVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleSpawnService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IDebugCommandService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInfrastructureHealthService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInteriorInstanceService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInteriorWorldService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IIpHashService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IKingdomClockService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerDataService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerGameStateService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerProfileService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerSessionStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerTeleportService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPopulationService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiActionService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiNavigator;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiPageRegistry;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.PlayerProfile;
import com.tavall.hytale.resourcegame.interior.InteriorLayoutService;
import com.tavall.hytale.resourcegame.interior.InteriorStructureService;
import com.tavall.hytale.resourcegame.persistence.InMemoryPlayerGameStateStore;
import com.tavall.hytale.resourcegame.persistence.InMemoryPlayerProfileStore;
import com.tavall.hytale.resourcegame.persistence.PlayerGameStateRepository;
import com.tavall.hytale.resourcegame.persistence.PlayerGameStateStore;
import com.tavall.hytale.resourcegame.persistence.PlayerProfileRepository;
import com.tavall.hytale.resourcegame.persistence.PlayerProfileStore;
import com.tavall.hytale.resourcegame.persistence.PostgresConnectionProvider;
import com.tavall.hytale.resourcegame.population.PromotionCost;
import com.tavall.hytale.resourcegame.services.CastleInteractionService;
import com.tavall.hytale.resourcegame.services.CastlePromptLaneService;
import com.tavall.hytale.resourcegame.services.CastleProximityPromptService;
import com.tavall.hytale.resourcegame.services.CastleSiteVisualService;
import com.tavall.hytale.resourcegame.services.CastleSpawnService;
import com.tavall.hytale.resourcegame.services.DebugCommandService;
import com.tavall.hytale.resourcegame.services.InteriorInstanceService;
import com.tavall.hytale.resourcegame.services.InteriorTourMarkerService;
import com.tavall.hytale.resourcegame.services.InteriorWorldService;
import com.tavall.hytale.resourcegame.services.IpHashService;
import com.tavall.hytale.resourcegame.services.InfrastructureHealthService;
import com.tavall.hytale.resourcegame.services.JsonMapperProvider;
import com.tavall.hytale.resourcegame.services.PlayerDataService;
import com.tavall.hytale.resourcegame.services.PlayerGameStateService;
import com.tavall.hytale.resourcegame.services.PlayerProfileService;
import com.tavall.hytale.resourcegame.services.PlayerSessionStore;
import com.tavall.hytale.resourcegame.services.PlayerTeleportService;
import com.tavall.hytale.resourcegame.services.PopulationDisplayGateway;
import com.tavall.hytale.resourcegame.services.PopulationDisplayService;
import com.tavall.hytale.resourcegame.services.PopulationService;
import com.tavall.hytale.resourcegame.services.ResourceService;
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
import com.tavall.hytale.resourcegame.world.CastleSiteLayoutService;
import com.tavall.hytale.resourcegame.world.CastleSiteStructureService;
import com.tavall.hytale.resourcegame.world.CastlePromptLaneLayoutService;
import com.tavall.hytale.resourcegame.world.CastlePromptLaneStructureService;
import org.tavall.abstractcache.semantic.SemanticCache;

import java.util.logging.Level;

/**
 * Repo-local composition root that mirrors the shared Tavall DI bootstrap style.
 */
public final class ResourceGameDependencyModule implements IDependencyModule {
    private final ResourceGamePlugin plugin;

    public ResourceGameDependencyModule(ResourceGamePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void registerDependencies() {
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
            plugin.getLogger().at(Level.WARNING).log("Postgres config not found. Falling back to in-memory profile and game-state stores.");
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
        InfrastructureHealthService infrastructureHealthService = new InfrastructureHealthService(cacheConfig, databaseConfig);
        CastleSiteVisualService castleSiteVisualService = new CastleSiteVisualService(
                castleEntityRegistry,
                castleAssetConfig,
                populationDisplayConfig,
                new CastleSiteLayoutService(),
                new CastleSiteStructureService()
        );

        CastleSpawnService castleSpawnService = new CastleSpawnService(castleAssetConfig, castleEntityRegistry, sessionStore, castleSiteVisualService);
        PopulationDisplayService populationDisplayService = new PopulationDisplayService(populationDisplayConfig);
        InteriorTourMarkerService interiorTourMarkerService = new InteriorTourMarkerService(populationDisplayConfig);
        PlayerTeleportService playerTeleportService = new PlayerTeleportService();
        InteriorInstanceService interiorInstanceService = new InteriorInstanceService();
        IpHashService ipHashService = new IpHashService();
        CastlePromptLaneService castlePromptLaneService = new CastlePromptLaneService(
                new CastlePromptLaneLayoutService(),
                new CastlePromptLaneStructureService(),
                playerTeleportService
        );
        UiPageRegistry pageRegistry = new UiPageRegistry();
        UiNavigator uiNavigator = new UiNavigator(pageRegistry);
        ResourceService resourceService = new ResourceService(sessionStore, gameStateService, castleSiteVisualService);
        PopulationService populationService = new PopulationService(
                sessionStore,
                gameStateService,
                resourceService,
                castleSiteVisualService,
                populationDisplayService,
                PromotionCost.defaultCost()
        );
        InteriorWorldService interiorWorldService = new InteriorWorldService(
                sessionStore,
                gameStateService,
                interiorInstanceService,
                new InteriorLayoutService(),
                new InteriorStructureService(),
                interiorTourMarkerService,
                playerTeleportService,
                populationDisplayService,
                uiNavigator
        );
        UiActionService uiActionService = new UiActionService(uiNavigator, interiorWorldService, populationService, sessionStore, gameStateService);
        registerUiPages(pageRegistry, uiActionService, infrastructureHealthService, gameStateService);

        KingdomClockService clockService = new KingdomClockService(clockConfig);
        PlayerDataService playerDataService = new PlayerDataService(
                profileService,
                gameStateService,
                sessionStore,
                castleSpawnService,
                ipHashService,
                clockService
        );
        CastleInteractionService castleInteractionService = new CastleInteractionService(
                castleEntityRegistry,
                sessionStore,
                uiNavigator,
                castleAssetConfig
        );
        CastleProximityPromptService castleProximityPromptService = new CastleProximityPromptService(castleInteractionService);
        DebugCommandService debugCommandService = new DebugCommandService(
                sessionStore,
                uiNavigator,
                populationService,
                resourceService,
                interiorWorldService,
                castleSpawnService,
                castlePromptLaneService,
                playerDataService,
                gameStateService,
                infrastructureHealthService
        );

        registerSingleton(IPlayerProfileService.class, profileService);
        registerSingleton(IPlayerGameStateService.class, gameStateService);
        registerSingleton(IPlayerSessionStore.class, sessionStore);
        registerSingleton(ICastleSiteVisualService.class, castleSiteVisualService);
        registerSingleton(ICastleSpawnService.class, castleSpawnService);
        registerSingleton(PopulationDisplayGateway.class, populationDisplayService);
        registerSingleton(IPlayerTeleportService.class, playerTeleportService);
        registerSingleton(ICastlePromptLaneService.class, castlePromptLaneService);
        registerSingleton(IUiPageRegistry.class, pageRegistry);
        registerSingleton(IUiNavigator.class, uiNavigator);
        registerSingleton(IResourceService.class, resourceService);
        registerSingleton(IPopulationService.class, populationService);
        registerSingleton(IInteriorInstanceService.class, interiorInstanceService);
        registerSingleton(IInteriorWorldService.class, interiorWorldService);
        registerSingleton(IUiActionService.class, uiActionService);
        registerSingleton(IIpHashService.class, ipHashService);
        registerSingleton(IKingdomClockService.class, clockService);
        registerSingleton(IPlayerDataService.class, playerDataService);
        registerSingleton(ICastleInteractionService.class, castleInteractionService);
        registerSingleton(ICastleProximityPromptService.class, castleProximityPromptService);
        registerSingleton(IDebugCommandService.class, debugCommandService);
        registerSingleton(IInfrastructureHealthService.class, infrastructureHealthService);
    }

    private void registerUiPages(
            IUiPageRegistry registry,
            IUiActionService actionService,
            IInfrastructureHealthService infrastructureHealthService,
            IPlayerGameStateService gameStateService
    ) {
        registry.register(UiPageType.CASTLE_MAIN, (player, context, state) -> new CastleMainPage(player, context, state, actionService));
        registry.register(UiPageType.CASTLE_INFO, (player, context, state) -> new CastleInfoPage(player, context, state, actionService));
        registry.register(UiPageType.CASTLE_CITIZENS, (player, context, state) -> new CastleCitizensPage(player, context, state, actionService));
        registry.register(UiPageType.CASTLE_TROOPS, (player, context, state) -> new CastleTroopsPage(player, context, state, actionService));
        registry.register(UiPageType.CASTLE_RESOURCES, (player, context, state) -> new CastleResourcesPage(player, context, state, actionService));
        registry.register(UiPageType.CASTLE_UPGRADES, (player, context, state) -> new CastleUpgradesPage(player, context, state, actionService));
        registry.register(UiPageType.INTERIOR_MAIN, (player, context, state) -> new InteriorMainPage(player, context, state, actionService));
        registry.register(
                UiPageType.DEBUG_NAVIGATOR,
                (player, context, state) -> new DebugNavigatorPage(
                        player,
                        context,
                        state,
                        actionService,
                        infrastructureHealthService,
                        gameStateService
                )
        );
    }

    private <T> void registerSingleton(Class<T> token, T instance) {
        DependencyLoaderAccess.registerInstance(token, instance);
    }
}
