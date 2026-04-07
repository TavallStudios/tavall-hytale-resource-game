package org.tavall.hytale.resourcegame.domain.player;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

/**
 * Core identity profile persisted in Postgres and reused by game state layers.
 */
public class PlayerProfile {

  private long internalPlayerId;
  private final UUID playerUuid;
  private String playerName;
  private ZoneId timezone;
  private String transformedIp;
  private final Instant createdAt;
  private Instant updatedAt;
  private Instant lastSeenAt;

  public PlayerProfile(
      long internalPlayerId,
      UUID playerUuid,
      String playerName,
      ZoneId timezone,
      String transformedIp,
      Instant createdAt,
      Instant updatedAt,
      Instant lastSeenAt
  ) {
    this.internalPlayerId = internalPlayerId;
    this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
    this.playerName = Objects.requireNonNull(playerName, "playerName");
    this.timezone = Objects.requireNonNull(timezone, "timezone");
    this.transformedIp = Objects.requireNonNull(transformedIp, "transformedIp");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    this.lastSeenAt = Objects.requireNonNull(lastSeenAt, "lastSeenAt");
  }

  public long internalPlayerId() {
    return internalPlayerId;
  }

  public UUID playerUuid() {
    return playerUuid;
  }

  public String playerName() {
    return playerName;
  }

  public ZoneId timezone() {
    return timezone;
  }

  public String transformedIp() {
    return transformedIp;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public Instant lastSeenAt() {
    return lastSeenAt;
  }

  public void assignInternalPlayerId(long nextInternalPlayerId) {
    this.internalPlayerId = nextInternalPlayerId;
  }

  public void refreshIdentity(String nextPlayerName, ZoneId nextTimezone, String nextTransformedIp, Instant eventTime) {
    this.playerName = Objects.requireNonNull(nextPlayerName, "nextPlayerName");
    this.timezone = Objects.requireNonNull(nextTimezone, "nextTimezone");
    this.transformedIp = Objects.requireNonNull(nextTransformedIp, "nextTransformedIp");
    this.updatedAt = eventTime;
    this.lastSeenAt = eventTime;
  }
}
