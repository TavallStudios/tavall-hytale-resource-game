package com.tavall.hytale.resourcegame.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class AccountProgressionTest {
    @Test
    void defaultAccountStartsWithNoBuildingUnlocks() {
        AccountProgression progression = AccountProgression.defaults();

        assertEquals(1, progression.level());
        for (BuildingType buildingType : BuildingType.values()) {
            assertFalse(progression.isUnlocked(buildingType), buildingType + " should start locked");
        }
    }

    @Test
    void buildingsUnlockByAccountLevel() {
        AccountProgression levelFive = AccountProgression.defaults().withLevel(5);
        AccountProgression levelFifty = AccountProgression.defaults().withLevel(50);

        assertTrue(levelFive.isUnlocked(BuildingType.FARMSTEAD));
        assertFalse(levelFive.isUnlocked(BuildingType.LUMBER_MILL));
        for (BuildingType buildingType : BuildingType.values()) {
            assertTrue(levelFifty.isUnlocked(buildingType), buildingType + " should unlock by level 50");
        }
    }

    @Test
    void experienceRequirementStopsIncreasingAfterFullUnlockLevel() {
        int levelFiftyRequirement = AccountProgression.requiredExperienceForNextLevel(50);

        assertEquals(levelFiftyRequirement, AccountProgression.requiredExperienceForNextLevel(75));
        assertTrue(levelFiftyRequirement > AccountProgression.requiredExperienceForNextLevel(10));
    }

    @Test
    void addedExperienceCarriesAcrossLevels() {
        AccountProgression progression = AccountProgression.defaults();
        int requiredForLevelTwo = progression.requiredExperienceForNextLevel();

        AccountProgression leveled = progression.withAddedExperience(requiredForLevelTwo + 5);

        assertEquals(2, leveled.level());
        assertEquals(5, leveled.experience());
        assertEquals(requiredForLevelTwo + 5L, leveled.totalExperience());
    }
}
