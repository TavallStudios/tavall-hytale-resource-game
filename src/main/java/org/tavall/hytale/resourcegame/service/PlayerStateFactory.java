package org.tavall.hytale.resourcegame.service;

import java.time.Duration;
import java.time.Instant;
import org.tavall.hytale.resourcegame.domain.player.PlayerGameState;
import org.tavall.hytale.resourcegame.domain.player.PlayerJoinRequest;
import org.tavall.hytale.resourcegame.domain.player.PlayerProfile;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;
import org.tavall.hytale.resourcegame.domain.player.PopulationAgingProfile;
import org.tavall.hytale.resourcegame.domain.population.PopulationRoster;
import org.tavall.hytale.resourcegame.domain.population.PopulationSummary;
import org.tavall.hytale.resourcegame.domain.resource.ResourceInventory;
import org.tavall.hytale.resourcegame.domain.resource.ResourceType;

/**
 * Creates the first-play session aggregate with starter values.
 */
public class PlayerStateFactory {

  public PlayerStateBundle createFirstJoin(PlayerJoinRequest joinRequest, Instant now) {
    PlayerProfile profile = new PlayerProfile(
        0L,
        joinRequest.playerId(),
        joinRequest.playerName(),
        joinRequest.timezone(),
        joinRequest.transformedIp(),
        now,
        now,
        now
    );
    ResourceInventory resources = new ResourceInventory();
    resources.set(ResourceType.FOOD, 120);
    resources.set(ResourceType.WOOD, 90);
    resources.set(ResourceType.IRON, 45);
    PopulationAgingProfile agingProfile = new PopulationAgingProfile(Duration.ofDays(1), now);
    PlayerGameState gameState = new PlayerGameState(
        0L,
        null,
        null,
        6,
        0,
        resources,
        null,
        now,
        now,
        agingProfile,
        null
    );
    PopulationRoster roster = new PopulationRoster();
    roster.syncToTarget(new PopulationSummary(6, 0), now);
    return new PlayerStateBundle(joinRequest.playerId(), profile, gameState, roster);
  }
}
