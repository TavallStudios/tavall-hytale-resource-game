package org.tavall.hytale.resourcegame.app;

import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.sql.DataSource;
import org.tavall.hytale.resourcegame.cache.PlayerHotStateCache;
import org.tavall.hytale.resourcegame.clock.KingdomClockService;
import org.tavall.hytale.resourcegame.command.CommandContext;
import org.tavall.hytale.resourcegame.command.CommandResult;
import org.tavall.hytale.resourcegame.command.KingdomCommandRouter;
import org.tavall.hytale.resourcegame.domain.player.PlayerJoinRequest;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;
import org.tavall.hytale.resourcegame.persistence.PlayerStateStore;
import org.tavall.hytale.resourcegame.persistence.postgres.InMemoryPostgresPlayerStateStore;
import org.tavall.hytale.resourcegame.persistence.postgres.PostgresPlayerStateStore;
import org.tavall.hytale.resourcegame.persistence.postgres.PostgresSchemaLifecycle;
import org.tavall.hytale.resourcegame.persistence.redis.InMemoryRedisKeyValueStore;
import org.tavall.hytale.resourcegame.persistence.redis.RedisKeyValueStore;
import org.tavall.hytale.resourcegame.persistence.redis.RedisPlayerStateStore;
import org.tavall.hytale.resourcegame.runtime.HytaleRuntimeGateway;
import org.tavall.hytale.resourcegame.runtime.WorldPosition;
import org.tavall.hytale.resourcegame.service.CastleAnchorRegistry;
import org.tavall.hytale.resourcegame.service.CastleInteractionService;
import org.tavall.hytale.resourcegame.service.CastleSelectionService;
import org.tavall.hytale.resourcegame.service.CastleSpawnService;
import org.tavall.hytale.resourcegame.service.CastleUiAssembler;
import org.tavall.hytale.resourcegame.service.CitizenTroopDisplayService;
import org.tavall.hytale.resourcegame.service.CitizenTroopUpgradeUiAssembler;
import org.tavall.hytale.resourcegame.service.DefaultCastleVisualCatalog;
import org.tavall.hytale.resourcegame.service.InteriorLayoutPlanner;
import org.tavall.hytale.resourcegame.service.InteriorOverviewUiAssembler;
import org.tavall.hytale.resourcegame.service.InteriorTransitionService;
import org.tavall.hytale.resourcegame.service.IpTransformService;
import org.tavall.hytale.resourcegame.service.KingdomAgingService;
import org.tavall.hytale.resourcegame.service.KingdomUiNavigatorService;
import org.tavall.hytale.resourcegame.service.PlayerInitializationService;
import org.tavall.hytale.resourcegame.service.PlayerSessionRegistry;
import org.tavall.hytale.resourcegame.service.PlayerStateFactory;
import org.tavall.hytale.resourcegame.service.PlayerStateGateway;
import org.tavall.hytale.resourcegame.service.PopulationContinuumService;
import org.tavall.hytale.resourcegame.service.PopulationMetadataCodec;
import org.tavall.hytale.resourcegame.service.ResourceMutationService;
import org.tavall.hytale.resourcegame.service.ResourceSummaryUiAssembler;

/**
 * Composition root for the first vertical slice runtime.
 */
public class KingdomPrototypeKernel {

  private final HytaleRuntimeGateway runtimeGateway;
  private final IpTransformService ipTransformService;
  private final PlayerInitializationService playerInitializationService;
  private final PlayerSessionRegistry playerSessionRegistry;
  private final CastleSelectionService castleSelectionService;
  private final PlayerStateGateway playerStateGateway;
  private final KingdomCommandRouter commandRouter;
  private final KingdomClockService kingdomClockService;
  private final KingdomAgingService kingdomAgingService;

  public KingdomPrototypeKernel(
      HytaleRuntimeGateway runtimeGateway,
      IpTransformService ipTransformService,
      PlayerInitializationService playerInitializationService,
      PlayerSessionRegistry playerSessionRegistry,
      CastleSelectionService castleSelectionService,
      PlayerStateGateway playerStateGateway,
      KingdomCommandRouter commandRouter,
      KingdomClockService kingdomClockService,
      KingdomAgingService kingdomAgingService
  ) {
    this.runtimeGateway = runtimeGateway;
    this.ipTransformService = ipTransformService;
    this.playerInitializationService = playerInitializationService;
    this.playerSessionRegistry = playerSessionRegistry;
    this.castleSelectionService = castleSelectionService;
    this.playerStateGateway = playerStateGateway;
    this.commandRouter = commandRouter;
    this.kingdomClockService = kingdomClockService;
    this.kingdomAgingService = kingdomAgingService;
  }

  public static KingdomPrototypeKernel create(
      DataSource postgresDataSource,
      RedisKeyValueStore redisKeyValueStore,
      HytaleRuntimeGateway runtimeGateway,
      ZoneId serverZoneId
  ) {
    PostgresSchemaLifecycle schemaLifecycle = new PostgresSchemaLifecycle(postgresDataSource);
    schemaLifecycle.ensureSchema();

    PlayerStateStore redisStore = new RedisPlayerStateStore(redisKeyValueStore);
    PlayerStateStore postgresStore = new PostgresPlayerStateStore(postgresDataSource);
    return createWithStateStores(runtimeGateway, serverZoneId, redisStore, postgresStore);
  }

  public static KingdomPrototypeKernel createInMemory(
      HytaleRuntimeGateway runtimeGateway,
      ZoneId serverZoneId
  ) {
    PlayerStateStore redisStore = new RedisPlayerStateStore(new InMemoryRedisKeyValueStore());
    PlayerStateStore postgresStore = new InMemoryPostgresPlayerStateStore();
    return createWithStateStores(runtimeGateway, serverZoneId, redisStore, postgresStore);
  }

  public static KingdomPrototypeKernel createWithStateStores(
      HytaleRuntimeGateway runtimeGateway,
      ZoneId serverZoneId,
      PlayerStateStore redisStore,
      PlayerStateStore postgresStore
  ) {
    PlayerHotStateCache hotCache = new PlayerHotStateCache(20);
    PlayerStateFactory stateFactory = new PlayerStateFactory();
    PopulationMetadataCodec metadataCodec = new PopulationMetadataCodec();
    PlayerStateGateway stateGateway = new PlayerStateGateway(hotCache, redisStore, postgresStore, stateFactory, metadataCodec);

    CastleAnchorRegistry castleAnchorRegistry = new CastleAnchorRegistry();
    CastleSpawnService castleSpawnService = new CastleSpawnService(new DefaultCastleVisualCatalog(), runtimeGateway, castleAnchorRegistry);
    PlayerSessionRegistry sessionRegistry = new PlayerSessionRegistry();
    PlayerInitializationService initService = new PlayerInitializationService(stateGateway, castleSpawnService, sessionRegistry);
    CastleInteractionService interactionService = new CastleInteractionService(castleAnchorRegistry, runtimeGateway);
    CastleUiAssembler castleUiAssembler = new CastleUiAssembler();
    CastleSelectionService selectionService = new CastleSelectionService(interactionService, castleUiAssembler, runtimeGateway);

    InteriorTransitionService interiorTransitionService = new InteriorTransitionService(runtimeGateway, new InteriorLayoutPlanner());
    CitizenTroopDisplayService displayService = new CitizenTroopDisplayService(runtimeGateway);
    PopulationContinuumService populationContinuumService = new PopulationContinuumService();
    ResourceMutationService resourceMutationService = new ResourceMutationService();
    KingdomUiNavigatorService navigatorService = new KingdomUiNavigatorService();
    CitizenTroopUpgradeUiAssembler upgradeUiAssembler = new CitizenTroopUpgradeUiAssembler(populationContinuumService);
    ResourceSummaryUiAssembler resourceSummaryUiAssembler = new ResourceSummaryUiAssembler();
    InteriorOverviewUiAssembler interiorOverviewUiAssembler = new InteriorOverviewUiAssembler();

    KingdomCommandRouter commandRouter = new KingdomCommandRouter(
        sessionRegistry,
        stateGateway,
        selectionService,
        castleUiAssembler,
        navigatorService,
        upgradeUiAssembler,
        resourceSummaryUiAssembler,
        interiorOverviewUiAssembler,
        interiorTransitionService,
        displayService,
        populationContinuumService,
        resourceMutationService,
        runtimeGateway
    );

    return new KingdomPrototypeKernel(
        runtimeGateway,
        new IpTransformService(),
        initService,
        sessionRegistry,
        selectionService,
        stateGateway,
        commandRouter,
        new KingdomClockService(serverZoneId),
        new KingdomAgingService()
    );
  }

  public CompletableFuture<PlayerStateBundle> initializePlayer(
      UUID playerId,
      String playerName,
      ZoneId timezone,
      String rawIp,
      WorldPosition joinPosition
  ) {
    runtimeGateway.movePlayer(playerId, joinPosition);
    PlayerJoinRequest request = new PlayerJoinRequest(playerId, playerName, timezone, ipTransformService.transform(rawIp), joinPosition);
    return playerInitializationService.initialize(request);
  }

  public CommandResult runCommand(UUID playerId, String rawCommand) {
    return commandRouter.route(new CommandContext(playerId), rawCommand);
  }

  public boolean pollCastleInteraction(UUID playerId) {
    return playerSessionRegistry.find(playerId)
        .map(bundle -> castleSelectionService.openIfSelectable(playerId, bundle))
        .orElse(false);
  }

  public Optional<PlayerStateBundle> session(UUID playerId) {
    return playerSessionRegistry.find(playerId);
  }

  public PlayerStateGateway playerStateGateway() {
    return playerStateGateway;
  }

  public KingdomClockService kingdomClockService() {
    return kingdomClockService;
  }

  public KingdomAgingService kingdomAgingService() {
    return kingdomAgingService;
  }
}
