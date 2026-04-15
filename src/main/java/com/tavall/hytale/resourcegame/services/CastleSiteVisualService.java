package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
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
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.resources.ResourceType;
import com.tavall.hytale.resourcegame.world.CastleEntityRegistry;
import com.tavall.hytale.resourcegame.world.CastleSiteLayout;
import com.tavall.hytale.resourcegame.world.CastleSiteLayoutService;
import com.tavall.hytale.resourcegame.world.CastleSiteStructureService;
import com.tavall.hytale.resourcegame.world.CastleSiteVisualRefs;
import it.unimi.dsi.fastutil.Pair;

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
    private static final int MAX_POPULATION_MARKERS = 6;
    private static final int MAX_RESOURCE_MARKERS = 4;

    private final CastleEntityRegistry castleEntityRegistry;
    private final CastleAssetConfig castleAssetConfig;
    private final PopulationDisplayConfig displayConfig;
    private final CastleEconomyPlanner planner;
    private final CastleSiteLayoutService layoutService;
    private final CastleSiteStructureService structureService;
    private final Map<UUID, CastleSiteVisualRefs> siteRefs = new ConcurrentHashMap<>();

    public CastleSiteVisualService(
            CastleEntityRegistry castleEntityRegistry,
            CastleAssetConfig castleAssetConfig,
            PopulationDisplayConfig displayConfig,
            CastleEconomyPlanner planner,
            CastleSiteLayoutService layoutService,
            CastleSiteStructureService structureService
    ) {
        this.castleEntityRegistry = castleEntityRegistry;
        this.castleAssetConfig = castleAssetConfig;
        this.displayConfig = displayConfig;
        this.planner = planner;
        this.layoutService = layoutService;
        this.structureService = structureService;
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
            int roleIndex = NPCPlugin.get().getIndex(displayConfig.npcRoleName());
            if (roleIndex < 0) {
                LOGGER.warning(() -> "Unable to build castle site visuals because NPC role '" + displayConfig.npcRoleName() + "' was not found.");
                return;
            }

            Ref<EntityStore> citizenAnchorRef = spawnNamed(
                    store,
                    roleIndex,
                    layout.citizenAnchor(),
                    "Citizen Yard: " + state.populationSummary().citizenCount()
                            + " | Idle " + snapshot.jobCount(com.tavall.hytale.resourcegame.domain.CitizenJobType.IDLE)
                            + " | Builders " + snapshot.jobCount(com.tavall.hytale.resourcegame.domain.CitizenJobType.BUILDER)
            );
            Ref<EntityStore> troopAnchorRef = spawnNamed(
                    store,
                    roleIndex,
                    layout.troopAnchor(),
                    "Troop Drill: " + state.populationSummary().troopCount()
                            + " | Trainees " + snapshot.jobCount(com.tavall.hytale.resourcegame.domain.CitizenJobType.TRAINEE)
            );
            Ref<EntityStore> foodNodeRef = spawnNamed(store, roleIndex, layout.foodNodeAnchor(), nodeLabel("Food Stores", state.resources().food(), snapshot.workersFor(ResourceType.FOOD), snapshot.gainFor(ResourceType.FOOD)));
            Ref<EntityStore> woodNodeRef = spawnNamed(store, roleIndex, layout.woodNodeAnchor(), nodeLabel("Wood Camp", state.resources().wood(), snapshot.workersFor(ResourceType.WOOD), snapshot.gainFor(ResourceType.WOOD)));
            Ref<EntityStore> ironNodeRef = spawnNamed(store, roleIndex, layout.ironNodeAnchor(), nodeLabel("Iron Vein", state.resources().iron(), snapshot.workersFor(ResourceType.IRON), snapshot.gainFor(ResourceType.IRON)));

            List<Ref<EntityStore>> citizenCrowdRefs = spawnCrowd(store, roleIndex, layout.citizenCrowdPositions(), visiblePopulationCount(snapshot.jobCount(com.tavall.hytale.resourcegame.domain.CitizenJobType.IDLE) + snapshot.jobCount(com.tavall.hytale.resourcegame.domain.CitizenJobType.BUILDER)));
            List<Ref<EntityStore>> troopCrowdRefs = spawnCrowd(store, roleIndex, layout.troopCrowdPositions(), visiblePopulationCount(state.populationSummary().troopCount() + snapshot.jobCount(com.tavall.hytale.resourcegame.domain.CitizenJobType.TRAINEE)));
            List<Ref<EntityStore>> foodPileRefs = spawnCrowd(store, roleIndex, layout.foodNodePositions(), visiblePopulationCount(snapshot.workersFor(ResourceType.FOOD)));
            List<Ref<EntityStore>> woodPileRefs = spawnCrowd(store, roleIndex, layout.woodNodePositions(), visiblePopulationCount(snapshot.workersFor(ResourceType.WOOD)));
            List<Ref<EntityStore>> ironPileRefs = spawnCrowd(store, roleIndex, layout.ironNodePositions(), visiblePopulationCount(snapshot.workersFor(ResourceType.IRON)));

            siteRefs.put(
                    playerId,
                    new CastleSiteVisualRefs(
                            citizenAnchorRef,
                            troopAnchorRef,
                            foodNodeRef,
                            woodNodeRef,
                            ironNodeRef,
                            citizenCrowdRefs,
                            troopCrowdRefs,
                            foodPileRefs,
                            woodPileRefs,
                            ironPileRefs
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

    private Ref<EntityStore> spawnNamed(Store<EntityStore> store, int roleIndex, Vector3d position, String label) {
        Pair<Ref<EntityStore>, ?> pair = NPCPlugin.get().spawnEntity(store, roleIndex, position, Vector3f.ZERO, null, null);
        Ref<EntityStore> ref = pair.first();
        if (ref != null && ref.isValid()) {
            store.putComponent(ref, DisplayNameComponent.getComponentType(), new DisplayNameComponent(Message.raw(label)));
        }
        return ref;
    }

    private List<Ref<EntityStore>> spawnCrowd(Store<EntityStore> store, int roleIndex, List<Vector3d> positions, int visibleCount) {
        List<Ref<EntityStore>> refs = new ArrayList<>();
        for (int index = 0; index < visibleCount && index < positions.size(); index++) {
            Pair<Ref<EntityStore>, ?> pair = NPCPlugin.get().spawnEntity(store, roleIndex, positions.get(index), Vector3f.ZERO, null, null);
            Ref<EntityStore> ref = pair.first();
            if (ref != null && ref.isValid()) {
                refs.add(ref);
            }
        }
        return List.copyOf(refs);
    }

    private int visiblePopulationCount(int count) {
        if (count <= 0) {
            return 0;
        }
        return Math.min(MAX_POPULATION_MARKERS, Math.max(1, (int) Math.ceil(count / 3.0)));
    }

    private int visibleResourceCount(int amount) {
        if (amount <= 0) {
            return 0;
        }
        return Math.min(MAX_RESOURCE_MARKERS, Math.max(1, (int) Math.ceil(amount / 30.0)));
    }

    private String nodeLabel(String label, int stored, int workers, int gainPerTick) {
        return label + ": " + stored + " | Workers " + workers + " | +" + gainPerTick + "/tick";
    }
}
