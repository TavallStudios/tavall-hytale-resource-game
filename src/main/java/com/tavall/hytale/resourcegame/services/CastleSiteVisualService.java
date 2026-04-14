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
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
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
    private final CastleSiteLayoutService layoutService;
    private final CastleSiteStructureService structureService;
    private final Map<UUID, CastleSiteVisualRefs> siteRefs = new ConcurrentHashMap<>();

    public CastleSiteVisualService(
            CastleEntityRegistry castleEntityRegistry,
            CastleAssetConfig castleAssetConfig,
            PopulationDisplayConfig displayConfig,
            CastleSiteLayoutService layoutService,
            CastleSiteStructureService structureService
    ) {
        this.castleEntityRegistry = castleEntityRegistry;
        this.castleAssetConfig = castleAssetConfig;
        this.displayConfig = displayConfig;
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
            structureService.ensureSite(world, layout);
            syncCastleLabel(playerId, state);
            Store<EntityStore> store = world.getEntityStore().getStore();
            int roleIndex = NPCPlugin.get().getIndex(displayConfig.npcRoleName());
            if (roleIndex < 0) {
                LOGGER.warning(() -> "Unable to build castle site visuals because NPC role '" + displayConfig.npcRoleName() + "' was not found.");
                return;
            }

            Ref<EntityStore> citizenAnchorRef = spawnNamed(store, roleIndex, layout.citizenAnchor(), "Citizen Yard: " + state.populationSummary().citizenCount());
            Ref<EntityStore> troopAnchorRef = spawnNamed(store, roleIndex, layout.troopAnchor(), "Troop Drill: " + state.populationSummary().troopCount());
            Ref<EntityStore> foodNodeRef = spawnNamed(store, roleIndex, layout.foodNodeAnchor(), "Food Stores: " + state.resources().food());
            Ref<EntityStore> woodNodeRef = spawnNamed(store, roleIndex, layout.woodNodeAnchor(), "Wood Camp: " + state.resources().wood());
            Ref<EntityStore> ironNodeRef = spawnNamed(store, roleIndex, layout.ironNodeAnchor(), "Iron Vein: " + state.resources().iron());

            List<Ref<EntityStore>> citizenCrowdRefs = spawnCrowd(store, roleIndex, layout.citizenCrowdPositions(), visiblePopulationCount(state.populationSummary().citizenCount()));
            List<Ref<EntityStore>> troopCrowdRefs = spawnCrowd(store, roleIndex, layout.troopCrowdPositions(), visiblePopulationCount(state.populationSummary().troopCount()));
            List<Ref<EntityStore>> foodPileRefs = spawnCrowd(store, roleIndex, layout.foodNodePositions(), visibleResourceCount(state.resources().food()));
            List<Ref<EntityStore>> woodPileRefs = spawnCrowd(store, roleIndex, layout.woodNodePositions(), visibleResourceCount(state.resources().wood()));
            List<Ref<EntityStore>> ironPileRefs = spawnCrowd(store, roleIndex, layout.ironNodePositions(), visibleResourceCount(state.resources().iron()));

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
}
