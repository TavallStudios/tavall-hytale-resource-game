package com.tavall.hytale.resourcegame.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Provides UI navigation context for the current player.
 */
public final class UiNavigationContext {
    private final UUID playerId;
    private final String playerName;
    private final String feedbackMessage;

    public UiNavigationContext(UUID playerId, String playerName) {
        this(playerId, playerName, "");
    }

    public UiNavigationContext(UUID playerId, String playerName, String feedbackMessage) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.playerName = Objects.requireNonNull(playerName, "playerName");
        this.feedbackMessage = feedbackMessage == null ? "" : feedbackMessage;
    }

    public UUID playerId() {
        return playerId;
    }

    public String playerName() {
        return playerName;
    }

    public String feedbackMessage() {
        return feedbackMessage;
    }

    public UiNavigationContext withFeedback(String feedbackMessage) {
        return new UiNavigationContext(playerId, playerName, feedbackMessage);
    }

    public UiNavigationContext clearFeedback() {
        return new UiNavigationContext(playerId, playerName);
    }
}
