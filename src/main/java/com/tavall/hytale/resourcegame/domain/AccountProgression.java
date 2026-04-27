package com.tavall.hytale.resourcegame.domain;

/**
 * Account-level progression gate for kingdom-wide unlocks.
 */
public record AccountProgression(int level, int experience, long totalExperience) {
    public static final int FULL_UNLOCK_LEVEL = 50;
    private static final int DEFAULT_LEVEL = 1;
    private static final int BASE_REQUIRED_EXPERIENCE = 120;
    private static final int LINEAR_LEVEL_STEP = 45;
    private static final int HARDENING_LEVEL = 25;
    private static final int FIXED_POST_UNLOCK_REQUIRED_EXPERIENCE = 8_000;

    public AccountProgression {
        level = Math.max(DEFAULT_LEVEL, level);
        int requiredExperience = requiredExperienceForNextLevel(level);
        experience = Math.max(0, Math.min(experience, requiredExperience - 1));
        totalExperience = Math.max(0L, totalExperience);
    }

    public static AccountProgression defaults() {
        return new AccountProgression(DEFAULT_LEVEL, 0, 0L);
    }

    public boolean isUnlocked(BuildingType buildingType) {
        return buildingType != null && level >= requiredLevel(buildingType);
    }

    public int requiredLevel(BuildingType buildingType) {
        if (buildingType == null) {
            return FULL_UNLOCK_LEVEL;
        }
        return switch (buildingType) {
            case FARMSTEAD -> 5;
            case LUMBER_MILL -> 10;
            case IRON_WORKS -> 15;
            case BARRACKS -> 25;
            case WORKSHOP -> 35;
        };
    }

    public AccountProgression withAddedExperience(int gainedExperience) {
        if (gainedExperience <= 0) {
            return this;
        }
        int nextLevel = level;
        int nextExperience = experience + gainedExperience;
        long nextTotalExperience = totalExperience + gainedExperience;
        while (nextExperience >= requiredExperienceForNextLevel(nextLevel)) {
            int requiredExperience = requiredExperienceForNextLevel(nextLevel);
            nextExperience -= requiredExperience;
            nextLevel++;
        }
        return new AccountProgression(nextLevel, nextExperience, nextTotalExperience);
    }

    public AccountProgression withLevel(int targetLevel) {
        return new AccountProgression(Math.max(DEFAULT_LEVEL, targetLevel), 0, totalExperience);
    }

    public int requiredExperienceForNextLevel() {
        return requiredExperienceForNextLevel(level);
    }

    public static int requiredExperienceForNextLevel(int level) {
        int safeLevel = Math.max(DEFAULT_LEVEL, level);
        if (safeLevel >= FULL_UNLOCK_LEVEL) {
            return FIXED_POST_UNLOCK_REQUIRED_EXPERIENCE;
        }
        int linearRequirement = BASE_REQUIRED_EXPERIENCE + (safeLevel * LINEAR_LEVEL_STEP);
        if (safeLevel < HARDENING_LEVEL) {
            return linearRequirement;
        }
        int hardenedLevels = safeLevel - HARDENING_LEVEL + 1;
        return linearRequirement + (hardenedLevels * hardenedLevels * 30);
    }
}
