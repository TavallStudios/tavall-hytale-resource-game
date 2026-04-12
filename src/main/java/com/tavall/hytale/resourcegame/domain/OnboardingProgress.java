package com.tavall.hytale.resourcegame.domain;

/**
 * Tracks first-join tutorial milestones that should only appear once.
 */
public final class OnboardingProgress {
    private final boolean firstInteriorTutorialPending;
    private final boolean firstUpgradeTutorialPending;

    public OnboardingProgress(boolean firstInteriorTutorialPending, boolean firstUpgradeTutorialPending) {
        this.firstInteriorTutorialPending = firstInteriorTutorialPending;
        this.firstUpgradeTutorialPending = firstUpgradeTutorialPending;
    }

    public boolean firstInteriorTutorialPending() {
        return firstInteriorTutorialPending;
    }

    public boolean firstUpgradeTutorialPending() {
        return firstUpgradeTutorialPending;
    }

    public OnboardingProgress markInteriorTutorialSeen() {
        if (!firstInteriorTutorialPending) {
            return this;
        }
        return new OnboardingProgress(false, firstUpgradeTutorialPending);
    }

    public OnboardingProgress markUpgradeTutorialSeen() {
        if (!firstUpgradeTutorialPending) {
            return this;
        }
        return new OnboardingProgress(firstInteriorTutorialPending, false);
    }

    public static OnboardingProgress defaults() {
        return new OnboardingProgress(true, true);
    }
}
