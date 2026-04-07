package org.tavall.hytale.resourcegame.persistence.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.tavall.hytale.resourcegame.domain.castle.CastleLocation;
import org.tavall.hytale.resourcegame.domain.player.PlayerGameState;
import org.tavall.hytale.resourcegame.domain.player.PlayerProfile;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;
import org.tavall.hytale.resourcegame.domain.player.PopulationAgingProfile;
import org.tavall.hytale.resourcegame.domain.population.CitizenAttributes;
import org.tavall.hytale.resourcegame.domain.population.CitizenJob;
import org.tavall.hytale.resourcegame.domain.population.CitizenUnitProfile;
import org.tavall.hytale.resourcegame.domain.population.PopulationRole;
import org.tavall.hytale.resourcegame.domain.population.PopulationRoster;
import org.tavall.hytale.resourcegame.domain.resource.ResourceInventory;
import org.tavall.hytale.resourcegame.domain.resource.ResourceType;
import org.tavall.hytale.resourcegame.persistence.PlayerStateStore;

/**
 * Postgres-backed durable store for profile, kingdom state, and population unit metadata.
 */
public class PostgresPlayerStateStore implements PlayerStateStore {

  private final DataSource dataSource;

  public PostgresPlayerStateStore(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public Optional<PlayerStateBundle> load(UUID playerId) {
    try (Connection connection = dataSource.getConnection()) {
      Optional<PlayerProfile> profile = loadProfile(connection, playerId);
      if (profile.isEmpty()) {
        return Optional.empty();
      }
      Optional<PlayerGameState> gameState = loadGameState(connection, profile.get().internalPlayerId());
      if (gameState.isEmpty()) {
        return Optional.empty();
      }
      PopulationRoster roster = loadPopulationRoster(connection, profile.get().internalPlayerId());
      if (roster.allUnits().isEmpty()) {
        roster.syncToTarget(gameState.get().populationSummary(), Instant.now());
      }
      return Optional.of(new PlayerStateBundle(playerId, profile.get(), gameState.get(), roster));
    } catch (SQLException exception) {
      throw new IllegalStateException("Unable to load player state", exception);
    }
  }

  @Override
  public PlayerStateBundle save(PlayerStateBundle bundle) {
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      long profileId = saveProfile(connection, bundle.profile());
      bundle.profile().assignInternalPlayerId(profileId);
      bundle.gameState().assignProfileId(profileId);
      saveGameState(connection, profileId, bundle.gameState());
      savePopulationRoster(connection, profileId, bundle.populationRoster());
      connection.commit();
      return bundle;
    } catch (SQLException exception) {
      throw new IllegalStateException("Unable to save player state", exception);
    }
  }

  private Optional<PlayerProfile> loadProfile(Connection connection, UUID playerId) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("""
        SELECT internal_player_id, player_uuid, player_name, timezone, transformed_ip, created_at, updated_at, last_seen_at
        FROM player_profile
        WHERE player_uuid = ?
        """)) {
      statement.setString(1, playerId.toString());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(new PlayerProfile(
            resultSet.getLong("internal_player_id"),
            UUID.fromString(resultSet.getString("player_uuid")),
            resultSet.getString("player_name"),
            ZoneId.of(resultSet.getString("timezone")),
            resultSet.getString("transformed_ip"),
            resultSet.getTimestamp("created_at").toInstant(),
            resultSet.getTimestamp("updated_at").toInstant(),
            resultSet.getTimestamp("last_seen_at").toInstant()
        ));
      }
    }
  }

  private Optional<PlayerGameState> loadGameState(Connection connection, long profileId) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("""
        SELECT profile_id, castle_id, castle_world, castle_x, castle_y, castle_z,
               citizen_count, troop_count, food, wood, iron, current_interior_world,
               population_metadata_json, aging_cadence, last_aging_evaluation,
               created_at, updated_at
        FROM player_game_state
        WHERE profile_id = ?
        """)) {
      statement.setLong(1, profileId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        ResourceInventory resources = new ResourceInventory();
        resources.set(ResourceType.FOOD, resultSet.getInt("food"));
        resources.set(ResourceType.WOOD, resultSet.getInt("wood"));
        resources.set(ResourceType.IRON, resultSet.getInt("iron"));

        CastleLocation castleLocation = null;
        if (resultSet.getString("castle_world") != null) {
          castleLocation = new CastleLocation(
              resultSet.getString("castle_world"),
              resultSet.getDouble("castle_x"),
              resultSet.getDouble("castle_y"),
              resultSet.getDouble("castle_z")
          );
        }
        String cadenceText = resultSet.getString("aging_cadence");
        Duration cadence = cadenceText == null ? null : Duration.parse(cadenceText);
        Timestamp lastAgingTime = resultSet.getTimestamp("last_aging_evaluation");
        PopulationAgingProfile agingProfile = new PopulationAgingProfile(
            cadence,
            lastAgingTime == null ? Instant.now() : lastAgingTime.toInstant()
        );
        PlayerGameState gameState = new PlayerGameState(
            resultSet.getLong("profile_id"),
            resultSet.getString("castle_id") == null ? null : UUID.fromString(resultSet.getString("castle_id")),
            castleLocation,
            resultSet.getInt("citizen_count"),
            resultSet.getInt("troop_count"),
            resources,
            resultSet.getString("current_interior_world"),
            resultSet.getTimestamp("created_at").toInstant(),
            resultSet.getTimestamp("updated_at").toInstant(),
            agingProfile,
            resultSet.getString("population_metadata_json")
        );
        return Optional.of(gameState);
      }
    }
  }

  private PopulationRoster loadPopulationRoster(Connection connection, long profileId) throws SQLException {
    PopulationRoster roster = new PopulationRoster();
    try (PreparedStatement statement = connection.prepareStatement("""
        SELECT unit_id, role, job, strength, discipline, craft, morale, birth_at, last_role_shift_at
        FROM player_population_unit
        WHERE profile_id = ?
        ORDER BY unit_id
        """)) {
      statement.setLong(1, profileId);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          CitizenUnitProfile unit = new CitizenUnitProfile(
              UUID.fromString(resultSet.getString("unit_id")),
              PopulationRole.valueOf(resultSet.getString("role")),
              CitizenJob.valueOf(resultSet.getString("job")),
              new CitizenAttributes(
                  resultSet.getInt("strength"),
                  resultSet.getInt("discipline"),
                  resultSet.getInt("craft"),
                  resultSet.getInt("morale")
              ),
              resultSet.getTimestamp("birth_at").toInstant(),
              resultSet.getTimestamp("last_role_shift_at").toInstant()
          );
          roster.addUnit(unit);
        }
      }
    }
    return roster;
  }

  private long saveProfile(Connection connection, PlayerProfile profile) throws SQLException {
    Optional<Long> existingId = findProfileId(connection, profile.playerUuid());
    if (existingId.isPresent()) {
      try (PreparedStatement statement = connection.prepareStatement("""
          UPDATE player_profile
          SET player_name = ?, timezone = ?, transformed_ip = ?, updated_at = ?, last_seen_at = ?
          WHERE internal_player_id = ?
          """)) {
        statement.setString(1, profile.playerName());
        statement.setString(2, profile.timezone().getId());
        statement.setString(3, profile.transformedIp());
        statement.setTimestamp(4, Timestamp.from(profile.updatedAt()));
        statement.setTimestamp(5, Timestamp.from(profile.lastSeenAt()));
        statement.setLong(6, existingId.get());
        statement.executeUpdate();
      }
      return existingId.get();
    }
    try (PreparedStatement statement = connection.prepareStatement("""
        INSERT INTO player_profile(player_uuid, player_name, timezone, transformed_ip, created_at, updated_at, last_seen_at)
        VALUES(?, ?, ?, ?, ?, ?, ?)
        """, Statement.RETURN_GENERATED_KEYS)) {
      statement.setString(1, profile.playerUuid().toString());
      statement.setString(2, profile.playerName());
      statement.setString(3, profile.timezone().getId());
      statement.setString(4, profile.transformedIp());
      statement.setTimestamp(5, Timestamp.from(profile.createdAt()));
      statement.setTimestamp(6, Timestamp.from(profile.updatedAt()));
      statement.setTimestamp(7, Timestamp.from(profile.lastSeenAt()));
      statement.executeUpdate();
      try (ResultSet resultSet = statement.getGeneratedKeys()) {
        if (resultSet.next()) {
          return resultSet.getLong(1);
        }
      }
    }
    throw new IllegalStateException("Failed to allocate internal player id");
  }

  private Optional<Long> findProfileId(Connection connection, UUID playerUuid) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("""
        SELECT internal_player_id
        FROM player_profile
        WHERE player_uuid = ?
        """)) {
      statement.setString(1, playerUuid.toString());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return Optional.of(resultSet.getLong(1));
        }
        return Optional.empty();
      }
    }
  }

  private void saveGameState(Connection connection, long profileId, PlayerGameState gameState) throws SQLException {
    if (hasGameState(connection, profileId)) {
      updateGameState(connection, profileId, gameState);
      return;
    }
    insertGameState(connection, profileId, gameState);
  }

  private boolean hasGameState(Connection connection, long profileId) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("SELECT profile_id FROM player_game_state WHERE profile_id = ?")) {
      statement.setLong(1, profileId);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next();
      }
    }
  }

  private void updateGameState(Connection connection, long profileId, PlayerGameState gameState) throws SQLException {
    CastleLocation castleLocation = gameState.castleLocation();
    try (PreparedStatement statement = connection.prepareStatement("""
        UPDATE player_game_state
        SET castle_id=?, castle_world=?, castle_x=?, castle_y=?, castle_z=?,
            citizen_count=?, troop_count=?, food=?, wood=?, iron=?,
            current_interior_world=?, population_metadata_json=?, aging_cadence=?,
            last_aging_evaluation=?, updated_at=?
        WHERE profile_id=?
        """)) {
      statement.setString(1, gameState.castleId() == null ? null : gameState.castleId().toString());
      statement.setString(2, castleLocation == null ? null : castleLocation.worldId());
      statement.setObject(3, castleLocation == null ? null : castleLocation.x());
      statement.setObject(4, castleLocation == null ? null : castleLocation.y());
      statement.setObject(5, castleLocation == null ? null : castleLocation.z());
      statement.setInt(6, gameState.citizenCount());
      statement.setInt(7, gameState.troopCount());
      statement.setInt(8, gameState.resources().get(ResourceType.FOOD));
      statement.setInt(9, gameState.resources().get(ResourceType.WOOD));
      statement.setInt(10, gameState.resources().get(ResourceType.IRON));
      statement.setString(11, gameState.currentInteriorWorldId());
      statement.setString(12, gameState.populationMetadataJson());
      statement.setString(13, gameState.agingProfile().unresolvedCadence() == null
          ? null
          : gameState.agingProfile().unresolvedCadence().toString());
      statement.setTimestamp(14, Timestamp.from(gameState.agingProfile().lastAgingEvaluation()));
      statement.setTimestamp(15, Timestamp.from(gameState.updatedAt()));
      statement.setLong(16, profileId);
      statement.executeUpdate();
    }
  }

  private void insertGameState(Connection connection, long profileId, PlayerGameState gameState) throws SQLException {
    CastleLocation castleLocation = gameState.castleLocation();
    try (PreparedStatement statement = connection.prepareStatement("""
        INSERT INTO player_game_state(
          profile_id, castle_id, castle_world, castle_x, castle_y, castle_z,
          citizen_count, troop_count, food, wood, iron,
          current_interior_world, population_metadata_json, aging_cadence,
          last_aging_evaluation, created_at, updated_at
        ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """)) {
      statement.setLong(1, profileId);
      statement.setString(2, gameState.castleId() == null ? null : gameState.castleId().toString());
      statement.setString(3, castleLocation == null ? null : castleLocation.worldId());
      statement.setObject(4, castleLocation == null ? null : castleLocation.x());
      statement.setObject(5, castleLocation == null ? null : castleLocation.y());
      statement.setObject(6, castleLocation == null ? null : castleLocation.z());
      statement.setInt(7, gameState.citizenCount());
      statement.setInt(8, gameState.troopCount());
      statement.setInt(9, gameState.resources().get(ResourceType.FOOD));
      statement.setInt(10, gameState.resources().get(ResourceType.WOOD));
      statement.setInt(11, gameState.resources().get(ResourceType.IRON));
      statement.setString(12, gameState.currentInteriorWorldId());
      statement.setString(13, gameState.populationMetadataJson());
      statement.setString(14, gameState.agingProfile().unresolvedCadence() == null
          ? null
          : gameState.agingProfile().unresolvedCadence().toString());
      statement.setTimestamp(15, Timestamp.from(gameState.agingProfile().lastAgingEvaluation()));
      statement.setTimestamp(16, Timestamp.from(gameState.createdAt()));
      statement.setTimestamp(17, Timestamp.from(gameState.updatedAt()));
      statement.executeUpdate();
    }
  }

  private void savePopulationRoster(Connection connection, long profileId, PopulationRoster roster) throws SQLException {
    try (PreparedStatement deleteStatement = connection.prepareStatement(
        "DELETE FROM player_population_unit WHERE profile_id = ?")) {
      deleteStatement.setLong(1, profileId);
      deleteStatement.executeUpdate();
    }
    try (PreparedStatement insertStatement = connection.prepareStatement("""
        INSERT INTO player_population_unit(
          profile_id, unit_id, role, job, strength, discipline, craft, morale, birth_at, last_role_shift_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """)) {
      for (CitizenUnitProfile unit : roster.allUnits()) {
        insertStatement.setLong(1, profileId);
        insertStatement.setString(2, unit.unitId().toString());
        insertStatement.setString(3, unit.role().name());
        insertStatement.setString(4, unit.job().name());
        insertStatement.setInt(5, unit.attributes().strength());
        insertStatement.setInt(6, unit.attributes().discipline());
        insertStatement.setInt(7, unit.attributes().craft());
        insertStatement.setInt(8, unit.attributes().morale());
        insertStatement.setTimestamp(9, Timestamp.from(unit.birthAt()));
        insertStatement.setTimestamp(10, Timestamp.from(unit.lastRoleShiftAt()));
        insertStatement.addBatch();
      }
      insertStatement.executeBatch();
    }
  }
}
