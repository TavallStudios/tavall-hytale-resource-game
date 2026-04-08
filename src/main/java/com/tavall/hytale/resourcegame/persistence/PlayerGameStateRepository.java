package com.tavall.hytale.resourcegame.persistence;

import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.InteriorSessionData;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.PopulationSummary;
import com.tavall.hytale.resourcegame.domain.ResourceInventory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC repository for player game state.
 */
public final class PlayerGameStateRepository implements PlayerGameStateStore {
    private final PostgresConnectionProvider connectionProvider;

    public PlayerGameStateRepository(PostgresConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Optional<PlayerGameState> findByProfileId(long profileId) throws SQLException {
        String sql = "SELECT id, profile_id, castle_id, castle_world, castle_x, castle_y, castle_z, citizen_count, troop_count, "
                + "food, wood, iron, interior_world, metadata_json, created_at, updated_at "
                + "FROM player_game_state WHERE profile_id = ?";
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, profileId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(resultSet));
            }
        }
    }

    @Override
    public PlayerGameState upsert(PlayerGameState state, Instant now) throws SQLException {
        String sql = "INSERT INTO player_game_state (profile_id, castle_id, castle_world, castle_x, castle_y, castle_z, "
                + "citizen_count, troop_count, food, wood, iron, interior_world, metadata_json, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?) "
                + "ON CONFLICT (profile_id) DO UPDATE SET "
                + "castle_id = EXCLUDED.castle_id, "
                + "castle_world = EXCLUDED.castle_world, "
                + "castle_x = EXCLUDED.castle_x, "
                + "castle_y = EXCLUDED.castle_y, "
                + "castle_z = EXCLUDED.castle_z, "
                + "citizen_count = EXCLUDED.citizen_count, "
                + "troop_count = EXCLUDED.troop_count, "
                + "food = EXCLUDED.food, "
                + "wood = EXCLUDED.wood, "
                + "iron = EXCLUDED.iron, "
                + "interior_world = EXCLUDED.interior_world, "
                + "metadata_json = EXCLUDED.metadata_json, "
                + "updated_at = EXCLUDED.updated_at "
                + "RETURNING id, profile_id, castle_id, castle_world, castle_x, castle_y, castle_z, citizen_count, troop_count, "
                + "food, wood, iron, interior_world, metadata_json, created_at, updated_at";
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setState(statement, state, now);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return mapRow(resultSet);
            }
        }
    }

    private void setState(PreparedStatement statement, PlayerGameState state, Instant now) throws SQLException {
        CastleLocationData location = state.castleLocation();
        int index = 1;
        statement.setLong(index++, state.profileId());
        statement.setObject(index++, state.castleId());
        statement.setString(index++, location == null ? null : location.worldName());
        statement.setObject(index++, location == null ? null : location.x());
        statement.setObject(index++, location == null ? null : location.y());
        statement.setObject(index++, location == null ? null : location.z());
        statement.setInt(index++, state.populationSummary().citizenCount());
        statement.setInt(index++, state.populationSummary().troopCount());
        statement.setInt(index++, state.resources().food());
        statement.setInt(index++, state.resources().wood());
        statement.setInt(index++, state.resources().iron());
        statement.setString(index++, state.interiorSession() == null ? null : state.interiorSession().interiorWorldName());
        statement.setString(index++, state.metadataJson());
        statement.setTimestamp(index++, Timestamp.from(now));
        statement.setTimestamp(index, Timestamp.from(now));
    }

    private PlayerGameState mapRow(ResultSet resultSet) throws SQLException {
        long id = resultSet.getLong("id");
        long profileId = resultSet.getLong("profile_id");
        UUID castleId = (UUID) resultSet.getObject("castle_id");
        String world = resultSet.getString("castle_world");
        Double x = resultSet.getObject("castle_x", Double.class);
        Double y = resultSet.getObject("castle_y", Double.class);
        Double z = resultSet.getObject("castle_z", Double.class);
        CastleLocationData location = world == null || x == null || y == null || z == null
                ? null
                : new CastleLocationData(world, x, y, z);
        int citizenCount = resultSet.getInt("citizen_count");
        int troopCount = resultSet.getInt("troop_count");
        int food = resultSet.getInt("food");
        int wood = resultSet.getInt("wood");
        int iron = resultSet.getInt("iron");
        String interiorWorld = resultSet.getString("interior_world");
        String metadataJson = resultSet.getString("metadata_json");
        Instant createdAt = toInstant(resultSet, "created_at");
        Instant updatedAt = toInstant(resultSet, "updated_at");

        PopulationSummary populationSummary = new PopulationSummary(
                citizenCount,
                troopCount,
                PopulationSummaryDefaults.citizenMetaData(),
                PopulationSummaryDefaults.troopMetaData(),
                PopulationSummaryDefaults.agingState()
        );
        ResourceInventory resources = new ResourceInventory(food, wood, iron);
        InteriorSessionData interiorSession = interiorWorld == null || location == null
                ? null
                : new InteriorSessionData(interiorWorld, location, updatedAt == null ? Instant.now() : updatedAt);
        return new PlayerGameState(
                id,
                profileId,
                castleId,
                location,
                populationSummary,
                resources,
                interiorSession,
                metadataJson,
                createdAt,
                updatedAt
        );
    }

    private Instant toInstant(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
