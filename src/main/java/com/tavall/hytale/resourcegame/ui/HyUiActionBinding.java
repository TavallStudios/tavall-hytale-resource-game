package com.tavall.hytale.resourcegame.ui;

import java.util.Objects;

/**
 * Maps one HYUIML element activation to the existing UI action pipeline.
 */
public record HyUiActionBinding(String elementId, String action, String payload) {
    public HyUiActionBinding {
        elementId = normalizeElementId(elementId);
        action = Objects.requireNonNull(action, "action");
        payload = payload == null ? "" : payload;
        if (elementId.isBlank()) {
            throw new IllegalArgumentException("elementId cannot be blank");
        }
        if (action.isBlank()) {
            throw new IllegalArgumentException("action cannot be blank");
        }
    }

    public static HyUiActionBinding action(String selectorOrElementId, String action) {
        return new HyUiActionBinding(selectorOrElementId, action, "");
    }

    public static HyUiActionBinding command(String selectorOrElementId, String commandLine) {
        return new HyUiActionBinding(selectorOrElementId, UiActions.RUN_COMMAND, commandLine);
    }

    public UiActionEventData eventData() {
        if (payload.isBlank()) {
            return UiActionEventData.action(action);
        }
        return UiActionEventData.actionWithPayload(action, payload);
    }

    private static String normalizeElementId(String selectorOrElementId) {
        String normalized = Objects.requireNonNull(selectorOrElementId, "selectorOrElementId").trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        int dotIndex = normalized.indexOf('.');
        if (dotIndex >= 0) {
            normalized = normalized.substring(0, dotIndex);
        }
        return normalized;
    }
}
