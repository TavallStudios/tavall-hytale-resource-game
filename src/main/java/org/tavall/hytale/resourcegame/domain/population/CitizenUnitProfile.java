package org.tavall.hytale.resourcegame.domain.population;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Base unit metadata for the citizen-to-troop continuum.
 */
public class CitizenUnitProfile {

  private final UUID unitId;
  private PopulationRole role;
  private CitizenJob job;
  private final CitizenAttributes attributes;
  private final Instant birthAt;
  private Instant lastRoleShiftAt;

  public CitizenUnitProfile(
      UUID unitId,
      PopulationRole role,
      CitizenJob job,
      CitizenAttributes attributes,
      Instant birthAt,
      Instant lastRoleShiftAt
  ) {
    this.unitId = Objects.requireNonNull(unitId, "unitId");
    this.role = Objects.requireNonNull(role, "role");
    this.job = Objects.requireNonNull(job, "job");
    this.attributes = Objects.requireNonNull(attributes, "attributes");
    this.birthAt = Objects.requireNonNull(birthAt, "birthAt");
    this.lastRoleShiftAt = Objects.requireNonNull(lastRoleShiftAt, "lastRoleShiftAt");
  }

  public UUID unitId() {
    return unitId;
  }

  public PopulationRole role() {
    return role;
  }

  public CitizenJob job() {
    return job;
  }

  public CitizenAttributes attributes() {
    return attributes;
  }

  public Instant birthAt() {
    return birthAt;
  }

  public Instant lastRoleShiftAt() {
    return lastRoleShiftAt;
  }

  public Duration ageAt(Instant now) {
    return Duration.between(birthAt, now);
  }

  public void assignRole(PopulationRole nextRole, Instant changedAt) {
    this.role = Objects.requireNonNull(nextRole, "nextRole");
    this.lastRoleShiftAt = Objects.requireNonNull(changedAt, "changedAt");
    if (nextRole == PopulationRole.TROOP && job == CitizenJob.IDLE) {
      this.job = CitizenJob.SOLDIER;
    }
  }

  public void assignJob(CitizenJob nextJob) {
    this.job = Objects.requireNonNull(nextJob, "nextJob");
  }
}
