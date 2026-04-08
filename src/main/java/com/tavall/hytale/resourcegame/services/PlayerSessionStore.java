package com.tavall.hytale.resourcegame.services;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory session cache for active players.
 */
public final class PlayerSessionStore {
    private final Map<UUID, PlayerSession> sessions = new ConcurrentHashMap<>();

    public PlayerSession get(UUID playerId) {
        return sessions.get(playerId);
    }

    public void put(PlayerSession session) {
        sessions.put(session.playerId(), session);
    }

    public void remove(UUID playerId) {
        sessions.remove(playerId);
    }
}
