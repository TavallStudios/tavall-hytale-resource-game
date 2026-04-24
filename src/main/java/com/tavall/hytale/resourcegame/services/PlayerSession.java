package com.tavall.hytale.resourcegame.services;

import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.PlayerProfile;

import java.util.Objects;
import java.util.UUID;

/**
 * Live session data for an active player.
 */
public final class PlayerSession {
    private final UUID playerId;
    private volatile PlayerProfile profile;
    private volatile PlayerGameState gameState;

    public PlayerSession(UUID playerId, PlayerProfile profile, PlayerGameState gameState) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.profile = Objects.requireNonNull(profile, "profile");
        this.gameState = Objects.requireNonNull(gameState, "gameState");
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

    public void updateProfile(PlayerProfile profile) {
        this.profile = Objects.requireNonNull(profile, "profile");
    }

    public void updateGameState(PlayerGameState gameState) {
        this.gameState = Objects.requireNonNull(gameState, "gameState");
    }
}
