package org.tavall.hytale.resourcegame.domain.ui;

public record UiAction(String actionId, String label, boolean enabled, String blockedReason) {
}
