package com.tavall.hytale.resourcegame.world;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Tracks the rendered refs that make up the castle-site scene.
 */
public final class CastleSiteVisualRefs {
    private final String worldName;
    private final Vector3d worldPosition;
    private final List<Ref<EntityStore>> castleLabelRefs;

    public CastleSiteVisualRefs(
            String worldName,
            Vector3d worldPosition,
            List<Ref<EntityStore>> castleLabelRefs
    ) {
        this.worldName = Objects.requireNonNull(worldName, "worldName");
        this.worldPosition = Objects.requireNonNull(worldPosition, "worldPosition");
        this.castleLabelRefs = castleLabelRefs == null ? List.of() : List.copyOf(castleLabelRefs);
    }

    public String worldName() {
        return worldName;
    }

    public Vector3d worldPosition() {
        return worldPosition;
    }

    public List<Ref<EntityStore>> allRefs() {
        List<Ref<EntityStore>> refs = new ArrayList<>();
        castleLabelRefs.forEach(ref -> addIfPresent(refs, ref));
        return List.copyOf(refs);
    }

    private void addIfPresent(List<Ref<EntityStore>> refs, Ref<EntityStore> ref) {
        if (ref != null) {
            refs.add(ref);
        }
    }
}
