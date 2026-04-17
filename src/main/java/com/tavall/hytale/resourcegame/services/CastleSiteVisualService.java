package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.tavall.hytale.resourcegame.config.CastleAssetConfig;
import com.tavall.hytale.resourcegame.config.PopulationDisplayConfig;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleSiteVisualService;
import com.tavall.hytale.resourcegame.domain.CastleEconomySnapshot;
import com.tavall.hytale.resourcegame.domain.CitizenJobType;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.resources.ResourceType;
import com.tavall.hytale.resourcegame.world.CastleEntityRegistry;
import com.tavall.hytale.resourcegame.world.CastleSiteLayout;
import com.tavall.hytale.resourcegame.world.CastleSiteLayoutService;
import com.tavall.hytale.resourcegame.world.CastleSiteStructureService;
import com.tavall.hytale.resourcegame.world.CastleSiteVisualRefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Maintains readable main-world visuals around a player's castle.
 */
public final class CastleSiteVisualService implements ICastleSiteVisualService, IDependencyInjectableConcrete {
    private static final Logger LOGGER = Logger.getLogger(CastleSiteVisualService.class.getName());

    private final CastleEntityRegistry castleEntityRegistry;
    private final CastleAssetConfig castleAssetConfig;
    private final PopulationDisplayConfig displayConfig;
    private final CastleEconomyPlanner planner;
    private final CastleSiteScenePlanner scenePlanner;
    private final CastleSiteLayoutService layoutService;
    private final CastleSiteStructureService structureService;
    private final NpcVisualSpawner npcVisualSpawner;
    private final Map<UUID, CastleSiteVisualRefs> siteRefs = new ConcurrentHashMap<>();

    public CastleSiteVisualService(
            CastleEntityRegistry castleEntityRegistry,
            CastleAssetConfig castleAssetConfig,
            PopulationDisplayConfig displayConfig,
            CastleEconomyPlanner planner,
            CastleSiteScenePlanner scenePlanner,
            CastleSiteLayoutService layoutService,
            CastleSiteStructureService structureService,
            NpcVisualSpawner npcVisualSpawner
    ) {
        this.castleEntityRegistry = castleEntityRegistry;
        this.castleAssetConfig = castleAssetConfig;
        this.displayConfig = displayConfig;
        this.planner = planner;
        this.scenePlanner = scenePlanner;
        this.layoutService = layoutService;
        this.structureService = structureService;
        this.npcVisualSpawner = npcVisualSpawner;
    }

    @Override
    public void ensureSite(UUID playerId, PlayerGameState state) {
        rebuildSite(playerId, state);
    }

    @Override
    public void refreshSite(UUID playerId, PlayerGameState state) {
        rebuildSite(playerId, state);
    }

    @Override
    public void clearSite(UUID playerId) {
        CastleSiteVisualRefs refs = siteRefs.remove(playerId);
        if (refs == null) {
            return;
        }
        for (Ref<EntityStore> ref : refs.allRefs()) {
            if (ref != null && ref.isValid()) {
                ref.getStore().removeEntity(ref, RemoveReason.REMOVE);
            }
        }
    }

    private void rebuildSite(UUID playerId, PlayerGameState state) {
        if (playerId == null || state == null || state.castleLocation() == null) {
            return;
        }
        World world = Universe.get().getWorld(state.castleLocation().worldName());
        if (world == null) {
            return;
        }
        world.execute(() -> {
            clearSite(playerId);
            CastleSiteLayout layout = layoutService.createLayout(state.castleLocation());
            CastleEconomySnapshot snapshot = planner.snapshot(state);
            structureService.ensureSite(world, layout);
            syncCastleLabel(playerId, state);
            Store<EntityStore> store = world.getEntityStore().getStore();
            int roleIndex = new NpcRoleResolver().resolveRoleIndex(displayConfig.npcRoleName());
            if (roleIndex < 0) {
                LOGGER.warning(() -> "Unable to build castle site visuals because NPC role '" + displayConfig.npcRoleName() + "' was not found.");
                return;
            }
            int totalStored = state.resources().food() + state.resources().wood() + state.resources().iron();
            int builderSceneCount = builderSpecialistCount(snapshot);
            int citizenSceneCount = snapshot.jobCount(CitizenJobType.IDLE)
                    + snapshot.jobCount(CitizenJobType.GATHERER)
                    + snapshot.jobCount(CitizenJobType.HUNTER)
                    + snapshot.jobCount(CitizenJobType.COOK)
                    + snapshot.jobCount(CitizenJobType.MINER)
                    + builderSceneCount;
            int troopSceneCount = state.populationSummary().troopCount() + snapshot.jobCount(CitizenJobType.TRAINEE);

            Ref<EntityStore> stockpileAnchorRef = spawnNamed(
                    store,
                    roleIndex,
                    layout.stockpileAnchor(),
                    scenePlanner.stockpileLabel(state),
                    scenePlanner.stockpileAnchorScale(totalStored)
            );
            Ref<EntityStore> citizenAnchorRef = spawnNamed(
                    store,
                    roleIndex,
                    layout.citizenAnchor(),
                    "Citizen Yard: " + state.populationSummary().citizenCount()
                            + " | Gatherers " + snapshot.jobCount(CitizenJobType.GATHERER)
                            + " | Builders " + builderSceneCount,
                    scenePlanner.populationAnchorScale(state.populationSummary().citizenCount())
            );
            Ref<EntityStore> troopAnchorRef = spawnNamed(
                    store,
                    roleIndex,
                    layout.troopAnchor(),
                    "Troop Drill: " + state.populationSummary().troopCount()
                            + " | Trainees " + snapshot.jobCount(CitizenJobType.TRAINEE),
                    scenePlanner.populationAnchorScale(troopSceneCount)
            );
            Ref<EntityStore> foodNodeRef = spawnNamed(
                    store,
                    roleIndex,
                    layout.foodNodeAnchor(),
                    nodeLabel("Food Stores", state.resources().food(), snapshot.workersFor(ResourceType.FOOD), snapshot.gainFor(ResourceType.FOOD)),
                    scenePlanner.nodeAnchorScale(state.resources().food(), snapshot.workersFor(ResourceType.FOOD))
            );
            Ref<EntityStore> woodNodeRef = spawnNamed(
                    store,
                    roleIndex,
                    layout.woodNodeAnchor(),
                    nodeLabel("Wood Camp", state.resources().wood(), snapshot.workersFor(ResourceType.WOOD), snapshot.gainFor(ResourceType.WOOD)),
                    scenePlanner.nodeAnchorScale(state.resources().wood(), snapshot.workersFor(ResourceType.WOOD))
            );
            Ref<EntityStore> ironNodeRef = spawnNamed(
                    store,
                    roleIndex,
                    layout.ironNodeAnchor(),
                    nodeLabel("Iron Vein", state.resources().iron(), snapshot.workersFor(ResourceType.IRON), snapshot.gainFor(ResourceType.IRON)),
                    scenePlanner.nodeAnchorScale(state.resources().iron(), snapshot.workersFor(ResourceType.IRON))
            );

            List<Ref<EntityStore>> stockpileRefs = spawnCrowd(
                    store,
                    roleIndex,
                    layout.stockpilePositions(),
                    scenePlanner.visibleStorageCount(totalStored),
                    scenePlanner.crowdScale(scenePlanner.visibleStorageCount(totalStored))
            );
            List<Ref<EntityStore>> citizenCrowdRefs = spawnCrowd(store, roleIndex, layout.citizenCrowdPositions(), scenePlanner.visibleWorkerCount(citizenSceneCount), scenePlanner.crowdScale(citizenSceneCount));
            List<Ref<EntityStore>> troopCrowdRefs = spawnCrowd(store, roleIndex, layout.troopCrowdPositions(), scenePlanner.visibleWorkerCount(troopSceneCount), scenePlanner.crowdScale(troopSceneCount));
            List<Ref<EntityStore>> foodPileRefs = spawnCrowd(store, roleIndex, layout.foodNodePositions(), scenePlanner.visibleStorageCount(state.resources().food()), scenePlanner.crowdScale(scenePlanner.visibleStorageCount(state.resources().food())));
            List<Ref<EntityStore>> woodPileRefs = spawnCrowd(store, roleIndex, layout.woodNodePositions(), scenePlanner.visibleStorageCount(state.resources().wood()), scenePlanner.crowdScale(scenePlanner.visibleStorageCount(state.resources().wood())));
            List<Ref<EntityStore>> ironPileRefs = spawnCrowd(store, roleIndex, layout.ironNodePositions(), scenePlanner.visibleStorageCount(state.resources().iron()), scenePlanner.crowdScale(scenePlanner.visibleStorageCount(state.resources().iron())));
            List<Ref<EntityStore>> foodConvoyRefs = spawnCrowd(store, roleIndex, layout.foodConvoyPositions(), scenePlanner.visibleConvoyCount(snapshot.workersFor(ResourceType.FOOD), snapshot.gainFor(ResourceType.FOOD)), scenePlanner.convoyScale(snapshot.workersFor(ResourceType.FOOD), snapshot.gainFor(ResourceType.FOOD)));
            List<Ref<EntityStore>> woodConvoyRefs = spawnCrowd(store, roleIndex, layout.woodConvoyPositions(), scenePlanner.visibleConvoyCount(snapshot.workersFor(ResourceType.WOOD), snapshot.gainFor(ResourceType.WOOD)), scenePlanner.convoyScale(snapshot.workersFor(ResourceType.WOOD), snapshot.gainFor(ResourceType.WOOD)));
            List<Ref<EntityStore>> ironConvoyRefs = spawnCrowd(store, roleIndex, layout.ironConvoyPositions(), scenePlanner.visibleConvoyCount(snapshot.workersFor(ResourceType.IRON), snapshot.gainFor(ResourceType.IRON)), scenePlanner.convoyScale(snapshot.workersFor(ResourceType.IRON), snapshot.gainFor(ResourceType.IRON)));

            siteRefs.put(
                    playerId,
                    new CastleSiteVisualRefs(
                            stockpileAnchorRef,
                            citizenAnchorRef,
                            troopAnchorRef,
                            foodNodeRef,
                            woodNodeRef,
                            ironNodeRef,
                            stockpileRefs,
                            citizenCrowdRefs,
                            troopCrowdRefs,
                            foodPileRefs,
                            woodPileRefs,
                            ironPileRefs,
                            foodConvoyRefs,
                            woodConvoyRefs,
                            ironConvoyRefs
                    )
            );
            LOGGER.info(() -> String.format(
                    "Castle site visuals refreshed for %s in world %s. citizens=%s troops=%s food=%s wood=%s iron=%s",
                    playerId,
                    world.getName(),
                    state.populationSummary().citizenCount(),
                    state.populationSummary().troopCount(),
                    state.resources().food(),
                    state.resources().wood(),
                    state.resources().iron()
            ));
        });
    }

    private void syncCastleLabel(UUID playerId, PlayerGameState state) {
        Ref<EntityStore> castleRef = castleEntityRegistry.get(playerId);
        if (castleRef == null || !castleRef.isValid()) {
            return;
        }
        String castleLabel = castleAssetConfig.displayName()
                + " | C:" + state.populationSummary().citizenCount()
                + " T:" + state.populationSummary().troopCount();
        castleRef.getStore().putComponent(
                castleRef,
                DisplayNameComponent.getComponentType(),
                new DisplayNameComponent(Message.raw(castleLabel))
        );
    }

    private Ref<EntityStore> spawnNamed(Store<EntityStore> store, int roleIndex, Vector3d position, String label, float scale) {
        return npcVisualSpawner.spawnNamed(store, roleIndex, position, label, scale);
    }

    private int builderSpecialistCount(CastleEconomySnapshot snapshot) {
        return snapshot.jobCount(CitizenJobType.BLACKSMITH)
                + snapshot.jobCount(CitizenJobType.ARCHITECT)
                + snapshot.jobCount(CitizenJobType.GRUNT_BUILDER)
                + snapshot.jobCount(CitizenJobType.BUILDER);
    }

    private List<Ref<EntityStore>> spawnCrowd(Store<EntityStore> store, int roleIndex, List<Vector3d> positions, int visibleCount, float scale) {
        return npcVisualSpawner.spawnGroup(store, roleIndex, positions, visibleCount, scale);
    }

    private String nodeLabel(String label, int stored, int workers, int gainPerTick) {
        return label + ": " + stored + " | Crew " + workers + " | +" + gainPerTick + "/tick";
    }
}
