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
    private final Ref<EntityStore> anchorRef;
    private final List<Ref<EntityStore>> crewRefs;
    private final List<Ref<EntityStore>> scaffoldRefs;

    public CastleBuildingVisualRefs(
            String worldName,
            Vector3d worldPosition,
            Ref<EntityStore> anchorRef,
            List<Ref<EntityStore>> crewRefs,
            List<Ref<EntityStore>> scaffoldRefs
    ) {
        this.worldName = Objects.requireNonNull(worldName, "worldName");
        this.worldPosition = Objects.requireNonNull(worldPosition, "worldPosition");
        this.anchorRef = anchorRef;
        this.crewRefs = List.copyOf(crewRefs);
        this.scaffoldRefs = List.copyOf(scaffoldRefs);
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
        if (Objects.equals(anchorRef, targetRef)) {
            return true;
        }
        return crewRefs.stream().anyMatch(targetRef::equals) || scaffoldRefs.stream().anyMatch(targetRef::equals);
    }

    public List<Ref<EntityStore>> allRefs() {
        List<Ref<EntityStore>> refs = new ArrayList<>();
        if (anchorRef != null) {
            refs.add(anchorRef);
        }
        refs.addAll(crewRefs);
        refs.addAll(scaffoldRefs);
        return List.copyOf(refs);
    }
}
