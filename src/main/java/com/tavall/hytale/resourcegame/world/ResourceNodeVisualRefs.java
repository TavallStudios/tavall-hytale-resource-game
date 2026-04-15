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
    private final List<Ref<EntityStore>> workerRefs;

    public ResourceNodeVisualRefs(Ref<EntityStore> anchorRef, List<Ref<EntityStore>> workerRefs) {
        this.anchorRef = anchorRef;
        this.workerRefs = List.copyOf(workerRefs);
    }

    public boolean matches(Ref<EntityStore> targetRef) {
        if (targetRef == null) {
            return false;
        }
        if (Objects.equals(anchorRef, targetRef)) {
            return true;
        }
        return workerRefs.stream().anyMatch(targetRef::equals);
    }

    public List<Ref<EntityStore>> allRefs() {
        List<Ref<EntityStore>> refs = new ArrayList<>();
        if (anchorRef != null) {
            refs.add(anchorRef);
        }
        refs.addAll(workerRefs);
        return List.copyOf(refs);
    }
}
