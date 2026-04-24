package com.tavall.hytale.resourcegame.domain;

import java.util.Objects;

/**
 * Aggregated view of the player population.
 */
public final class PopulationSummary {
    private final int citizenCount;
    private final int troopCount;
    private final CitizenMetaData citizenMetaData;
    private final TroopMetaData troopMetaData;
    private final AgingState agingState;

    public PopulationSummary(
            int citizenCount,
            int troopCount,
            CitizenMetaData citizenMetaData,
            TroopMetaData troopMetaData,
            AgingState agingState
    ) {
        this.citizenCount = citizenCount;
        this.troopCount = troopCount;
        this.citizenMetaData = Objects.requireNonNull(citizenMetaData, "citizenMetaData");
        this.troopMetaData = Objects.requireNonNull(troopMetaData, "troopMetaData");
        this.agingState = Objects.requireNonNull(agingState, "agingState");
    }

    public int citizenCount() {
        return citizenCount;
    }

    public int troopCount() {
        return troopCount;
    }

    public CitizenMetaData citizenMetaData() {
        return citizenMetaData;
    }

    public TroopMetaData troopMetaData() {
        return troopMetaData;
    }

    /**
     * Total military power represented by this aggregate population.
     */
    public int might() {
        return troopMetaData.estimatedMight(troopCount);
    }

    public AgingState agingState() {
        return agingState;
    }

    public PopulationSummary withCitizenCount(int citizenCount) {
        return new PopulationSummary(citizenCount, troopCount, citizenMetaData, troopMetaData, agingState);
    }

    public PopulationSummary withTroopCount(int troopCount) {
        return new PopulationSummary(citizenCount, troopCount, citizenMetaData, troopMetaData, agingState);
    }

    public PopulationSummary withAgingState(AgingState agingState) {
        return new PopulationSummary(citizenCount, troopCount, citizenMetaData, troopMetaData, agingState);
    }
}
