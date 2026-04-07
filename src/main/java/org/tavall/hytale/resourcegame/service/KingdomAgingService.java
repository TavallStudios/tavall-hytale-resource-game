package org.tavall.hytale.resourcegame.service;

import java.time.Duration;
import java.time.Instant;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;
import org.tavall.hytale.resourcegame.domain.population.CitizenUnitProfile;

/**
 * Aging scaffold used to track elapsed real-world time for population units.
 */
public class KingdomAgingService {

  public void markAgingTick(PlayerStateBundle bundle, Instant now) {
    bundle.gameState().agingProfile().markAgingEvaluation(now);
  }

  public Duration oldestUnitAge(PlayerStateBundle bundle, Instant now) {
    Duration oldest = Duration.ZERO;
    for (CitizenUnitProfile unit : bundle.populationRoster().allUnits()) {
      Duration unitAge = unit.ageAt(now);
      if (unitAge.compareTo(oldest) > 0) {
        oldest = unitAge;
      }
    }
    return oldest;
  }
}
