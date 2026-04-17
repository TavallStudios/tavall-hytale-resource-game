package com.tavall.hytale.resourcegame.world;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Tracks a single placed resource node anchor plus its visible worker refs.
 */
public final class ResourceNodeVisualRefs {
    private final String worldName;
    private final Vector3d worldPosition;
    private final List<Ref<EntityStore>> anchorRefs;

    public ResourceNodeVisualRefs(
            String worldName,
            Vector3d worldPosition,
            List<Ref<EntityStore>> anchorRefs
    ) {
        this.worldName = Objects.requireNonNull(worldName, "worldName");
        this.worldPosition = Objects.requireNonNull(worldPosition, "worldPosition");
        this.anchorRefs = anchorRefs == null ? List.of() : List.copyOf(anchorRefs);
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
        return anchorRefs.stream().anyMatch(ref -> Objects.equals(ref, targetRef));
    }

    public List<Ref<EntityStore>> allRefs() {
        List<Ref<EntityStore>> refs = new ArrayList<>();
        refs.addAll(anchorRefs);
        return List.copyOf(refs);
    }
}
