package com.tavall.hytale.resourcegame.services;

import com.tavall.hytale.resourcegame.domain.AgingState;
import com.tavall.hytale.resourcegame.domain.CastleEconomySnapshot;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.CitizenJobType;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.PopulationSummary;
import com.tavall.hytale.resourcegame.domain.ResourceInventory;
import com.tavall.hytale.resourcegame.persistence.PopulationSummaryDefaults;
import com.tavall.hytale.resourcegame.resources.ResourceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CastleEconomyPlannerTest {
    @Test
    void snapshotDistributesAllCitizensAcrossJobs() {
        CastleEconomyPlanner planner = new CastleEconomyPlanner();

        CastleEconomySnapshot snapshot = planner.snapshot(state(12, 1, 20, 20, 20));

        int totalAssigned = snapshot.jobCounts().values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(12, totalAssigned);
        assertTrue(snapshot.jobCount(CitizenJobType.HUNTER) >= 1);
        assertTrue(snapshot.jobCount(CitizenJobType.COOK) >= 1);
        assertTrue(snapshot.jobCount(CitizenJobType.MINER) >= 1);
        assertTrue(snapshot.jobCount(CitizenJobType.GRUNT_BUILDER) >= 1);
    }

    @Test
    void snapshotAllocatesGatherersAcrossAllStarterNodes() {
        CastleEconomyPlanner planner = new CastleEconomyPlanner();

        CastleEconomySnapshot snapshot = planner.snapshot(state(12, 0, 0, 0, 0));

        int resourceWorkers = snapshot.jobCount(CitizenJobType.GATHERER)
                + snapshot.jobCount(CitizenJobType.HUNTER)
                + snapshot.jobCount(CitizenJobType.COOK)
                + snapshot.jobCount(CitizenJobType.MINER)
                + snapshot.jobCount(CitizenJobType.BLACKSMITH)
                + snapshot.jobCount(CitizenJobType.GRUNT_BUILDER);
        int allocated = snapshot.workersFor(ResourceType.FOOD)
                + snapshot.workersFor(ResourceType.WOOD)
                + snapshot.workersFor(ResourceType.IRON);
        assertEquals(resourceWorkers, allocated);
        assertTrue(snapshot.workersFor(ResourceType.WOOD) >= 1);
        assertTrue(snapshot.workersFor(ResourceType.IRON) >= 1);
        assertTrue(snapshot.workersFor(ResourceType.FOOD) >= 1);
    }

    @Test
    void snapshotReportsPerTickGainFromWorkers() {
        CastleEconomyPlanner planner = new CastleEconomyPlanner();

        CastleEconomySnapshot snapshot = planner.snapshot(state(9, 3, 5, 40, 2));

        assertEquals(snapshot.workersFor(ResourceType.FOOD) * 3, snapshot.gainFor(ResourceType.FOOD));
        assertEquals(snapshot.workersFor(ResourceType.WOOD) * 2, snapshot.gainFor(ResourceType.WOOD));
        assertEquals(snapshot.workersFor(ResourceType.IRON), snapshot.gainFor(ResourceType.IRON));
    }

    private PlayerGameState state(int citizens, int troops, int food, int wood, int iron) {
        Instant now = Instant.parse("2026-04-14T18:00:00Z");
        return new PlayerGameState(
                1L,
                2L,
                UUID.randomUUID(),
                new CastleLocationData("overworld", 0.0, 70.0, 0.0),
                new PopulationSummary(
                        citizens,
                        troops,
                        PopulationSummaryDefaults.citizenMetaData(),
                        PopulationSummaryDefaults.troopMetaData(),
                        AgingState.defaults(now)
                ),
                new ResourceInventory(food, wood, iron),
                null,
                null,
                now,
                now
        );
    }
}
