package com.tavall.hytale.resourcegame.domain;

import com.tavall.hytale.resourcegame.ui.UiPageType;

import java.util.Objects;

/**
 * Stores the last tracked UI page and navigation context for a player.
 */
public final class TrackedUiState {
    private final UiPageType pageType;
    private final UiNavigationContext navigationContext;

    public TrackedUiState(UiPageType pageType, UiNavigationContext navigationContext) {
        this.pageType = Objects.requireNonNull(pageType, "pageType");
        this.navigationContext = Objects.requireNonNull(navigationContext, "navigationContext");
    }

    public UiPageType pageType() {
        return pageType;
    }

    public UiNavigationContext navigationContext() {
        return navigationContext;
    }
}
