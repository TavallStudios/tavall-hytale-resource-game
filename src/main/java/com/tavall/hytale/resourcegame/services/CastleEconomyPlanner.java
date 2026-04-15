package com.tavall.hytale.resourcegame.services;

import com.tavall.hytale.resourcegame.domain.CastleEconomySnapshot;
import com.tavall.hytale.resourcegame.domain.CitizenJobType;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.resources.ResourceType;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;

/**
 * Computes workforce distribution and per-tick production from the current castle state.
 */
public final class CastleEconomyPlanner {
    private static final int FOOD_PER_WORKER = 3;
    private static final int WOOD_PER_WORKER = 2;
    private static final int IRON_PER_WORKER = 1;

    public CastleEconomySnapshot snapshot(PlayerGameState state) {
        int citizens = Math.max(0, state.populationSummary().citizenCount());
        int troops = Math.max(0, state.populationSummary().troopCount());

        int builders = citizens >= 8 ? 1 : 0;
        int trainees = troops < 4 ? Math.min(2, Math.max(0, citizens / 5)) : Math.min(1, Math.max(0, citizens / 8));
        int idle = citizens >= 10 ? 1 : 0;
        int gatherers = Math.max(0, citizens - builders - trainees - idle);

        if (gatherers == 0 && citizens > 0) {
            if (trainees > 0) {
                trainees--;
                gatherers++;
            } else if (builders > 0) {
                builders--;
                gatherers++;
            } else if (idle > 0) {
                idle--;
                gatherers++;
            }
        }

        EnumMap<CitizenJobType, Integer> jobCounts = new EnumMap<>(CitizenJobType.class);
        jobCounts.put(CitizenJobType.IDLE, idle);
        jobCounts.put(CitizenJobType.GATHERER, gatherers);
        jobCounts.put(CitizenJobType.BUILDER, builders);
        jobCounts.put(CitizenJobType.TRAINEE, trainees);
        jobCounts.put(CitizenJobType.SOLDIER, 0);

        EnumMap<ResourceType, Integer> workers = allocateGatherers(state, gatherers);
        EnumMap<ResourceType, Integer> gains = new EnumMap<>(ResourceType.class);
        gains.put(ResourceType.FOOD, workers.get(ResourceType.FOOD) * FOOD_PER_WORKER);
        gains.put(ResourceType.WOOD, workers.get(ResourceType.WOOD) * WOOD_PER_WORKER);
        gains.put(ResourceType.IRON, workers.get(ResourceType.IRON) * IRON_PER_WORKER);
        return new CastleEconomySnapshot(jobCounts, workers, gains);
    }

    public String workforceSummary(PlayerGameState state) {
        CastleEconomySnapshot snapshot = snapshot(state);
        return "Gatherers "
                + snapshot.jobCount(CitizenJobType.GATHERER)
                + " | Builders "
                + snapshot.jobCount(CitizenJobType.BUILDER)
                + " | Trainees "
                + snapshot.jobCount(CitizenJobType.TRAINEE)
                + " | Idle "
                + snapshot.jobCount(CitizenJobType.IDLE);
    }

    public String nodeSummary(PlayerGameState state, ResourceType resourceType) {
        CastleEconomySnapshot snapshot = snapshot(state);
        int currentAmount = switch (resourceType) {
            case FOOD -> state.resources().food();
            case WOOD -> state.resources().wood();
            case IRON -> state.resources().iron();
        };
        return currentAmount
                + " stored | "
                + snapshot.workersFor(resourceType)
                + " workers | +"
                + snapshot.gainFor(resourceType)
                + " per tick";
    }

    private EnumMap<ResourceType, Integer> allocateGatherers(PlayerGameState state, int gatherers) {
        EnumMap<ResourceType, Integer> workers = new EnumMap<>(ResourceType.class);
        for (ResourceType type : ResourceType.values()) {
            workers.put(type, 0);
        }
        if (gatherers <= 0) {
            return workers;
        }

        EnumMap<ResourceType, Integer> pressure = new EnumMap<>(ResourceType.class);
        pressure.put(ResourceType.FOOD, Math.max(1, 60 + (state.populationSummary().troopCount() * 4) - state.resources().food()));
        pressure.put(ResourceType.WOOD, Math.max(1, 45 + state.populationSummary().citizenCount() - state.resources().wood()));
        pressure.put(ResourceType.IRON, Math.max(1, 25 + (state.populationSummary().troopCount() * 3) - state.resources().iron()));

        List<ResourceType> priority = List.of(ResourceType.FOOD, ResourceType.WOOD, ResourceType.IRON);
        int remaining = gatherers;
        if (remaining >= 3) {
            for (ResourceType type : priority) {
                workers.put(type, 1);
                remaining--;
            }
        }

        while (remaining > 0) {
            ResourceType next = priority.stream()
                    .max(Comparator.comparingInt(type -> pressure.get(type) - (workers.get(type) * 10)))
                    .orElse(ResourceType.FOOD);
            workers.put(next, workers.get(next) + 1);
            remaining--;
        }
        return workers;
    }
}
