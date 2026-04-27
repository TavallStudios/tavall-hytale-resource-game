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
    private final AccountProgression accountProgression;
    private final DebugModeState debugModeState;
    private final List<ResourceNodeData> resourceNodes;
    private final List<CastleBuildingData> castleBuildings;
    private final int interiorInstanceIndex;

    public GameStateMetadata(
            CitizenMetaData citizenMetaData,
            TroopMetaData troopMetaData,
            AgingState agingState,
            Map<CitizenJobType, Integer> jobCounts,
            OnboardingProgress onboardingProgress,
            AccountProgression accountProgression,
            DebugModeState debugModeState,
            List<ResourceNodeData> resourceNodes,
            List<CastleBuildingData> castleBuildings,
            Integer interiorInstanceIndex
    ) {
        this.citizenMetaData = citizenMetaData;
        this.troopMetaData = troopMetaData;
        this.agingState = agingState;
        this.jobCounts = jobCounts == null ? Map.of() : Map.copyOf(jobCounts);
        this.onboardingProgress = onboardingProgress == null ? OnboardingProgress.defaults() : onboardingProgress;
        this.accountProgression = accountProgression == null ? AccountProgression.defaults() : accountProgression;
        this.debugModeState = debugModeState == null ? DebugModeState.disabled() : debugModeState;
        this.resourceNodes = resourceNodes == null ? List.of() : List.copyOf(resourceNodes);
        this.castleBuildings = castleBuildings == null ? List.of() : List.copyOf(castleBuildings);
        this.interiorInstanceIndex = interiorInstanceIndex == null ? 0 : Math.max(0, interiorInstanceIndex);
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

    public AccountProgression accountProgression() {
        return accountProgression;
    }

    public DebugModeState debugModeState() {
        return debugModeState;
    }

    public List<ResourceNodeData> resourceNodes() {
        return resourceNodes;
    }

    public List<CastleBuildingData> castleBuildings() {
        return castleBuildings;
    }

    public int interiorInstanceIndex() {
        return interiorInstanceIndex;
    }

    public static GameStateMetadata fromPopulation(PopulationSummary populationSummary) {
        return fromPopulation(populationSummary, OnboardingProgress.defaults(), List.of(), List.of(), 0);
    }

    public static GameStateMetadata fromPopulation(PopulationSummary populationSummary, OnboardingProgress onboardingProgress) {
        return fromPopulation(populationSummary, onboardingProgress, List.of(), List.of(), 0);
    }

    public static GameStateMetadata fromPopulation(
            PopulationSummary populationSummary,
            OnboardingProgress onboardingProgress,
            List<ResourceNodeData> resourceNodes
    ) {
        return fromPopulation(populationSummary, onboardingProgress, resourceNodes, List.of(), 0);
    }

    public static GameStateMetadata fromPopulation(
            PopulationSummary populationSummary,
            OnboardingProgress onboardingProgress,
            List<ResourceNodeData> resourceNodes,
            List<CastleBuildingData> castleBuildings
    ) {
        return fromPopulation(populationSummary, onboardingProgress, resourceNodes, castleBuildings, 0);
    }

    public static GameStateMetadata fromPopulation(
            PopulationSummary populationSummary,
            OnboardingProgress onboardingProgress,
            List<ResourceNodeData> resourceNodes,
            List<CastleBuildingData> castleBuildings,
            int interiorInstanceIndex
    ) {
        return fromPopulation(populationSummary, onboardingProgress, AccountProgression.defaults(), DebugModeState.disabled(), resourceNodes, castleBuildings, interiorInstanceIndex);
    }

    public static GameStateMetadata fromPopulation(
            PopulationSummary populationSummary,
            OnboardingProgress onboardingProgress,
            AccountProgression accountProgression,
            List<ResourceNodeData> resourceNodes,
            List<CastleBuildingData> castleBuildings,
            int interiorInstanceIndex
    ) {
        return fromPopulation(populationSummary, onboardingProgress, accountProgression, DebugModeState.disabled(), resourceNodes, castleBuildings, interiorInstanceIndex);
    }

    public static GameStateMetadata fromPopulation(
            PopulationSummary populationSummary,
            OnboardingProgress onboardingProgress,
            AccountProgression accountProgression,
            DebugModeState debugModeState,
            List<ResourceNodeData> resourceNodes,
            List<CastleBuildingData> castleBuildings,
            int interiorInstanceIndex
    ) {
        return new GameStateMetadata(
                populationSummary.citizenMetaData(),
                populationSummary.troopMetaData(),
                populationSummary.agingState(),
                populationSummary.citizenMetaData().jobCounts(),
                onboardingProgress,
                accountProgression,
                debugModeState,
                resourceNodes,
                castleBuildings,
                interiorInstanceIndex
        );
    }
}
