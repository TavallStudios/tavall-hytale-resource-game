package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.tavall.hytale.resourcegame.config.PopulationDisplayConfig;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleBuildingService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleBuildingVisualService;
import com.tavall.hytale.resourcegame.domain.BuildingAreaType;
import com.tavall.hytale.resourcegame.domain.BuildingConstructionStage;
import com.tavall.hytale.resourcegame.domain.CastleBuildingData;
import com.tavall.hytale.resourcegame.domain.CastleBuildingSummary;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.world.CastleBuildingStructureService;
import com.tavall.hytale.resourcegame.world.CastleBuildingVisualRefs;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Renders placed kingdom buildings with staged construction visuals.
 */
public final class CastleBuildingVisualService implements ICastleBuildingVisualService, IDependencyInjectableConcrete {
    private static final Logger LOGGER = Logger.getLogger(CastleBuildingVisualService.class.getName());

    private final PopulationDisplayConfig displayConfig;
    private final ICastleBuildingService buildingService;
    private final CastleBuildingStructureService structureService;
    private final NpcVisualSpawner npcVisualSpawner;
    private final Map<UUID, Map<UUID, CastleBuildingVisualRefs>> buildingRefs = new ConcurrentHashMap<>();

    public CastleBuildingVisualService(
            PopulationDisplayConfig displayConfig,
            ICastleBuildingService buildingService,
            CastleBuildingStructureService structureService,
            NpcVisualSpawner npcVisualSpawner
    ) {
        this.displayConfig = displayConfig;
        this.buildingService = buildingService;
        this.structureService = structureService;
        this.npcVisualSpawner = npcVisualSpawner;
    }

    @Override
    public void ensureBuildings(UUID playerId, PlayerGameState state) {
        rebuildBuildings(playerId, state);
    }

    @Override
    public void refreshBuildings(UUID playerId, PlayerGameState state) {
        rebuildBuildings(playerId, state);
    }

    @Override
    public void clearBuildings(UUID playerId) {
        Map<UUID, CastleBuildingVisualRefs> refsByBuilding = buildingRefs.remove(playerId);
        if (refsByBuilding == null) {
            return;
        }
        for (CastleBuildingVisualRefs refs : refsByBuilding.values()) {
            World world = Universe.get().getWorld(refs.worldName());
            if (world != null) {
                world.execute(() -> clearRefsOnWorld(world, refs));
                continue;
            }
            for (Ref<EntityStore> ref : refs.allRefs()) {
                removeRef(ref);
            }
        }
    }

    @Override
    public Optional<UUID> findBuildingId(UUID playerId, Ref<EntityStore> targetRef) {
        if (playerId == null || targetRef == null || !targetRef.isValid()) {
            return Optional.empty();
        }
        Map<UUID, CastleBuildingVisualRefs> refsByBuilding = buildingRefs.get(playerId);
        if (refsByBuilding == null) {
            return Optional.empty();
        }
        return refsByBuilding.entrySet().stream()
                .filter(entry -> entry.getValue().matches(targetRef))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private void rebuildBuildings(UUID playerId, PlayerGameState state) {
        clearBuildings(playerId);
        if (playerId == null || state == null) {
            return;
        }
        List<CastleBuildingData> buildings = buildingService.listBuildings(state);
        if (buildings.isEmpty()) {
            return;
        }
        Map<UUID, CastleBuildingVisualRefs> rebuilt = new ConcurrentHashMap<>();
        for (CastleBuildingData building : buildings) {
            CastleBuildingSummary summary = buildingService.summary(playerId, state, building, Instant.now());
            World world = Universe.get().getWorld(summary.worldName());
            if (world == null) {
                continue;
            }
            world.execute(() -> {
                Store<EntityStore> store = world.getEntityStore().getStore();
                int roleIndex = new NpcRoleResolver().resolveRoleIndex(displayConfig.npcRoleName());
                if (roleIndex < 0) {
                    LOGGER.warning(() -> "Unable to build kingdom building visuals because NPC role '" + displayConfig.npcRoleName() + "' was not found.");
                    return;
                }
                Vector3d position = new Vector3d(summary.worldX(), summary.worldY(), summary.worldZ());
                structureService.ensureBuildingSite(world, summary);
                Ref<EntityStore> anchorRef = npcVisualSpawner.spawnNamed(
                        store,
                        roleIndex,
                        position.add(0.0D, 2.6D, 0.0D),
                        label(summary),
                        anchorScale(summary)
                );
                List<Ref<EntityStore>> crewRefs = npcVisualSpawner.spawnGroup(
                        store,
                        roleIndex,
                        crewPositions(position, summary),
                        visibleCrewCount(summary),
                        crewScale(summary)
                );
                List<Ref<EntityStore>> scaffoldRefs = npcVisualSpawner.spawnGroup(
                        store,
                        roleIndex,
                        scaffoldPositions(position, summary.constructionStage()),
                        visibleScaffoldCount(summary),
                        scaffoldScale(summary)
                );
                rebuilt.put(
                        building.buildingId(),
                        new CastleBuildingVisualRefs(summary.worldName(), position, anchorRef, crewRefs, scaffoldRefs)
                );
            });
        }
        buildingRefs.put(playerId, rebuilt);
    }

    private void clearRefsOnWorld(World world, CastleBuildingVisualRefs refs) {
        structureService.clearBuildingSite(world, refs.worldPosition());
        for (Ref<EntityStore> ref : refs.allRefs()) {
            removeRef(ref);
        }
    }

    private void removeRef(Ref<EntityStore> ref) {
        if (ref == null || !ref.isValid()) {
            return;
        }
        ref.getStore().removeEntity(ref, RemoveReason.REMOVE);
    }

    private String label(CastleBuildingSummary summary) {
        String effectLine = summary.foodPerTickBonus() > 0 || summary.woodPerTickBonus() > 0 || summary.ironPerTickBonus() > 0
                ? " | +" + summary.foodPerTickBonus() + "F +" + summary.woodPerTickBonus() + "W +" + summary.ironPerTickBonus() + "I"
                : summary.constructionSpeedBonus() > 0.0D
                ? " | Build Speed " + (int) Math.round(summary.constructionSpeedBonus() * 100.0D) + "%"
                : " | Troop Cost -" + summary.promotionDiscount().foodCost() + "/" + summary.promotionDiscount().woodCost() + "/" + summary.promotionDiscount().ironCost();
        String stageText = summary.isUnderConstruction()
                ? " | " + summary.constructionStage().name().toLowerCase()
                + " " + (int) Math.round(summary.progressRatio() * 100.0D) + "%"
                + " | " + summary.remainingSeconds() + "s"
                : " | L" + summary.completedLevel();
        return summary.buildingData().buildingType().displayName() + stageText + effectLine;
    }

    private float anchorScale(CastleBuildingSummary summary) {
        return clampScale(summary.isUnderConstruction() ? 1.15F + (float) summary.progressRatio() : 1.3F + (summary.completedLevel() * 0.18F), 1.0F, 2.4F);
    }

    private float crewScale(CastleBuildingSummary summary) {
        float areaBonus = summary.buildingData().areaType() == BuildingAreaType.CASTLE_INTERIOR ? 0.1F : 0.0F;
        return clampScale(0.75F + (summary.displayLevel() * 0.12F) + areaBonus, 0.7F, 1.4F);
    }

    private float scaffoldScale(CastleBuildingSummary summary) {
        return clampScale(0.65F + (float) (summary.progressRatio() * 0.4F), 0.65F, 1.2F);
    }

    private int visibleCrewCount(CastleBuildingSummary summary) {
        if (summary.isUnderConstruction()) {
            return Math.min(4, 1 + summary.displayLevel());
        }
        return Math.min(5, 1 + summary.completedLevel());
    }

    private int visibleScaffoldCount(CastleBuildingSummary summary) {
        return switch (summary.constructionStage()) {
            case FOUNDATION -> summary.isUnderConstruction() ? 1 : 0;
            case SCAFFOLDING -> 4;
            case SHELL -> 3;
            case COMPLETE -> 0;
        };
    }

    private List<Vector3d> crewPositions(Vector3d base, CastleBuildingSummary summary) {
        double offset = summary.buildingData().areaType() == BuildingAreaType.CASTLE_INTERIOR ? 1.3D : 1.8D;
        List<Vector3d> positions = new ArrayList<>();
        positions.add(new Vector3d(base.getX() + offset, base.getY(), base.getZ()));
        positions.add(new Vector3d(base.getX() - offset, base.getY(), base.getZ()));
        positions.add(new Vector3d(base.getX(), base.getY(), base.getZ() + offset));
        positions.add(new Vector3d(base.getX(), base.getY(), base.getZ() - offset));
        positions.add(new Vector3d(base.getX() + offset, base.getY(), base.getZ() + offset));
        return positions;
    }

    private List<Vector3d> scaffoldPositions(Vector3d base, BuildingConstructionStage stage) {
        if (stage == BuildingConstructionStage.COMPLETE) {
            return List.of();
        }
        double height = switch (stage) {
            case FOUNDATION -> 1.2D;
            case SCAFFOLDING -> 2.0D;
            case SHELL -> 2.8D;
            case COMPLETE -> 0.0D;
        };
        return List.of(
                new Vector3d(base.getX() + 1.8D, base.getY() + height, base.getZ() + 1.8D),
                new Vector3d(base.getX() - 1.8D, base.getY() + height, base.getZ() + 1.8D),
                new Vector3d(base.getX() + 1.8D, base.getY() + height, base.getZ() - 1.8D),
                new Vector3d(base.getX() - 1.8D, base.getY() + height, base.getZ() - 1.8D)
        );
    }

    private float clampScale(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
