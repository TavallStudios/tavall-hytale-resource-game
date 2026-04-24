package com.tavall.hytale.resourcegame.world;

import com.hypixel.hytale.math.vector.Vector3d;
import com.tavall.hytale.resourcegame.domain.ResourceNodeData;

/**
 * Computes a deterministic standing lane for resource-node focus and selection.
 */
public final class ResourceNodePromptLaneLayoutService {
    private static final double PROMPT_DISTANCE = 4.0D;

    public ResourceNodePromptLaneLayout createLayout(ResourceNodeData node) {
        double floorY = node.location().supportBlockVector().getY();
        double promptZ = node.z() - PROMPT_DISTANCE;
        Vector3d origin = new Vector3d(node.x(), floorY, promptZ);
        Vector3d alignmentPoint = new Vector3d(node.x(), node.location().standingBaseVector().getY(), promptZ);
        return new ResourceNodePromptLaneLayout(origin, alignmentPoint);
    }
}
