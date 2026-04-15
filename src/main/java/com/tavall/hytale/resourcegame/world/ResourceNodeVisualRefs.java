package com.tavall.hytale.resourcegame.world;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Tracks a single placed resource node anchor plus its visible worker refs.
 */
public final class ResourceNodeVisualRefs {
    private final Ref<EntityStore> anchorRef;
    private final Ref<EntityStore> routeAnchorRef;
    private final List<Ref<EntityStore>> workerRefs;
    private final List<Ref<EntityStore>> routeRefs;

    public ResourceNodeVisualRefs(
            Ref<EntityStore> anchorRef,
            Ref<EntityStore> routeAnchorRef,
            List<Ref<EntityStore>> workerRefs,
            List<Ref<EntityStore>> routeRefs
    ) {
        this.anchorRef = anchorRef;
        this.routeAnchorRef = routeAnchorRef;
        this.workerRefs = List.copyOf(workerRefs);
        this.routeRefs = List.copyOf(routeRefs);
    }

    public boolean matches(Ref<EntityStore> targetRef) {
        if (targetRef == null) {
            return false;
        }
        if (Objects.equals(anchorRef, targetRef)) {
            return true;
        }
        if (Objects.equals(routeAnchorRef, targetRef)) {
            return true;
        }
        return workerRefs.stream().anyMatch(targetRef::equals) || routeRefs.stream().anyMatch(targetRef::equals);
    }

    public List<Ref<EntityStore>> allRefs() {
        List<Ref<EntityStore>> refs = new ArrayList<>();
        if (anchorRef != null) {
            refs.add(anchorRef);
        }
        if (routeAnchorRef != null) {
            refs.add(routeAnchorRef);
        }
        refs.addAll(workerRefs);
        refs.addAll(routeRefs);
        return List.copyOf(refs);
    }
}
