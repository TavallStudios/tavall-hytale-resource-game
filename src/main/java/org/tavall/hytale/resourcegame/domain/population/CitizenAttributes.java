package org.tavall.hytale.resourcegame.domain.population;

/**
 * Starter stat shape for continuum units that can be expanded later without schema churn.
 */
public record CitizenAttributes(int strength, int discipline, int craft, int morale) {
}
