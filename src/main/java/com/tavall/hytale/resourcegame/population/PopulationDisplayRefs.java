package com.tavall.hytale.resourcegame.population;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.domain.CitizenJobType;

import java.util.Map;
import java.util.Optional;

/**
 * Holds anchor entity references for population displays.
 */
public final class PopulationDisplayRefs {
    private final Map<CitizenJobType, Ref<EntityStore>> workerRefs;
    private final Ref<EntityStore> troopsRef;

    public PopulationDisplayRefs(Map<CitizenJobType, Ref<EntityStore>> workerRefs, Ref<EntityStore> troopsRef) {
        this.workerRefs = Map.copyOf(workerRefs);
        this.troopsRef = troopsRef;
    }

    public Ref<EntityStore> citizensRef() {
        return workerRefs.get(CitizenJobType.IDLE);
    }

    public Ref<EntityStore> troopsRef() {
        return troopsRef;
    }

    public Map<CitizenJobType, Ref<EntityStore>> workerRefs() {
        return workerRefs;
    }

    public Optional<CitizenJobType> resolveWorkerType(Ref<EntityStore> targetRef) {
        return workerRefs.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().equals(targetRef))
                .map(Map.Entry::getKey)
                .findFirst();
    }
}
