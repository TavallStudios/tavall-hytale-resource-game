package com.tavall.hytale.resourcegame.world;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Tracks world refs for a single rendered building scene.
 */
public final class CastleBuildingVisualRefs {
    private final String worldName;
    private final Vector3d worldPosition;
    private final List<Ref<EntityStore>> labelRefs;

    public CastleBuildingVisualRefs(
            String worldName,
            Vector3d worldPosition,
            List<Ref<EntityStore>> labelRefs
    ) {
        this.worldName = Objects.requireNonNull(worldName, "worldName");
        this.worldPosition = Objects.requireNonNull(worldPosition, "worldPosition");
        this.labelRefs = labelRefs == null ? List.of() : List.copyOf(labelRefs);
    }

    public String worldName() {
        return worldName;
    }

    public Vector3d worldPosition() {
        return worldPosition;
    }

    public boolean matches(Ref<EntityStore> targetRef) {
        if (targetRef == null) {
            return false;
        }
        return labelRefs.stream().anyMatch(ref -> Objects.equals(ref, targetRef));
    }

    public List<Ref<EntityStore>> allRefs() {
        List<Ref<EntityStore>> refs = new ArrayList<>();
        refs.addAll(labelRefs);
        return List.copyOf(refs);
    }
}
