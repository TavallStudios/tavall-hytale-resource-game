package org.tavall.hytale.resourcegame.domain.player;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.tavall.hytale.resourcegame.domain.castle.CastleLocation;
import org.tavall.hytale.resourcegame.domain.population.PopulationSummary;
import org.tavall.hytale.resourcegame.domain.resource.ResourceInventory;

/**
 * Durable per-player kingdom state separate from identity profile.
 */
public class PlayerGameState {

  private long profileId;
  private UUID castleId;
  private CastleLocation castleLocation;
  private int citizenCount;
  private int troopCount;
  private final ResourceInventory resources;
  private String currentInteriorWorldId;
  private final Instant createdAt;
  private Instant updatedAt;
  private final PopulationAgingProfile agingProfile;
  private String populationMetadataJson;

  public PlayerGameState(
      long profileId,
      UUID castleId,
      CastleLocation castleLocation,
      int citizenCount,
      int troopCount,
      ResourceInventory resources,
      String currentInteriorWorldId,
      Instant createdAt,
      Instant updatedAt,
      PopulationAgingProfile agingProfile,
      String populationMetadataJson
  ) {
    this.profileId = profileId;
    this.castleId = castleId;
    this.castleLocation = castleLocation;
    this.citizenCount = citizenCount;
    this.troopCount = troopCount;
    this.resources = Objects.requireNonNull(resources, "resources");
    this.currentInteriorWorldId = currentInteriorWorldId;
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    this.agingProfile = Objects.requireNonNull(agingProfile, "agingProfile");
    this.populationMetadataJson = populationMetadataJson;
  }

  public long profileId() {
    return profileId;
  }

  public void assignProfileId(long nextProfileId) {
    this.profileId = nextProfileId;
  }

  public UUID castleId() {
    return castleId;
  }

  public CastleLocation castleLocation() {
    return castleLocation;
  }

  public int citizenCount() {
    return citizenCount;
  }

  public int troopCount() {
    return troopCount;
  }

  public ResourceInventory resources() {
    return resources;
  }

  public String currentInteriorWorldId() {
    return currentInteriorWorldId;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public PopulationAgingProfile agingProfile() {
    return agingProfile;
  }

  public String populationMetadataJson() {
    return populationMetadataJson;
  }

  public PopulationSummary populationSummary() {
    return new PopulationSummary(citizenCount, troopCount);
  }

  public void assignCastle(UUID nextCastleId, CastleLocation nextCastleLocation, Instant eventTime) {
    this.castleId = Objects.requireNonNull(nextCastleId, "nextCastleId");
    this.castleLocation = Objects.requireNonNull(nextCastleLocation, "nextCastleLocation");
    this.updatedAt = eventTime;
  }

  public void assignPopulation(PopulationSummary nextSummary, Instant eventTime) {
    this.citizenCount = nextSummary.citizens();
    this.troopCount = nextSummary.troops();
    this.updatedAt = eventTime;
  }

  public void assignInteriorWorldId(String nextInteriorWorldId, Instant eventTime) {
    this.currentInteriorWorldId = nextInteriorWorldId;
    this.updatedAt = eventTime;
  }

  public void assignPopulationMetadataJson(String nextPopulationMetadataJson, Instant eventTime) {
    this.populationMetadataJson = nextPopulationMetadataJson;
    this.updatedAt = eventTime;
  }

  public void touchUpdatedAt(Instant eventTime) {
    this.updatedAt = eventTime;
  }
}
