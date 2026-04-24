package com.tavall.hytale.resourcegame.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class BuildingTypeTest {
    @Test
    void buildingTypesExposeMaxLevelThirty() {
        for (BuildingType type : BuildingType.values()) {
            assertEquals(30, type.maxLevel(), type + " maxLevel");
        }
    }

    @Test
    void levelProfileClampsToValidRange() {
        for (BuildingType type : BuildingType.values()) {
            BuildingLevelProfile minProfile = type.levelProfile(1);
            BuildingLevelProfile clampedLow = type.levelProfile(0);
            BuildingLevelProfile clampedHigh = type.levelProfile(999);
            BuildingLevelProfile maxProfile = type.levelProfile(type.maxLevel());

            assertProfilesEqual(minProfile, clampedLow, type + " clampedLow");
            assertProfilesEqual(maxProfile, clampedHigh, type + " clampedHigh");
        }
    }

    @Test
    void scaledProfilesIncreaseMeaningfullyByMaxLevel() {
        BuildingLevelProfile farm3 = BuildingType.FARMSTEAD.levelProfile(3);
        BuildingLevelProfile farm30 = BuildingType.FARMSTEAD.levelProfile(30);
        assertTrue(farm30.foodPerTickBonus() > farm3.foodPerTickBonus());
        assertTrue(farm30.buildSeconds() > farm3.buildSeconds());

        BuildingLevelProfile barracks3 = BuildingType.BARRACKS.levelProfile(3);
        BuildingLevelProfile barracks30 = BuildingType.BARRACKS.levelProfile(30);
        assertTrue(barracks30.promotionDiscount().foodCost() >= barracks3.promotionDiscount().foodCost());

        BuildingLevelProfile workshop3 = BuildingType.WORKSHOP.levelProfile(3);
        BuildingLevelProfile workshop30 = BuildingType.WORKSHOP.levelProfile(30);
        assertTrue(workshop30.constructionSpeedBonus() >= workshop3.constructionSpeedBonus());
    }

    private void assertProfilesEqual(BuildingLevelProfile expected, BuildingLevelProfile actual, String label) {
        assertEquals(expected.foodCost(), actual.foodCost(), label + " foodCost");
        assertEquals(expected.woodCost(), actual.woodCost(), label + " woodCost");
        assertEquals(expected.ironCost(), actual.ironCost(), label + " ironCost");
        assertEquals(expected.buildSeconds(), actual.buildSeconds(), label + " buildSeconds");
        assertEquals(expected.foodPerTickBonus(), actual.foodPerTickBonus(), label + " foodPerTickBonus");
        assertEquals(expected.woodPerTickBonus(), actual.woodPerTickBonus(), label + " woodPerTickBonus");
        assertEquals(expected.ironPerTickBonus(), actual.ironPerTickBonus(), label + " ironPerTickBonus");
        assertEquals(expected.constructionSpeedBonus(), actual.constructionSpeedBonus(), 0.00001D, label + " constructionSpeedBonus");
        assertEquals(expected.promotionDiscount().foodCost(), actual.promotionDiscount().foodCost(), label + " promotionDiscountFood");
        assertEquals(expected.promotionDiscount().woodCost(), actual.promotionDiscount().woodCost(), label + " promotionDiscountWood");
        assertEquals(expected.promotionDiscount().ironCost(), actual.promotionDiscount().ironCost(), label + " promotionDiscountIron");
    }
}
