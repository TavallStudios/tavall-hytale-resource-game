package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.config.CastleAssetConfig;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleSiteVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerSessionStore;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.world.CastleSiteLayout;
import com.tavall.hytale.resourcegame.world.CastleSiteLayoutService;
import com.tavall.hytale.resourcegame.world.CastleSiteStructureService;
import com.tavall.hytale.resourcegame.world.CastleSiteVisualRefs;
import com.tavall.hytale.resourcegame.world.ProtectedStructureType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.List;

/**
 * Maintains the main-world castle block marker plus its overhead label.
 */
public final class CastleSiteVisualService implements ICastleSiteVisualService, IDependencyInjectableConcrete {
    private static final Logger LOGGER = Logger.getLogger(CastleSiteVisualService.class.getName());

    private final CastleAssetConfig castleAssetConfig;
    private final CastleSiteLayoutService layoutService;
    private final CastleSiteStructureService structureService;
    private final WorldLabelService worldLabelService;
    private final StructureProtectionService protectionService;
    private final IPlayerSessionStore sessionStore;
    private final Map<UUID, CastleSiteVisualRefs> siteRefs = new ConcurrentHashMap<>();

    public CastleSiteVisualService(
            CastleAssetConfig castleAssetConfig,
            CastleSiteLayoutService layoutService,
            CastleSiteStructureService structureService,
            WorldLabelService worldLabelService,
            StructureProtectionService protectionService,
            IPlayerSessionStore sessionStore
    ) {
        this.castleAssetConfig = castleAssetConfig;
        this.layoutService = layoutService;
        this.structureService = structureService;
        this.worldLabelService = worldLabelService;
        this.protectionService = protectionService;
        this.sessionStore = sessionStore;
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
        protectionService.clearStructure(structureKey(playerId));
        if (refs == null) {
            return;
        }
        World world = Universe.get().getWorld(refs.worldName());
        if (world != null) {
            world.execute(() -> clearRefsOnWorld(world, refs));
            return;
        }
        removeRefs(refs);
    }

    private void rebuildSite(UUID playerId, PlayerGameState state) {
        if (playerId == null || state == null || state.castleLocation() == null) {
            return;
        }
        World world = Universe.get().getWorld(state.castleLocation().worldName());
        if (world == null) {
            return;
        }
        CastleSiteVisualRefs previousRefs = siteRefs.remove(playerId);
        protectionService.clearStructure(structureKey(playerId));
        world.execute(() -> {
            clearExistingSite(world, previousRefs);
            CastleSiteLayout layout = layoutService.createLayout(state.castleLocation());
            protectionService.replaceStructure(
                    structureKey(playerId),
                    playerId,
                    ProtectedStructureType.CASTLE,
                    world.getName(),
                    state.castleAssetType(),
                    structureService.ensureSite(world, layout)
            );
            List<Ref<EntityStore>> labelRefs = worldLabelService.spawnLabelStack(
                    world,
                    new Vector3d(state.castleLocation().x(), state.castleLocation().y() + 3.8D, state.castleLocation().z()),
                    castleLabelLines(playerId, state)
            );
            siteRefs.put(playerId, new CastleSiteVisualRefs(world.getName(), layout.origin(), labelRefs));
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

    private void clearExistingSite(World targetWorld, CastleSiteVisualRefs refs) {
        if (refs == null) {
            return;
        }
        World previousWorld = Universe.get().getWorld(refs.worldName());
        if (previousWorld == null) {
            removeRefs(refs);
            return;
        }
        if (targetWorld != null && targetWorld.getName().equals(previousWorld.getName())) {
            clearRefsOnWorld(previousWorld, refs);
            return;
        }
        previousWorld.execute(() -> clearRefsOnWorld(previousWorld, refs));
    }

    private void clearRefsOnWorld(World world, CastleSiteVisualRefs refs) {
        CastleLocationData location = new CastleLocationData(
                refs.worldName(),
                refs.worldPosition().getX(),
                refs.worldPosition().getY(),
                refs.worldPosition().getZ()
        );
        CastleSiteLayout layout = layoutService.createLayout(location);
        structureService.clearSite(world, layout);
        removeRefs(refs);
    }

    private void removeRefs(CastleSiteVisualRefs refs) {
        for (Ref<EntityStore> ref : refs.allRefs()) {
            if (ref != null && ref.isValid()) {
                ref.getStore().removeEntity(ref, RemoveReason.REMOVE);
            }
        }
    }

    private List<String> castleLabelLines(UUID playerId, PlayerGameState state) {
        return List.of(
                castleAssetConfig.displayName() + " | " + ownerName(playerId),
                "Troops " + state.populationSummary().troopCount() + " | Might " + state.populationSummary().might(),
                "Look within 7.5m | Right-click for stats, attack, friend"
        );
    }

    private String ownerName(UUID playerId) {
        PlayerSession session = sessionStore.get(playerId);
        if (session != null && session.profile() != null && session.profile().name() != null && !session.profile().name().isBlank()) {
            return session.profile().name();
        }
        return playerId.toString().substring(0, 8);
    }

    private String structureKey(UUID playerId) {
        return "castle:" + playerId;
    }
}
