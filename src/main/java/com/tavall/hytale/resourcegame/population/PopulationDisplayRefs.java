package com.tavall.hytale.resourcegame.population;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.domain.CitizenJobType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds anchor entity references for population displays.
 */
public final class PopulationDisplayRefs {
    private final String worldName;
    private final Map<CitizenJobType, Vector3d> workerPositions;
    private final Map<CitizenJobType, String> workerLabels;
    private final Map<CitizenJobType, Ref<EntityStore>> workerRefs;
    private final Ref<EntityStore> troopsRef;
    private final String troopLabel;
    private final int citizenCount;
    private final int troopCount;

    public PopulationDisplayRefs(
            String worldName,
            Map<CitizenJobType, Vector3d> workerPositions,
            Map<CitizenJobType, String> workerLabels,
            Map<CitizenJobType, Ref<EntityStore>> workerRefs,
            Ref<EntityStore> troopsRef,
            String troopLabel,
            int citizenCount,
            int troopCount
    ) {
        this.worldName = worldName;
        this.workerPositions = Map.copyOf(new EnumMap<>(workerPositions));
        this.workerLabels = Map.copyOf(new EnumMap<>(workerLabels));
        this.workerRefs = Map.copyOf(workerRefs);
        this.troopsRef = troopsRef;
        this.troopLabel = troopLabel;
        this.citizenCount = citizenCount;
        this.troopCount = troopCount;
    }

    public String worldName() {
        return worldName;
    }

    public Map<CitizenJobType, Vector3d> workerPositions() {
        return workerPositions;
    }

    public Map<CitizenJobType, String> workerLabels() {
        return workerLabels;
    }

    public Ref<EntityStore> citizensRef() {
        return workerRefs.get(CitizenJobType.IDLE);
    }

    public Ref<EntityStore> troopsRef() {
        return troopsRef;
    }

    public String troopLabel() {
        return troopLabel;
    }

    public int citizenCount() {
        return citizenCount;
    }

    public int troopCount() {
        return troopCount;
    }

    public Map<CitizenJobType, Ref<EntityStore>> workerRefs() {
        return workerRefs;
    }

    public List<Ref<EntityStore>> allRefs() {
        List<Ref<EntityStore>> refs = new ArrayList<>(workerRefs.values());
        refs.add(troopsRef);
        return List.copyOf(refs);
    }

    public Optional<CitizenJobType> resolveWorkerType(Ref<EntityStore> targetRef) {
        return workerRefs.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().equals(targetRef))
                .map(Map.Entry::getKey)
                .findFirst();
    }
}
