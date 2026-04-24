package com.tavall.hytale.resourcegame.services;

import com.tavall.hytale.resourcegame.domain.CastleEconomySnapshot;
import com.tavall.hytale.resourcegame.domain.CitizenJobType;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.resources.ResourceType;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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

        int remaining = citizens;
        int idle = take(remaining, citizens >= 10 ? 1 : 0);
        remaining -= idle;
        int trainees = take(remaining, troops < 4 ? Math.min(2, citizens / 5) : Math.min(1, citizens / 8));
        remaining -= trainees;
        int architect = take(remaining, citizens >= 12 ? 1 : 0);
        remaining -= architect;
        int blacksmith = take(remaining, citizens >= 10 && troops > 0 ? 1 : 0);
        remaining -= blacksmith;
        int cook = take(remaining, citizens >= 6 ? 1 : 0);
        remaining -= cook;
        int hunter = take(remaining, citizens >= 5 ? 1 : 0);
        remaining -= hunter;
        int miner = take(remaining, citizens >= 7 ? 1 : 0);
        remaining -= miner;
        int gruntBuilder = take(remaining, citizens >= 8 ? 1 : 0);
        remaining -= gruntBuilder;
        int gatherers = Math.max(0, remaining);

        if (gatherers == 0 && citizens > 0) {
            gatherers = 1;
            if (idle > 0) {
                idle--;
            } else if (trainees > 0) {
                trainees--;
            } else if (gruntBuilder > 0) {
                gruntBuilder--;
            } else if (architect > 0) {
                architect--;
            } else if (blacksmith > 0) {
                blacksmith--;
            } else if (cook > 0) {
                cook--;
            } else if (hunter > 0) {
                hunter--;
            } else if (miner > 0) {
                miner--;
            }
        }

        EnumMap<CitizenJobType, Integer> jobCounts = new EnumMap<>(CitizenJobType.class);
        jobCounts.put(CitizenJobType.IDLE, idle);
        jobCounts.put(CitizenJobType.GATHERER, gatherers);
        jobCounts.put(CitizenJobType.HUNTER, hunter);
        jobCounts.put(CitizenJobType.COOK, cook);
        jobCounts.put(CitizenJobType.MINER, miner);
        jobCounts.put(CitizenJobType.BLACKSMITH, blacksmith);
        jobCounts.put(CitizenJobType.ARCHITECT, architect);
        jobCounts.put(CitizenJobType.GRUNT_BUILDER, gruntBuilder);
        jobCounts.put(CitizenJobType.BUILDER, 0);
        jobCounts.put(CitizenJobType.TRAINEE, trainees);
        jobCounts.put(CitizenJobType.SOLDIER, 0);

        EnumMap<ResourceType, Integer> workers = allocateWorkers(state, jobCounts);
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
                + " | Hunters "
                + snapshot.jobCount(CitizenJobType.HUNTER)
                + " | Miners "
                + snapshot.jobCount(CitizenJobType.MINER)
                + " | Builders "
                + builderCount(snapshot)
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
                + " workers/troops routed | +"
                + snapshot.gainFor(resourceType)
                + " per tick";
    }

    private EnumMap<ResourceType, Integer> allocateWorkers(PlayerGameState state, Map<CitizenJobType, Integer> jobCounts) {
        EnumMap<ResourceType, Integer> workers = allocateGatherers(state, jobCounts.getOrDefault(CitizenJobType.GATHERER, 0));
        workers.put(ResourceType.FOOD, workers.get(ResourceType.FOOD)
                + jobCounts.getOrDefault(CitizenJobType.HUNTER, 0)
                + jobCounts.getOrDefault(CitizenJobType.COOK, 0));
        workers.put(ResourceType.WOOD, workers.get(ResourceType.WOOD)
                + jobCounts.getOrDefault(CitizenJobType.GRUNT_BUILDER, 0));
        workers.put(ResourceType.IRON, workers.get(ResourceType.IRON)
                + jobCounts.getOrDefault(CitizenJobType.MINER, 0)
                + jobCounts.getOrDefault(CitizenJobType.BLACKSMITH, 0));
        return workers;
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

    private int take(int available, int requested) {
        return Math.max(0, Math.min(available, requested));
    }

    private int builderCount(CastleEconomySnapshot snapshot) {
        return snapshot.jobCount(CitizenJobType.ARCHITECT)
                + snapshot.jobCount(CitizenJobType.GRUNT_BUILDER)
                + snapshot.jobCount(CitizenJobType.BLACKSMITH)
                + snapshot.jobCount(CitizenJobType.BUILDER);
    }
}
