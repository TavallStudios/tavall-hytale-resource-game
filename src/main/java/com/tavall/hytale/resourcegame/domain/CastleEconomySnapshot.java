package com.tavall.hytale.resourcegame.domain;

import com.tavall.hytale.resourcegame.resources.ResourceType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Derived workforce and production snapshot for the current castle state.
 */
public final class CastleEconomySnapshot {
    private final Map<CitizenJobType, Integer> jobCounts;
    private final Map<ResourceType, Integer> nodeWorkers;
    private final Map<ResourceType, Integer> resourceGainPerTick;

    public CastleEconomySnapshot(
            Map<CitizenJobType, Integer> jobCounts,
            Map<ResourceType, Integer> nodeWorkers,
            Map<ResourceType, Integer> resourceGainPerTick
    ) {
        EnumMap<CitizenJobType, Integer> safeJobCounts = new EnumMap<>(CitizenJobType.class);
        for (CitizenJobType type : CitizenJobType.values()) {
            safeJobCounts.put(type, jobCounts == null ? 0 : Math.max(0, jobCounts.getOrDefault(type, 0)));
        }
        EnumMap<ResourceType, Integer> safeNodeWorkers = new EnumMap<>(ResourceType.class);
        EnumMap<ResourceType, Integer> safeGain = new EnumMap<>(ResourceType.class);
        for (ResourceType type : ResourceType.values()) {
            safeNodeWorkers.put(type, nodeWorkers == null ? 0 : Math.max(0, nodeWorkers.getOrDefault(type, 0)));
            safeGain.put(type, resourceGainPerTick == null ? 0 : Math.max(0, resourceGainPerTick.getOrDefault(type, 0)));
        }
        this.jobCounts = Map.copyOf(safeJobCounts);
        this.nodeWorkers = Map.copyOf(safeNodeWorkers);
        this.resourceGainPerTick = Map.copyOf(safeGain);
    }

    public Map<CitizenJobType, Integer> jobCounts() {
        return jobCounts;
    }

    public int jobCount(CitizenJobType jobType) {
        return jobCounts.getOrDefault(jobType, 0);
    }

    public Map<ResourceType, Integer> nodeWorkers() {
        return nodeWorkers;
    }

    public int workersFor(ResourceType resourceType) {
        return nodeWorkers.getOrDefault(resourceType, 0);
    }

    public Map<ResourceType, Integer> resourceGainPerTick() {
        return resourceGainPerTick;
    }

    public int gainFor(ResourceType resourceType) {
        return resourceGainPerTick.getOrDefault(resourceType, 0);
    }
}
