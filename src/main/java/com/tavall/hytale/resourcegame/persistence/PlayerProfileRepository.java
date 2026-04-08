package com.tavall.hytale.resourcegame.persistence;

import com.tavall.hytale.resourcegame.domain.PlayerProfile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC repository for player profiles.
 */
public final class PlayerProfileRepository implements PlayerProfileStore {
    private final PostgresConnectionProvider connectionProvider;

    public PlayerProfileRepository(PostgresConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Optional<PlayerProfile> findByUuid(UUID uuid) throws SQLException {
        String sql = "SELECT id, uuid, name, timezone, ip_hash, created_at, updated_at, last_seen_at "
                + "FROM player_profile WHERE uuid = ?";
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, uuid);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(resultSet));
            }
        }
    }

    @Override
    public PlayerProfile upsert(UUID uuid, String name, String timezone, String ipHash, Instant now) throws SQLException {
        String sql = "INSERT INTO player_profile (uuid, name, timezone, ip_hash, created_at, updated_at, last_seen_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT (uuid) DO UPDATE SET "
                + "name = EXCLUDED.name, "
                + "timezone = EXCLUDED.timezone, "
                + "ip_hash = EXCLUDED.ip_hash, "
                + "updated_at = EXCLUDED.updated_at, "
                + "last_seen_at = EXCLUDED.last_seen_at "
                + "RETURNING id, uuid, name, timezone, ip_hash, created_at, updated_at, last_seen_at";
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, uuid);
            statement.setString(2, name);
            statement.setString(3, timezone);
            statement.setString(4, ipHash);
            statement.setTimestamp(5, Timestamp.from(now));
            statement.setTimestamp(6, Timestamp.from(now));
            statement.setTimestamp(7, Timestamp.from(now));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return mapRow(resultSet);
            }
        }
    }

    private PlayerProfile mapRow(ResultSet resultSet) throws SQLException {
        long id = resultSet.getLong("id");
        UUID uuid = (UUID) resultSet.getObject("uuid");
        String name = resultSet.getString("name");
        String timezone = resultSet.getString("timezone");
        String ipHash = resultSet.getString("ip_hash");
        Instant createdAt = toInstant(resultSet, "created_at");
        Instant updatedAt = toInstant(resultSet, "updated_at");
        Instant lastSeenAt = toInstant(resultSet, "last_seen_at");
        return new PlayerProfile(id, uuid, name, timezone, ipHash, createdAt, updatedAt, lastSeenAt);
    }

    private Instant toInstant(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
