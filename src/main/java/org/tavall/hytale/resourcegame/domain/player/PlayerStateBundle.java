package org.tavall.hytale.resourcegame.domain.player;

import java.util.Objects;
import java.util.UUID;
import org.tavall.hytale.resourcegame.domain.population.PopulationRoster;

/**
 * Active aggregate used by gameplay systems and persistence orchestration.
 */
public class PlayerStateBundle {

  private final UUID playerId;
  private final PlayerProfile profile;
  private final PlayerGameState gameState;
  private final PopulationRoster populationRoster;

  public PlayerStateBundle(
      UUID playerId,
      PlayerProfile profile,
      PlayerGameState gameState,
      PopulationRoster populationRoster
  ) {
    this.playerId = Objects.requireNonNull(playerId, "playerId");
    this.profile = Objects.requireNonNull(profile, "profile");
    this.gameState = Objects.requireNonNull(gameState, "gameState");
    this.populationRoster = Objects.requireNonNull(populationRoster, "populationRoster");
  }

  public UUID playerId() {
    return playerId;
  }

  public PlayerProfile profile() {
    return profile;
  }

  public PlayerGameState gameState() {
    return gameState;
  }

  public PopulationRoster populationRoster() {
    return populationRoster;
  }
}
