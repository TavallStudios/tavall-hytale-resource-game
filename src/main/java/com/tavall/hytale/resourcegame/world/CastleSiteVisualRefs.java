package com.tavall.hytale.resourcegame.world;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks the spawned refs that make up the castle-site scene.
 */
public final class CastleSiteVisualRefs {
    private final Ref<EntityStore> stockpileAnchorRef;
    private final Ref<EntityStore> citizenAnchorRef;
    private final Ref<EntityStore> troopAnchorRef;
    private final Ref<EntityStore> foodNodeRef;
    private final Ref<EntityStore> woodNodeRef;
    private final Ref<EntityStore> ironNodeRef;
    private final List<Ref<EntityStore>> stockpileRefs;
    private final List<Ref<EntityStore>> citizenCrowdRefs;
    private final List<Ref<EntityStore>> troopCrowdRefs;
    private final List<Ref<EntityStore>> foodPileRefs;
    private final List<Ref<EntityStore>> woodPileRefs;
    private final List<Ref<EntityStore>> ironPileRefs;
    private final List<Ref<EntityStore>> foodConvoyRefs;
    private final List<Ref<EntityStore>> woodConvoyRefs;
    private final List<Ref<EntityStore>> ironConvoyRefs;

    public CastleSiteVisualRefs(
            Ref<EntityStore> stockpileAnchorRef,
            Ref<EntityStore> citizenAnchorRef,
            Ref<EntityStore> troopAnchorRef,
            Ref<EntityStore> foodNodeRef,
            Ref<EntityStore> woodNodeRef,
            Ref<EntityStore> ironNodeRef,
            List<Ref<EntityStore>> stockpileRefs,
            List<Ref<EntityStore>> citizenCrowdRefs,
            List<Ref<EntityStore>> troopCrowdRefs,
            List<Ref<EntityStore>> foodPileRefs,
            List<Ref<EntityStore>> woodPileRefs,
            List<Ref<EntityStore>> ironPileRefs,
            List<Ref<EntityStore>> foodConvoyRefs,
            List<Ref<EntityStore>> woodConvoyRefs,
            List<Ref<EntityStore>> ironConvoyRefs
    ) {
        this.stockpileAnchorRef = stockpileAnchorRef;
        this.citizenAnchorRef = citizenAnchorRef;
        this.troopAnchorRef = troopAnchorRef;
        this.foodNodeRef = foodNodeRef;
        this.woodNodeRef = woodNodeRef;
        this.ironNodeRef = ironNodeRef;
        this.stockpileRefs = List.copyOf(stockpileRefs);
        this.citizenCrowdRefs = List.copyOf(citizenCrowdRefs);
        this.troopCrowdRefs = List.copyOf(troopCrowdRefs);
        this.foodPileRefs = List.copyOf(foodPileRefs);
        this.woodPileRefs = List.copyOf(woodPileRefs);
        this.ironPileRefs = List.copyOf(ironPileRefs);
        this.foodConvoyRefs = List.copyOf(foodConvoyRefs);
        this.woodConvoyRefs = List.copyOf(woodConvoyRefs);
        this.ironConvoyRefs = List.copyOf(ironConvoyRefs);
    }

    public List<Ref<EntityStore>> allRefs() {
        List<Ref<EntityStore>> refs = new ArrayList<>();
        addIfPresent(refs, stockpileAnchorRef);
        addIfPresent(refs, citizenAnchorRef);
        addIfPresent(refs, troopAnchorRef);
        addIfPresent(refs, foodNodeRef);
        addIfPresent(refs, woodNodeRef);
        addIfPresent(refs, ironNodeRef);
        refs.addAll(stockpileRefs);
        refs.addAll(citizenCrowdRefs);
        refs.addAll(troopCrowdRefs);
        refs.addAll(foodPileRefs);
        refs.addAll(woodPileRefs);
        refs.addAll(ironPileRefs);
        refs.addAll(foodConvoyRefs);
        refs.addAll(woodConvoyRefs);
        refs.addAll(ironConvoyRefs);
        return List.copyOf(refs);
    }

    private void addIfPresent(List<Ref<EntityStore>> refs, Ref<EntityStore> ref) {
        if (ref != null) {
            refs.add(ref);
        }
    }
}
