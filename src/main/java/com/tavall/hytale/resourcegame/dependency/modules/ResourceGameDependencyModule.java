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
import com.tavall.hytale.resourcegame.dependency.interfaces.IBuildingInteractionService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleBuildingService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleBuildingVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleInteractionService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleEconomySimulationService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastlePromptLaneService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleProximityPromptService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastlePlacementService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleSiteVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleSpawnService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IDebugCommandService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IFocusedWorldInteractionService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IFocusedWorldOverrideService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInfrastructureHealthService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInteriorInstanceService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInteriorWorldService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IIpHashService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IKingdomClockService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlacementInteractionService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlacementModeService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlacementPreviewService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerDataService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerGameStateService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerProfileService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerSessionStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerTeleportService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPopulationService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IProtectedBlockSystemService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeInteractionService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodePromptLaneService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeVisualPulseService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiActionService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiNavigator;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiPageRegistry;
import com.tavall.hytale.resourcegame.dependency.interfaces.IWorkerNpcInteractionService;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.PlayerProfile;
import com.tavall.hytale.resourcegame.interior.InteriorLayoutService;
import com.tavall.hytale.resourcegame.interior.InteriorStructureService;
import com.tavall.hytale.resourcegame.persistence.InMemoryPlayerGameStateStore;
import com.tavall.hytale.resourcegame.persistence.InMemoryPlayerProfileStore;
import com.tavall.hytale.resourcegame.persistence.PersistenceStoreBootstrap;
import com.tavall.hytale.resourcegame.persistence.PlayerGameStateRepository;
import com.tavall.hytale.resourcegame.persistence.PlayerGameStateStore;
import com.tavall.hytale.resourcegame.persistence.PlayerProfileRepository;
import com.tavall.hytale.resourcegame.persistence.PlayerProfileStore;
import com.tavall.hytale.resourcegame.persistence.PostgresConnectionProvider;
import com.tavall.hytale.resourcegame.persistence.ResolvedPersistenceStores;
import com.tavall.hytale.resourcegame.population.PromotionCost;
import com.tavall.hytale.resourcegame.services.CastleInteractionService;
import com.tavall.hytale.resourcegame.services.BuildingInteractionService;
import com.tavall.hytale.resourcegame.services.BuildingPlacementPlanner;
import com.tavall.hytale.resourcegame.services.CastleBuildingService;
import com.tavall.hytale.resourcegame.services.CastleBuildingVisualService;
import com.tavall.hytale.resourcegame.services.CastleEconomyPlanner;
import com.tavall.hytale.resourcegame.services.CastleEconomySimulationService;
import com.tavall.hytale.resourcegame.services.CastlePromptLaneService;
import com.tavall.hytale.resourcegame.services.CastleProximityPromptService;
import com.tavall.hytale.resourcegame.services.CastlePlacementService;
import com.tavall.hytale.resourcegame.services.CastleSiteScenePlanner;
import com.tavall.hytale.resourcegame.services.CastleSiteVisualService;
import com.tavall.hytale.resourcegame.services.CastleSpawnService;
import com.tavall.hytale.resourcegame.services.DebugCommandService;
import com.tavall.hytale.resourcegame.services.FocusedWorldInteractionService;
import com.tavall.hytale.resourcegame.services.FocusedWorldOverrideService;
import com.tavall.hytale.resourcegame.services.FocusedWorldTargetPlanner;
import com.tavall.hytale.resourcegame.services.InteriorInstanceService;
import com.tavall.hytale.resourcegame.services.InteriorTourMarkerService;
import com.tavall.hytale.resourcegame.services.InteriorWorldService;
import com.tavall.hytale.resourcegame.services.IpHashService;
import com.tavall.hytale.resourcegame.services.InfrastructureHealthService;
import com.tavall.hytale.resourcegame.services.JsonMapperProvider;
import com.tavall.hytale.resourcegame.services.PlacementInteractionService;
import com.tavall.hytale.resourcegame.services.PlacementModeService;
import com.tavall.hytale.resourcegame.services.PlacementPreviewService;
import com.tavall.hytale.resourcegame.services.PlayerDataService;
import com.tavall.hytale.resourcegame.services.PlayerGameStateService;
import com.tavall.hytale.resourcegame.services.PlayerProfileService;
import com.tavall.hytale.resourcegame.services.PlayerSessionStore;
import com.tavall.hytale.resourcegame.services.PlayerTeleportService;
import com.tavall.hytale.resourcegame.services.PopulationDisplayGateway;
import com.tavall.hytale.resourcegame.services.PopulationDisplayService;
import com.tavall.hytale.resourcegame.services.ProtectedBlockSystemService;
import com.tavall.hytale.resourcegame.services.PopulationService;
import com.tavall.hytale.resourcegame.services.ResourceNodeInteractionService;
import com.tavall.hytale.resourcegame.services.ResourceNodePromptLaneService;
import com.tavall.hytale.resourcegame.services.ResourceNodeRoutePlanner;
import com.tavall.hytale.resourcegame.services.ResourceNodeService;
import com.tavall.hytale.resourcegame.services.ResourceNodeVisualPulseService;
import com.tavall.hytale.resourcegame.services.ResourceNodeVisualService;
import com.tavall.hytale.resourcegame.services.ResourceService;
import com.tavall.hytale.resourcegame.services.StructureProtectionService;
import com.tavall.hytale.resourcegame.services.VisualVerificationControlService;
import com.tavall.hytale.resourcegame.services.WorkerNpcInteractionService;
import com.tavall.hytale.resourcegame.services.WorldLabelService;
import com.tavall.hytale.resourcegame.ui.CastleCitizensPage;
import com.tavall.hytale.resourcegame.ui.CastleBuildingsPage;
import com.tavall.hytale.resourcegame.ui.CastleInfoPage;
import com.tavall.hytale.resourcegame.ui.CastleMainPage;
import com.tavall.hytale.resourcegame.ui.CastleResourcesPage;
import com.tavall.hytale.resourcegame.ui.CastleTroopsPage;
import com.tavall.hytale.resourcegame.ui.CastleUpgradesPage;
import com.tavall.hytale.resourcegame.ui.BuildingDetailPage;
import com.tavall.hytale.resourcegame.ui.DebugNavigatorPage;
import com.tavall.hytale.resourcegame.ui.InteriorMainPage;
import com.tavall.hytale.resourcegame.ui.ResourceNodePage;
import com.tavall.hytale.resourcegame.ui.UiActionService;
import com.tavall.hytale.resourcegame.ui.UiNavigator;
import com.tavall.hytale.resourcegame.ui.UiPageRegistry;
import com.tavall.hytale.resourcegame.ui.UiPageType;
import com.tavall.hytale.resourcegame.world.CastleEntityRegistry;
import com.tavall.hytale.resourcegame.world.CastleBuildingStructureService;
import com.tavall.hytale.resourcegame.world.BuildingPlacementStageStructureService;
import com.tavall.hytale.resourcegame.world.CastleSiteLayoutService;
import com.tavall.hytale.resourcegame.world.CastleSiteStructureService;
import com.tavall.hytale.resourcegame.world.CastlePromptLaneLayoutService;
import com.tavall.hytale.resourcegame.world.CastlePromptLaneStructureService;
import com.tavall.hytale.resourcegame.world.ResourceNodePromptLaneLayoutService;
import com.tavall.hytale.resourcegame.world.ResourceNodePromptLaneStructureService;
import com.tavall.hytale.resourcegame.world.ResourceNodeStructureService;
import com.tavall.hytale.resourcegame.commands.KingdomInteractionCommandSupport;
import com.tavall.hytale.resourcegame.commands.KingdomBuildingCommandSupport;
import com.tavall.hytale.resourcegame.commands.KingdomNodeCommandSupport;
import com.tavall.hytale.resourcegame.commands.KingdomPlacementCommandSupport;
import com.tavall.hytale.resourcegame.commands.KingdomHologramCommandSupport;
import org.tavall.abstractcache.semantic.SemanticCache;

import java.util.logging.Logger;

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

        PersistenceStoreBootstrap persistenceBootstrap = new PersistenceStoreBootstrap(Logger.getLogger(ResourceGameDependencyModule.class.getName()));
        ResolvedPersistenceStores persistenceStores = persistenceBootstrap.resolve(databaseConfig);
        PlayerProfileStore profileStore = persistenceStores.profileStore();
        PlayerGameStateStore gameStateStore = persistenceStores.gameStateStore();

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

        CastleAssetConfig castleAssetConfig = CastleAssetConfig.defaults();
        PopulationDisplayConfig populationDisplayConfig = PopulationDisplayConfig.defaults();
        InteriorLayoutService interiorLayoutService = new InteriorLayoutService();
        InteriorStructureService interiorStructureService = new InteriorStructureService();
        InfrastructureHealthService infrastructureHealthService = new InfrastructureHealthService(cacheConfig, databaseConfig);
        CastleEconomyPlanner economyPlanner = new CastleEconomyPlanner();
        WorldLabelService worldLabelService = new WorldLabelService();
        StructureProtectionService structureProtectionService = new StructureProtectionService();
        ProtectedBlockSystemService protectedBlockSystemService = new ProtectedBlockSystemService(structureProtectionService);
        InteriorInstanceService interiorInstanceService = new InteriorInstanceService();
        CastleBuildingService buildingService = new CastleBuildingService(
                sessionStore,
                gameStateService,
                interiorInstanceService,
                interiorLayoutService,
                mapperProvider.mapper()
        );
        CastleBuildingVisualService buildingVisualService = new CastleBuildingVisualService(
                buildingService,
                new CastleBuildingStructureService(),
                worldLabelService,
                structureProtectionService
        );
        BuildingPlacementStageStructureService buildingPlacementStageStructureService = new BuildingPlacementStageStructureService();
        BuildingPlacementPlanner buildingPlacementPlanner = new BuildingPlacementPlanner(
                buildingService,
                interiorInstanceService,
                gameStateService,
                interiorLayoutService
        );
        ResourceNodeService resourceNodeService = new ResourceNodeService(sessionStore, gameStateService, mapperProvider.mapper(), economyPlanner);
        ResourceNodeRoutePlanner resourceNodeRoutePlanner = new ResourceNodeRoutePlanner();
        ResourceNodeVisualService resourceNodeVisualService = new ResourceNodeVisualService(
                resourceNodeService,
                new ResourceNodeStructureService(),
                worldLabelService,
                structureProtectionService
        );
        ResourceNodeVisualPulseService resourceNodeVisualPulseService = new ResourceNodeVisualPulseService(sessionStore, resourceNodeVisualService);
        CastleSiteVisualService castleSiteVisualService = new CastleSiteVisualService(
                castleAssetConfig,
                new CastleSiteLayoutService(),
                new CastleSiteStructureService(castleAssetConfig),
                worldLabelService,
                structureProtectionService,
                sessionStore
        );

        CastleSpawnService castleSpawnService = new CastleSpawnService(castleAssetConfig, sessionStore, castleSiteVisualService);
        PopulationDisplayService populationDisplayService = new PopulationDisplayService(populationDisplayConfig, worldLabelService);
        InteriorTourMarkerService interiorTourMarkerService = new InteriorTourMarkerService(worldLabelService);
        PlayerTeleportService playerTeleportService = new PlayerTeleportService();
        IpHashService ipHashService = new IpHashService();
        PlacementPreviewService placementPreviewService = new PlacementPreviewService(worldLabelService);
        CastlePromptLaneService castlePromptLaneService = new CastlePromptLaneService(
                new CastlePromptLaneLayoutService(),
                new CastlePromptLaneStructureService(),
                playerTeleportService
        );
        ResourceNodePromptLaneService resourceNodePromptLaneService = new ResourceNodePromptLaneService(
                new ResourceNodePromptLaneLayoutService(),
                new ResourceNodePromptLaneStructureService(),
                playerTeleportService
        );
        UiPageRegistry pageRegistry = new UiPageRegistry();
        UiNavigator uiNavigator = new UiNavigator(pageRegistry);
        WorkerNpcInteractionService workerNpcInteractionService = new WorkerNpcInteractionService(populationDisplayService, sessionStore, uiNavigator);
        ResourceService resourceService = new ResourceService(sessionStore, gameStateService, castleSiteVisualService, uiNavigator);
        PopulationService populationService = new PopulationService(
                sessionStore,
                gameStateService,
                resourceService,
                castleSiteVisualService,
                populationDisplayService,
                PromotionCost.defaultCost(),
                buildingService,
                resourceNodeService,
                resourceNodeVisualService,
                uiNavigator
        );
        CastleEconomySimulationService castleEconomySimulationService = new CastleEconomySimulationService(
                sessionStore,
                gameStateService,
                buildingService,
                buildingVisualService,
                castleSiteVisualService,
                economyPlanner,
                resourceNodeService,
                resourceNodeVisualService,
                uiNavigator
        );
        CastlePlacementService castlePlacementService = new CastlePlacementService(
                sessionStore,
                gameStateService,
                castleSpawnService,
                castleSiteVisualService,
                buildingVisualService,
                resourceNodeVisualService
        );
        PlacementModeService placementModeService = new PlacementModeService(
                sessionStore,
                placementPreviewService,
                buildingService,
                buildingVisualService,
                castlePlacementService,
                resourceNodeService,
                resourceNodeVisualService
        );
        PlacementInteractionService placementInteractionService = new PlacementInteractionService(placementModeService);
        InteriorWorldService interiorWorldService = new InteriorWorldService(
                sessionStore,
                gameStateService,
                interiorInstanceService,
                interiorLayoutService,
                interiorStructureService,
                interiorTourMarkerService,
                playerTeleportService,
                populationDisplayService,
                buildingVisualService,
                uiNavigator
        );
        UiActionService uiActionService = new UiActionService(
                uiNavigator,
                interiorWorldService,
                populationService,
                buildingService,
                buildingVisualService,
                sessionStore,
                gameStateService,
                resourceNodeService,
                resourceNodeVisualService
        );
        registerUiPages(pageRegistry, uiActionService, infrastructureHealthService, gameStateService, economyPlanner, resourceNodeService, buildingService);

        KingdomClockService clockService = new KingdomClockService(clockConfig);
        PlayerDataService playerDataService = new PlayerDataService(
                profileService,
                gameStateService,
                sessionStore,
                castleSpawnService,
                interiorInstanceService,
                ipHashService,
                clockService,
                resourceNodeVisualService,
                buildingVisualService,
                populationDisplayService,
                interiorTourMarkerService,
                uiNavigator
        );
        VisualVerificationControlService visualVerificationControlService = new VisualVerificationControlService(
                playerDataService,
                sessionStore,
                uiNavigator
        );
        CastleInteractionService castleInteractionService = new CastleInteractionService(
                sessionStore,
                uiNavigator,
                castleAssetConfig
        );
        FocusedWorldOverrideService focusedWorldOverrideService = new FocusedWorldOverrideService();
        FocusedWorldTargetPlanner focusedWorldTargetPlanner = new FocusedWorldTargetPlanner();
        FocusedWorldInteractionService focusedWorldInteractionService = new FocusedWorldInteractionService(
                sessionStore,
                buildingService,
                castleInteractionService,
                focusedWorldOverrideService,
                resourceNodeService,
                uiNavigator,
                focusedWorldTargetPlanner
        );
        CastleProximityPromptService castleProximityPromptService = new CastleProximityPromptService(castleInteractionService, placementModeService);
        ResourceNodeInteractionService resourceNodeInteractionService = new ResourceNodeInteractionService(
                sessionStore,
                resourceNodeVisualService,
                focusedWorldInteractionService,
                uiNavigator
        );
        BuildingInteractionService buildingInteractionService = new BuildingInteractionService(sessionStore, buildingVisualService, focusedWorldInteractionService, uiNavigator);
        KingdomPlacementCommandSupport placementCommandSupport = new KingdomPlacementCommandSupport(placementModeService);
        KingdomBuildingCommandSupport buildingCommandSupport = new KingdomBuildingCommandSupport(
                buildingService,
                buildingVisualService,
                uiNavigator,
                playerTeleportService,
                placementModeService,
                focusedWorldInteractionService,
                buildingPlacementPlanner,
                buildingPlacementStageStructureService
        );
        KingdomNodeCommandSupport nodeCommandSupport = new KingdomNodeCommandSupport(
                resourceNodeService,
                resourceNodeVisualService,
                uiNavigator,
                playerTeleportService,
                placementModeService,
                resourceNodePromptLaneService,
                focusedWorldInteractionService,
                focusedWorldOverrideService
        );
        KingdomInteractionCommandSupport interactionCommandSupport = new KingdomInteractionCommandSupport(focusedWorldInteractionService);
        KingdomHologramCommandSupport hologramCommandSupport = new KingdomHologramCommandSupport(worldLabelService);
        DebugCommandService debugCommandService = new DebugCommandService(
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
                interactionCommandSupport,
                hologramCommandSupport
        );

        registerSingleton(IPlayerProfileService.class, profileService);
        registerSingleton(IPlayerGameStateService.class, gameStateService);
        registerSingleton(IPlayerSessionStore.class, sessionStore);
        registerSingleton(ICastleBuildingService.class, buildingService);
        registerSingleton(ICastleBuildingVisualService.class, buildingVisualService);
        registerSingleton(BuildingPlacementStageStructureService.class, buildingPlacementStageStructureService);
        registerSingleton(BuildingPlacementPlanner.class, buildingPlacementPlanner);
        registerSingleton(ICastleEconomySimulationService.class, castleEconomySimulationService);
        registerSingleton(ICastleSiteVisualService.class, castleSiteVisualService);
        registerSingleton(IResourceNodeService.class, resourceNodeService);
        registerSingleton(IResourceNodeVisualService.class, resourceNodeVisualService);
        registerSingleton(IResourceNodeVisualPulseService.class, resourceNodeVisualPulseService);
        registerSingleton(ICastleSpawnService.class, castleSpawnService);
        registerSingleton(PopulationDisplayGateway.class, populationDisplayService);
        registerSingleton(IPlayerTeleportService.class, playerTeleportService);
        registerSingleton(ICastlePlacementService.class, castlePlacementService);
        registerSingleton(ICastlePromptLaneService.class, castlePromptLaneService);
        registerSingleton(IResourceNodePromptLaneService.class, resourceNodePromptLaneService);
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
        registerSingleton(VisualVerificationControlService.class, visualVerificationControlService);
        registerSingleton(ICastleInteractionService.class, castleInteractionService);
        registerSingleton(IFocusedWorldOverrideService.class, focusedWorldOverrideService);
        registerSingleton(IFocusedWorldInteractionService.class, focusedWorldInteractionService);
        registerSingleton(ICastleProximityPromptService.class, castleProximityPromptService);
        registerSingleton(IPlacementPreviewService.class, placementPreviewService);
        registerSingleton(IPlacementModeService.class, placementModeService);
        registerSingleton(IPlacementInteractionService.class, placementInteractionService);
        registerSingleton(IResourceNodeInteractionService.class, resourceNodeInteractionService);
        registerSingleton(IBuildingInteractionService.class, buildingInteractionService);
        registerSingleton(IWorkerNpcInteractionService.class, workerNpcInteractionService);
        registerSingleton(KingdomBuildingCommandSupport.class, buildingCommandSupport);
        registerSingleton(KingdomNodeCommandSupport.class, nodeCommandSupport);
        registerSingleton(KingdomPlacementCommandSupport.class, placementCommandSupport);
        registerSingleton(KingdomInteractionCommandSupport.class, interactionCommandSupport);
        registerSingleton(KingdomHologramCommandSupport.class, hologramCommandSupport);
        registerSingleton(IDebugCommandService.class, debugCommandService);
        registerSingleton(IInfrastructureHealthService.class, infrastructureHealthService);
        registerSingleton(WorldLabelService.class, worldLabelService);
        registerSingleton(StructureProtectionService.class, structureProtectionService);
        registerSingleton(IProtectedBlockSystemService.class, protectedBlockSystemService);
    }

    private void registerUiPages(
            IUiPageRegistry registry,
            IUiActionService actionService,
            IInfrastructureHealthService infrastructureHealthService,
            IPlayerGameStateService gameStateService,
            CastleEconomyPlanner economyPlanner,
            IResourceNodeService resourceNodeService,
            ICastleBuildingService buildingService
    ) {
        registry.register(UiPageType.CASTLE_MAIN, (player, context, state) -> new CastleMainPage(player, context, state, actionService, economyPlanner));
        registry.register(UiPageType.CASTLE_INFO, (player, context, state) -> new CastleInfoPage(player, context, state, actionService));
        registry.register(UiPageType.CASTLE_CITIZENS, (player, context, state) -> new CastleCitizensPage(player, context, state, actionService, economyPlanner));
        registry.register(UiPageType.CASTLE_TROOPS, (player, context, state) -> new CastleTroopsPage(player, context, state, actionService));
        registry.register(UiPageType.CASTLE_RESOURCES, (player, context, state) -> new CastleResourcesPage(player, context, state, actionService, economyPlanner));
        registry.register(UiPageType.CASTLE_UPGRADES, (player, context, state) -> new CastleUpgradesPage(player, context, state, actionService));
        registry.register(UiPageType.CASTLE_BUILDINGS, (player, context, state) -> new CastleBuildingsPage(player, context, state, actionService, buildingService));
        registry.register(UiPageType.RESOURCE_NODE_DETAIL, (player, context, state) -> new ResourceNodePage(player, context, state, actionService, resourceNodeService));
        registry.register(UiPageType.BUILDING_DETAIL, (player, context, state) -> new BuildingDetailPage(player, context, state, actionService, buildingService));
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
