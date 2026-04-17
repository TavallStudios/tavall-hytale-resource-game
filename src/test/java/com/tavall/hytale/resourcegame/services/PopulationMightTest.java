package com.tavall.hytale.resourcegame.services;

import com.tavall.hytale.resourcegame.domain.AgingState;
import com.tavall.hytale.resourcegame.domain.CitizenMetaData;
import com.tavall.hytale.resourcegame.domain.PopulationSummary;
import com.tavall.hytale.resourcegame.domain.TroopMetaData;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class PopulationMightTest {
    @Test
    void mightUsesTierOneAggregateUntilTierCountsArePersisted() {
        PopulationSummary summary = new PopulationSummary(
                12,
                7,
                CitizenMetaData.defaults(),
                TroopMetaData.defaults(),
                AgingState.defaults(Instant.parse("2026-04-16T00:00:00Z"))
        );

        assertEquals(7, summary.might());
    }

    @Test
    void mightNeverDropsBelowZeroForMalformedAggregateCounts() {
        assertEquals(0, TroopMetaData.defaults().estimatedMight(-5));
    }
}
