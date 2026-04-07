package org.tavall.hytale.resourcegame.service;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;
import org.tavall.hytale.resourcegame.domain.population.CitizenJob;
import org.tavall.hytale.resourcegame.domain.population.CitizenUnitProfile;
import org.tavall.hytale.resourcegame.domain.population.PopulationRole;
import org.tavall.hytale.resourcegame.domain.population.PopulationSummary;
import org.tavall.hytale.resourcegame.domain.resource.ResourceType;
import org.tavall.hytale.resourcegame.service.model.PopulationShiftResult;

/**
 * Applies citizen/troop continuum transitions and validates resource gating.
 */
public class PopulationContinuumService {

  private final Map<ResourceType, Integer> troopPromotionCost = new EnumMap<>(ResourceType.class);

  public PopulationContinuumService() {
    troopPromotionCost.put(ResourceType.FOOD, 2);
    troopPromotionCost.put(ResourceType.WOOD, 1);
    troopPromotionCost.put(ResourceType.IRON, 1);
  }

  public PopulationShiftResult promoteToTroop(PlayerStateBundle bundle, int amount) {
    if (amount <= 0) {
      return new PopulationShiftResult(false, "Amount must be positive");
    }
    if (bundle.gameState().citizenCount() < amount) {
      return new PopulationShiftResult(false, "Not enough citizens");
    }
    for (Map.Entry<ResourceType, Integer> cost : troopPromotionCost.entrySet()) {
      int required = cost.getValue() * amount;
      if (!bundle.gameState().resources().hasAtLeast(cost.getKey(), required)) {
        return new PopulationShiftResult(false, "Missing " + cost.getKey().name().toLowerCase());
      }
    }
    for (Map.Entry<ResourceType, Integer> cost : troopPromotionCost.entrySet()) {
      bundle.gameState().resources().add(cost.getKey(), -cost.getValue() * amount);
    }
    PopulationSummary before = bundle.gameState().populationSummary();
    PopulationSummary after = new PopulationSummary(before.citizens() - amount, before.troops() + amount);
    bundle.populationRoster().syncToTarget(after, Instant.now());
    bundle.gameState().assignPopulation(after, Instant.now());
    return new PopulationShiftResult(true, "Promoted " + amount + " citizens to troops");
  }

  public PopulationShiftResult demoteToCitizen(PlayerStateBundle bundle, int amount) {
    if (amount <= 0) {
      return new PopulationShiftResult(false, "Amount must be positive");
    }
    if (bundle.gameState().troopCount() < amount) {
      return new PopulationShiftResult(false, "Not enough troops");
    }
    PopulationSummary before = bundle.gameState().populationSummary();
    PopulationSummary after = new PopulationSummary(before.citizens() + amount, before.troops() - amount);
    bundle.populationRoster().syncToTarget(after, Instant.now());
    bundle.gameState().assignPopulation(after, Instant.now());
    return new PopulationShiftResult(true, "Demoted " + amount + " troops to citizens");
  }

  public void setCitizens(PlayerStateBundle bundle, int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("citizens cannot be negative");
    }
    PopulationSummary before = bundle.gameState().populationSummary();
    PopulationSummary next = new PopulationSummary(amount, before.troops());
    bundle.populationRoster().syncToTarget(next, Instant.now());
    bundle.gameState().assignPopulation(next, Instant.now());
  }

  public void setTroops(PlayerStateBundle bundle, int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("troops cannot be negative");
    }
    PopulationSummary before = bundle.gameState().populationSummary();
    PopulationSummary next = new PopulationSummary(before.citizens(), amount);
    bundle.populationRoster().syncToTarget(next, Instant.now());
    bundle.gameState().assignPopulation(next, Instant.now());
  }

  public void addCitizens(PlayerStateBundle bundle, int amount) {
    setCitizens(bundle, bundle.gameState().citizenCount() + amount);
  }

  public void addTroops(PlayerStateBundle bundle, int amount) {
    setTroops(bundle, bundle.gameState().troopCount() + amount);
  }

  public Map<ResourceType, Integer> troopPromotionCost() {
    return Map.copyOf(troopPromotionCost);
  }

  public void setJobForRole(PlayerStateBundle bundle, PopulationRole role, CitizenJob job) {
    for (CitizenUnitProfile unit : bundle.populationRoster().allUnits()) {
      if (unit.role() == role) {
        unit.assignJob(job);
      }
    }
  }
}
