package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleBuildingService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleBuildingVisualService;
import com.tavall.hytale.resourcegame.domain.CastleBuildingData;
import com.tavall.hytale.resourcegame.domain.CastleBuildingSummary;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.population.PromotionCost;
import com.tavall.hytale.resourcegame.world.CastleBuildingStructureService;
import com.tavall.hytale.resourcegame.world.CastleBuildingVisualRefs;
import com.tavall.hytale.resourcegame.world.ProtectedStructureType;
import com.tavall.hytale.resourcegame.tasks.WorldTasks;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders placed kingdom buildings as block-only staged structures.
 */
public final class CastleBuildingVisualService implements ICastleBuildingVisualService, IDependencyInjectableConcrete {
    private final ICastleBuildingService buildingService;
    private final CastleBuildingStructureService structureService;
    private final WorldLabelService worldLabelService;
    private final StructureProtectionService protectionService;
    private final Map<UUID, Map<UUID, CastleBuildingVisualRefs>> buildingRefs = new ConcurrentHashMap<>();

    public CastleBuildingVisualService(
            ICastleBuildingService buildingService,
            CastleBuildingStructureService structureService,
            WorldLabelService worldLabelService,
            StructureProtectionService protectionService
    ) {
        this.buildingService = buildingService;
        this.structureService = structureService;
        this.worldLabelService = worldLabelService;
        this.protectionService = protectionService;
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
        for (Map.Entry<UUID, CastleBuildingVisualRefs> entry : refsByBuilding.entrySet()) {
            protectionService.clearStructure(structureKey(entry.getKey()));
            CastleBuildingVisualRefs refs = entry.getValue();
            World world = Universe.get().getWorld(refs.worldName());
            if (world != null) {
                WorldTasks.executeSafe(world, "CastleBuildingVisualService.clearRefsOnWorld", () -> clearRefsOnWorld(world, refs));
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
        Map<UUID, CastleBuildingVisualRefs> previousRefs = buildingRefs.remove(playerId);
        clearExistingBuildings(previousRefs);
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
            WorldTasks.executeSafe(world, "CastleBuildingVisualService.rebuildBuilding(" + building.buildingId() + ")", () -> {
                List<Ref<EntityStore>> labelRefs = spawnLabels(world, summary);
                protectionService.replaceStructure(
                        structureKey(building.buildingId()),
                        playerId,
                        ProtectedStructureType.BUILDING,
                        summary.worldName(),
                        building.buildingType().shortKey(),
                        structureService.ensureBuildingSite(world, summary)
                );
                rebuilt.put(
                        building.buildingId(),
                        new CastleBuildingVisualRefs(
                                summary.worldName(),
                                new Vector3d(summary.worldX(), summary.worldY(), summary.worldZ()),
                                labelRefs
                        )
                );
            });
        }
        buildingRefs.put(playerId, rebuilt);
    }

    private void clearExistingBuildings(Map<UUID, CastleBuildingVisualRefs> refsByBuilding) {
        if (refsByBuilding == null || refsByBuilding.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, CastleBuildingVisualRefs> entry : refsByBuilding.entrySet()) {
            protectionService.clearStructure(structureKey(entry.getKey()));
            CastleBuildingVisualRefs refs = entry.getValue();
            World world = Universe.get().getWorld(refs.worldName());
            if (world != null) {
                WorldTasks.executeSafe(world, "CastleBuildingVisualService.clearRefsOnWorld", () -> clearRefsOnWorld(world, refs));
                continue;
            }
            for (Ref<EntityStore> ref : refs.allRefs()) {
                removeRef(ref);
            }
        }
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
        try {
            ref.getStore().removeEntity(ref, RemoveReason.REMOVE);
        } catch (Throwable ignored) {
        }
    }

    private List<Ref<EntityStore>> spawnLabels(World world, CastleBuildingSummary summary) {
        Vector3d labelPosition = new Vector3d(summary.worldX(), summary.worldY() + 2.6D, summary.worldZ());
        return worldLabelService.spawnLabelStack(world, labelPosition, buildingLabelLines(summary));
    }

    private List<String> buildingLabelLines(CastleBuildingSummary summary) {
        StringBuilder bonusBuilder = new StringBuilder();
        if (summary.foodPerTickBonus() > 0) {
            appendToken(bonusBuilder, "+" + summary.foodPerTickBonus() + "F/t");
        }
        if (summary.woodPerTickBonus() > 0) {
            appendToken(bonusBuilder, "+" + summary.woodPerTickBonus() + "W/t");
        }
        if (summary.ironPerTickBonus() > 0) {
            appendToken(bonusBuilder, "+" + summary.ironPerTickBonus() + "I/t");
        }
        if (summary.constructionSpeedBonus() > 0.0D) {
            appendToken(bonusBuilder, "Build +" + (int) Math.round(summary.constructionSpeedBonus() * 100.0D) + "%");
        }
        PromotionCost promotionDiscount = summary.promotionDiscount();
        int totalDiscount = promotionDiscount.foodCost() + promotionDiscount.woodCost() + promotionDiscount.ironCost();
        if (totalDiscount > 0) {
            appendToken(
                    bonusBuilder,
                    "Promo -" + promotionDiscount.foodCost() + "F/"
                            + promotionDiscount.woodCost() + "W/"
                            + promotionDiscount.ironCost() + "I"
            );
        }
        String detailLine = bonusBuilder.length() == 0 ? "No passive bonuses yet" : bonusBuilder.toString();
        String actionLine = summary.isUnderConstruction()
                ? summary.remainingSeconds() + "s left"
                : "Right-click";
        return List.of(
                summary.buildingData().buildingType().displayName()
                        + " | Lv " + summary.displayLevel()
                        + " | " + summary.statusText(),
                detailLine,
                actionLine
        );
    }

    private void appendToken(StringBuilder builder, String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(" | ");
        }
        builder.append(token);
    }

    private String structureKey(UUID buildingId) {
        return "building:" + buildingId;
    }
}
