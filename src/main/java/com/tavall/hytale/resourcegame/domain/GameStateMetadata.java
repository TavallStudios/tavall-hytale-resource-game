package com.tavall.hytale.resourcegame.domain;

import java.util.Map;

/**
 * JSON-friendly snapshot of population metadata.
 */
public final class GameStateMetadata {
    private final CitizenMetaData citizenMetaData;
    private final TroopMetaData troopMetaData;
    private final AgingState agingState;
    private final Map<CitizenJobType, Integer> jobCounts;

    public GameStateMetadata(
            CitizenMetaData citizenMetaData,
            TroopMetaData troopMetaData,
            AgingState agingState,
            Map<CitizenJobType, Integer> jobCounts
    ) {
        this.citizenMetaData = citizenMetaData;
        this.troopMetaData = troopMetaData;
        this.agingState = agingState;
        this.jobCounts = jobCounts;
    }

    public CitizenMetaData citizenMetaData() {
        return citizenMetaData;
    }

    public TroopMetaData troopMetaData() {
        return troopMetaData;
    }

    public AgingState agingState() {
        return agingState;
    }

    public Map<CitizenJobType, Integer> jobCounts() {
        return jobCounts;
    }

    public static GameStateMetadata fromPopulation(PopulationSummary populationSummary) {
        return new GameStateMetadata(
                populationSummary.citizenMetaData(),
                populationSummary.troopMetaData(),
                populationSummary.agingState(),
                populationSummary.citizenMetaData().jobCounts()
        );
    }
}
