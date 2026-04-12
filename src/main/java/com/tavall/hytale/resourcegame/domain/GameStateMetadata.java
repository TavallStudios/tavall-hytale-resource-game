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
    private final OnboardingProgress onboardingProgress;

    public GameStateMetadata(
            CitizenMetaData citizenMetaData,
            TroopMetaData troopMetaData,
            AgingState agingState,
            Map<CitizenJobType, Integer> jobCounts,
            OnboardingProgress onboardingProgress
    ) {
        this.citizenMetaData = citizenMetaData;
        this.troopMetaData = troopMetaData;
        this.agingState = agingState;
        this.jobCounts = jobCounts == null ? Map.of() : Map.copyOf(jobCounts);
        this.onboardingProgress = onboardingProgress == null ? OnboardingProgress.defaults() : onboardingProgress;
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

    public OnboardingProgress onboardingProgress() {
        return onboardingProgress;
    }

    public static GameStateMetadata fromPopulation(PopulationSummary populationSummary) {
        return fromPopulation(populationSummary, OnboardingProgress.defaults());
    }

    public static GameStateMetadata fromPopulation(PopulationSummary populationSummary, OnboardingProgress onboardingProgress) {
        return new GameStateMetadata(
                populationSummary.citizenMetaData(),
                populationSummary.troopMetaData(),
                populationSummary.agingState(),
                populationSummary.citizenMetaData().jobCounts(),
                onboardingProgress
        );
    }
}
