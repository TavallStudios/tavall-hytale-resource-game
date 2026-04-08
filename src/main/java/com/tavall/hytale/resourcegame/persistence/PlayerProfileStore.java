package com.tavall.hytale.resourcegame.persistence;

import com.tavall.hytale.resourcegame.domain.PlayerProfile;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence contract for player profiles.
 */
public interface PlayerProfileStore {
    Optional<PlayerProfile> findByUuid(UUID uuid) throws SQLException;

    PlayerProfile upsert(UUID uuid, String name, String timezone, String ipHash, Instant now) throws SQLException;
}
