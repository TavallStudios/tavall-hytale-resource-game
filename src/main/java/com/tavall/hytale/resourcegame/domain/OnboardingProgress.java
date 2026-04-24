package com.tavall.hytale.resourcegame.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Tracks first-join tutorial milestones that should only appear once.
 */
public final class OnboardingProgress {
    private final boolean firstInteriorTutorialPending;
    private final boolean firstUpgradeTutorialPending;
    private final boolean firstInteriorTourPending;

    @JsonCreator
    public OnboardingProgress(
            @JsonProperty("firstInteriorTutorialPending") Boolean firstInteriorTutorialPending,
            @JsonProperty("firstUpgradeTutorialPending") Boolean firstUpgradeTutorialPending,
            @JsonProperty("firstInteriorTourPending") Boolean firstInteriorTourPending
    ) {
        this.firstInteriorTutorialPending = firstInteriorTutorialPending == null || firstInteriorTutorialPending;
        this.firstUpgradeTutorialPending = firstUpgradeTutorialPending == null || firstUpgradeTutorialPending;
        this.firstInteriorTourPending = firstInteriorTourPending == null || firstInteriorTourPending;
    }

    public boolean firstInteriorTutorialPending() {
        return firstInteriorTutorialPending;
    }

    public boolean firstUpgradeTutorialPending() {
        return firstUpgradeTutorialPending;
    }

    public boolean firstInteriorTourPending() {
        return firstInteriorTourPending;
    }

    public OnboardingProgress markInteriorTutorialSeen() {
        if (!firstInteriorTutorialPending) {
            return this;
        }
        return new OnboardingProgress(false, firstUpgradeTutorialPending, firstInteriorTourPending);
    }

    public OnboardingProgress markInteriorTourSeen() {
        if (!firstInteriorTourPending) {
            return this;
        }
        return new OnboardingProgress(firstInteriorTutorialPending, firstUpgradeTutorialPending, false);
    }

    public OnboardingProgress markUpgradeTutorialSeen() {
        if (!firstUpgradeTutorialPending) {
            return this;
        }
        return new OnboardingProgress(firstInteriorTutorialPending, false, firstInteriorTourPending);
    }

    public static OnboardingProgress defaults() {
        return new OnboardingProgress(true, true, true);
    }
}
