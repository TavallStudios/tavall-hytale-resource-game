package com.tavall.hytale.resourcegame.services;

import com.tavall.hytale.resourcegame.domain.AgingState;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.PopulationSummary;
import com.tavall.hytale.resourcegame.domain.ResourceInventory;
import com.tavall.hytale.resourcegame.persistence.PopulationSummaryDefaults;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class CastleSiteScenePlannerTest {
    @Test
    void plannerCapsStorageWorkersAndConvoysToReadableCounts() {
        CastleSiteScenePlanner planner = new CastleSiteScenePlanner();

        assertEquals(0, planner.visibleStorageCount(0));
        assertEquals(1, planner.visibleStorageCount(20));
        assertEquals(6, planner.visibleStorageCount(220));

        assertEquals(0, planner.visibleWorkerCount(0));
        assertEquals(1, planner.visibleWorkerCount(1));
        assertEquals(6, planner.visibleWorkerCount(12));

        assertEquals(0, planner.visibleConvoyCount(0, 5));
        assertEquals(0, planner.visibleConvoyCount(2, 0));
        assertEquals(1, planner.visibleConvoyCount(2, 2));
        assertEquals(4, planner.visibleConvoyCount(9, 12));
    }

    @Test
    void plannerBuildsCompactStockpileLabel() {
        CastleSiteScenePlanner planner = new CastleSiteScenePlanner();

        String label = planner.stockpileLabel(state(47, 36, 18));

        assertEquals("Stockpile | Food 47 | Wood 36 | Iron 18", label);
    }

    private PlayerGameState state(int food, int wood, int iron) {
        Instant now = Instant.parse("2026-04-15T17:00:00Z");
        return new PlayerGameState(
                7L,
                8L,
                UUID.randomUUID(),
                new CastleLocationData("default", 0.0, 65.0, 0.0),
                new PopulationSummary(
                        12,
                        2,
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
