package com.tavall.hytale.resourcegame.world;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks the spawned refs that make up the castle-site scene.
 */
public final class CastleSiteVisualRefs {
    private final Ref<EntityStore> citizenAnchorRef;
    private final Ref<EntityStore> troopAnchorRef;
    private final Ref<EntityStore> foodNodeRef;
    private final Ref<EntityStore> woodNodeRef;
    private final Ref<EntityStore> ironNodeRef;
    private final List<Ref<EntityStore>> citizenCrowdRefs;
    private final List<Ref<EntityStore>> troopCrowdRefs;
    private final List<Ref<EntityStore>> foodPileRefs;
    private final List<Ref<EntityStore>> woodPileRefs;
    private final List<Ref<EntityStore>> ironPileRefs;

    public CastleSiteVisualRefs(
            Ref<EntityStore> citizenAnchorRef,
            Ref<EntityStore> troopAnchorRef,
            Ref<EntityStore> foodNodeRef,
            Ref<EntityStore> woodNodeRef,
            Ref<EntityStore> ironNodeRef,
            List<Ref<EntityStore>> citizenCrowdRefs,
            List<Ref<EntityStore>> troopCrowdRefs,
            List<Ref<EntityStore>> foodPileRefs,
            List<Ref<EntityStore>> woodPileRefs,
            List<Ref<EntityStore>> ironPileRefs
    ) {
        this.citizenAnchorRef = citizenAnchorRef;
        this.troopAnchorRef = troopAnchorRef;
        this.foodNodeRef = foodNodeRef;
        this.woodNodeRef = woodNodeRef;
        this.ironNodeRef = ironNodeRef;
        this.citizenCrowdRefs = List.copyOf(citizenCrowdRefs);
        this.troopCrowdRefs = List.copyOf(troopCrowdRefs);
        this.foodPileRefs = List.copyOf(foodPileRefs);
        this.woodPileRefs = List.copyOf(woodPileRefs);
        this.ironPileRefs = List.copyOf(ironPileRefs);
    }

    public List<Ref<EntityStore>> allRefs() {
        List<Ref<EntityStore>> refs = new ArrayList<>();
        addIfPresent(refs, citizenAnchorRef);
        addIfPresent(refs, troopAnchorRef);
        addIfPresent(refs, foodNodeRef);
        addIfPresent(refs, woodNodeRef);
        addIfPresent(refs, ironNodeRef);
        refs.addAll(citizenCrowdRefs);
        refs.addAll(troopCrowdRefs);
        refs.addAll(foodPileRefs);
        refs.addAll(woodPileRefs);
        refs.addAll(ironPileRefs);
        return List.copyOf(refs);
    }

    private void addIfPresent(List<Ref<EntityStore>> refs, Ref<EntityStore> ref) {
        if (ref != null) {
            refs.add(ref);
        }
    }
}
