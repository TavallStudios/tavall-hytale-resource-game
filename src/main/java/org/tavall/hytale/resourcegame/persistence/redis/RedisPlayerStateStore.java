package org.tavall.hytale.resourcegame.persistence.redis;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import org.tavall.hytale.resourcegame.domain.castle.CastleLocation;
import org.tavall.hytale.resourcegame.domain.player.PlayerGameState;
import org.tavall.hytale.resourcegame.domain.player.PlayerProfile;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;
import org.tavall.hytale.resourcegame.domain.player.PopulationAgingProfile;
import org.tavall.hytale.resourcegame.domain.population.PopulationRoster;
import org.tavall.hytale.resourcegame.domain.population.PopulationSummary;
import org.tavall.hytale.resourcegame.domain.resource.ResourceInventory;
import org.tavall.hytale.resourcegame.domain.resource.ResourceType;
import org.tavall.hytale.resourcegame.persistence.PlayerStateStore;

/**
 * Redis fast-access layer for hot player state lookups before durable database reads.
 */
public class RedisPlayerStateStore implements PlayerStateStore {

  private static final Duration CACHE_TTL = Duration.ofMinutes(20);

  private final RedisKeyValueStore redisKeyValueStore;

  public RedisPlayerStateStore(RedisKeyValueStore redisKeyValueStore) {
    this.redisKeyValueStore = redisKeyValueStore;
  }

  @Override
  public Optional<PlayerStateBundle> load(UUID playerId) {
    return redisKeyValueStore.get(key(playerId)).map(this::decode);
  }

  @Override
  public PlayerStateBundle save(PlayerStateBundle bundle) {
    redisKeyValueStore.set(key(bundle.playerId()), encode(bundle), CACHE_TTL);
    return bundle;
  }

  private String key(UUID playerId) {
    return "kingdom:player:" + playerId;
  }

  private String encode(PlayerStateBundle bundle) {
    Properties properties = new Properties();
    properties.setProperty("playerId", bundle.playerId().toString());
    properties.setProperty("profileId", String.valueOf(bundle.profile().internalPlayerId()));
    properties.setProperty("playerUuid", bundle.profile().playerUuid().toString());
    properties.setProperty("playerName", bundle.profile().playerName());
    properties.setProperty("timezone", bundle.profile().timezone().getId());
    properties.setProperty("transformedIp", bundle.profile().transformedIp());
    properties.setProperty("profileCreatedAt", bundle.profile().createdAt().toString());
    properties.setProperty("profileUpdatedAt", bundle.profile().updatedAt().toString());
    properties.setProperty("profileLastSeenAt", bundle.profile().lastSeenAt().toString());
    if (bundle.gameState().castleId() != null) {
      properties.setProperty("castleId", bundle.gameState().castleId().toString());
    }
    if (bundle.gameState().castleLocation() != null) {
      properties.setProperty("castleWorld", bundle.gameState().castleLocation().worldId());
      properties.setProperty("castleX", String.valueOf(bundle.gameState().castleLocation().x()));
      properties.setProperty("castleY", String.valueOf(bundle.gameState().castleLocation().y()));
      properties.setProperty("castleZ", String.valueOf(bundle.gameState().castleLocation().z()));
    }
    properties.setProperty("citizenCount", String.valueOf(bundle.gameState().citizenCount()));
    properties.setProperty("troopCount", String.valueOf(bundle.gameState().troopCount()));
    properties.setProperty("food", String.valueOf(bundle.gameState().resources().get(ResourceType.FOOD)));
    properties.setProperty("wood", String.valueOf(bundle.gameState().resources().get(ResourceType.WOOD)));
    properties.setProperty("iron", String.valueOf(bundle.gameState().resources().get(ResourceType.IRON)));
    if (bundle.gameState().currentInteriorWorldId() != null) {
      properties.setProperty("interiorWorld", bundle.gameState().currentInteriorWorldId());
    }
    properties.setProperty("gameCreatedAt", bundle.gameState().createdAt().toString());
    properties.setProperty("gameUpdatedAt", bundle.gameState().updatedAt().toString());
    if (bundle.gameState().populationMetadataJson() != null) {
      properties.setProperty("populationMetadata", bundle.gameState().populationMetadataJson());
    }
    if (bundle.gameState().agingProfile().unresolvedCadence() != null) {
      properties.setProperty("agingCadence", bundle.gameState().agingProfile().unresolvedCadence().toString());
    }
    properties.setProperty("lastAgingEvaluation", bundle.gameState().agingProfile().lastAgingEvaluation().toString());
    StringBuilder builder = new StringBuilder();
    for (String key : properties.stringPropertyNames()) {
      builder.append(key).append("=").append(escape(properties.getProperty(key))).append("\n");
    }
    return builder.toString();
  }

  private PlayerStateBundle decode(String encoded) {
    Properties properties = new Properties();
    String[] lines = encoded.split("\\n");
    for (String line : lines) {
      if (line.isBlank() || !line.contains("=")) {
        continue;
      }
      String[] keyValue = line.split("=", 2);
      properties.setProperty(keyValue[0], unescape(keyValue[1]));
    }

    PlayerProfile profile = new PlayerProfile(
        Long.parseLong(properties.getProperty("profileId", "0")),
        UUID.fromString(properties.getProperty("playerUuid")),
        properties.getProperty("playerName"),
        ZoneId.of(properties.getProperty("timezone")),
        properties.getProperty("transformedIp"),
        Instant.parse(properties.getProperty("profileCreatedAt")),
        Instant.parse(properties.getProperty("profileUpdatedAt")),
        Instant.parse(properties.getProperty("profileLastSeenAt"))
    );

    CastleLocation castleLocation = null;
    if (properties.getProperty("castleWorld") != null) {
      castleLocation = new CastleLocation(
          properties.getProperty("castleWorld"),
          Double.parseDouble(properties.getProperty("castleX", "0")),
          Double.parseDouble(properties.getProperty("castleY", "0")),
          Double.parseDouble(properties.getProperty("castleZ", "0"))
      );
    }
    ResourceInventory resources = new ResourceInventory();
    resources.set(ResourceType.FOOD, Integer.parseInt(properties.getProperty("food", "0")));
    resources.set(ResourceType.WOOD, Integer.parseInt(properties.getProperty("wood", "0")));
    resources.set(ResourceType.IRON, Integer.parseInt(properties.getProperty("iron", "0")));

    Duration cadence = properties.getProperty("agingCadence") == null
        ? null
        : Duration.parse(properties.getProperty("agingCadence"));
    PopulationAgingProfile agingProfile = new PopulationAgingProfile(
        cadence,
        Instant.parse(properties.getProperty("lastAgingEvaluation", Instant.now().toString()))
    );

    PlayerGameState gameState = new PlayerGameState(
        Long.parseLong(properties.getProperty("profileId", "0")),
        properties.getProperty("castleId") == null ? null : UUID.fromString(properties.getProperty("castleId")),
        castleLocation,
        Integer.parseInt(properties.getProperty("citizenCount", "0")),
        Integer.parseInt(properties.getProperty("troopCount", "0")),
        resources,
        properties.getProperty("interiorWorld"),
        Instant.parse(properties.getProperty("gameCreatedAt")),
        Instant.parse(properties.getProperty("gameUpdatedAt")),
        agingProfile,
        properties.getProperty("populationMetadata")
    );

    PopulationRoster roster = new PopulationRoster();
    roster.syncToTarget(new PopulationSummary(gameState.citizenCount(), gameState.troopCount()), Instant.now());

    return new PlayerStateBundle(UUID.fromString(properties.getProperty("playerId")), profile, gameState, roster);
  }

  private String escape(String value) {
    return value.replace("\\", "\\\\").replace("\n", "\\n");
  }

  private String unescape(String value) {
    return value.replace("\\n", "\n").replace("\\\\", "\\");
  }
}
