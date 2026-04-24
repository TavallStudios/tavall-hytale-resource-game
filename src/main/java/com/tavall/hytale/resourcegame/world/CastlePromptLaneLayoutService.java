package com.tavall.hytale.resourcegame.world;

import com.hypixel.hytale.math.vector.Vector3d;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;

/**
 * Computes a deterministic standing lane for castle prompt testing and onboarding.
 */
public final class CastlePromptLaneLayoutService {
    private static final double PROMPT_DISTANCE = 4.0;

    public CastlePromptLaneLayout createLayout(CastleLocationData castleLocation) {
        double floorY = castleLocation.supportBlockVector().getY();
        double promptZ = castleLocation.z() - PROMPT_DISTANCE;
        Vector3d origin = new Vector3d(castleLocation.x(), floorY, promptZ);
        Vector3d alignmentPoint = new Vector3d(castleLocation.x(), castleLocation.standingBaseVector().getY(), promptZ);
        return new CastlePromptLaneLayout(origin, alignmentPoint);
    }
}
