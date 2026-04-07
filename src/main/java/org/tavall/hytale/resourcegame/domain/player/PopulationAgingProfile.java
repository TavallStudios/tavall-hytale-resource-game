package org.tavall.hytale.resourcegame.domain.player;

import java.time.Duration;
import java.time.Instant;

/**
 * Aging timing metadata. Final tuning remains intentionally deferred to TODO planning.
 */
public class PopulationAgingProfile {

  private Duration unresolvedCadence;
  private Instant lastAgingEvaluation;

  public PopulationAgingProfile(Duration unresolvedCadence, Instant lastAgingEvaluation) {
    this.unresolvedCadence = unresolvedCadence;
    this.lastAgingEvaluation = lastAgingEvaluation;
  }

  public Duration unresolvedCadence() {
    return unresolvedCadence;
  }

  public Instant lastAgingEvaluation() {
    return lastAgingEvaluation;
  }

  public void setUnresolvedCadence(Duration nextCadence) {
    this.unresolvedCadence = nextCadence;
  }

  public void markAgingEvaluation(Instant evaluatedAt) {
    this.lastAgingEvaluation = evaluatedAt;
  }
}
