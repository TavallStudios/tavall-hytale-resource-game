package com.tavall.hytale.resourcegame.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Aggregated metadata describing citizen capabilities and job distribution.
 */
public final class CitizenMetaData {
    private final double productivityMedian;
    private final double battleReadinessMedian;
    private final double moraleMedian;
    private final Map<CitizenJobType, Integer> jobCounts;

    public CitizenMetaData(
            double productivityMedian,
            double battleReadinessMedian,
            double moraleMedian,
            Map<CitizenJobType, Integer> jobCounts) {
        this.productivityMedian = productivityMedian;
        this.battleReadinessMedian = battleReadinessMedian;
        this.moraleMedian = moraleMedian;
        EnumMap<CitizenJobType, Integer> copy = new EnumMap<>(CitizenJobType.class);
        if (jobCounts != null) {
            copy.putAll(jobCounts);
        }
        this.jobCounts = Map.copyOf(copy);
    }

    public double productivityMedian() {
        return productivityMedian;
    }

    public double battleReadinessMedian() {
        return battleReadinessMedian;
    }

    public double moraleMedian() {
        return moraleMedian;
    }

    public Map<CitizenJobType, Integer> jobCounts() {
        return jobCounts;
    }

    public CitizenMetaData withJobCounts(Map<CitizenJobType, Integer> jobCounts) {
        return new CitizenMetaData(productivityMedian, battleReadinessMedian, moraleMedian, jobCounts);
    }

    public static CitizenMetaData defaults() {
        return new CitizenMetaData(0.5, 0.2, 0.6, Map.of(CitizenJobType.IDLE, 0));
    }
}