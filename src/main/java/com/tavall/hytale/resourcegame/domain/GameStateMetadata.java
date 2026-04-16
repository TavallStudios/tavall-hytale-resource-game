package com.tavall.hytale.resourcegame.domain;

import java.util.Map;
import java.util.List;

/**
 * JSON-friendly snapshot of population metadata.
 */
public final class GameStateMetadata {
    private final CitizenMetaData citizenMetaData;
    private final TroopMetaData troopMetaData;
    private final AgingState agingState;
    private final Map<CitizenJobType, Integer> jobCounts;
    private final OnboardingProgress onboardingProgress;
    private final List<ResourceNodeData> resourceNodes;
    private final List<CastleBuildingData> castleBuildings;

    public GameStateMetadata(
            CitizenMetaData citizenMetaData,
            TroopMetaData troopMetaData,
            AgingState agingState,
            Map<CitizenJobType, Integer> jobCounts,
            OnboardingProgress onboardingProgress,
            List<ResourceNodeData> resourceNodes,
            List<CastleBuildingData> castleBuildings
    ) {
        this.citizenMetaData = citizenMetaData;
        this.troopMetaData = troopMetaData;
        this.agingState = agingState;
        this.jobCounts = jobCounts == null ? Map.of() : Map.copyOf(jobCounts);
        this.onboardingProgress = onboardingProgress == null ? OnboardingProgress.defaults() : onboardingProgress;
        this.resourceNodes = resourceNodes == null ? List.of() : List.copyOf(resourceNodes);
        this.castleBuildings = castleBuildings == null ? List.of() : List.copyOf(castleBuildings);
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

    public List<ResourceNodeData> resourceNodes() {
        return resourceNodes;
    }

    public List<CastleBuildingData> castleBuildings() {
        return castleBuildings;
    }

    public static GameStateMetadata fromPopulation(PopulationSummary populationSummary) {
        return fromPopulation(populationSummary, OnboardingProgress.defaults(), List.of(), List.of());
    }

    public static GameStateMetadata fromPopulation(PopulationSummary populationSummary, OnboardingProgress onboardingProgress) {
        return fromPopulation(populationSummary, onboardingProgress, List.of(), List.of());
    }

    public static GameStateMetadata fromPopulation(
            PopulationSummary populationSummary,
            OnboardingProgress onboardingProgress,
            List<ResourceNodeData> resourceNodes
    ) {
        return fromPopulation(populationSummary, onboardingProgress, resourceNodes, List.of());
    }

    public static GameStateMetadata fromPopulation(
            PopulationSummary populationSummary,
            OnboardingProgress onboardingProgress,
            List<ResourceNodeData> resourceNodes,
            List<CastleBuildingData> castleBuildings
    ) {
        return new GameStateMetadata(
                populationSummary.citizenMetaData(),
                populationSummary.troopMetaData(),
                populationSummary.agingState(),
                populationSummary.citizenMetaData().jobCounts(),
                onboardingProgress,
                resourceNodes,
                castleBuildings
        );
    }
}
