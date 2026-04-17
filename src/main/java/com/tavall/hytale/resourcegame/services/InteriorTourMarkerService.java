package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.interior.InteriorLayout;
import com.tavall.hytale.resourcegame.interior.InteriorTourStop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Spawns and clears first-join interior tour labels without NPC placeholders.
 */
public final class InteriorTourMarkerService implements IDependencyInjectableConcrete {
    private static final Logger LOGGER = Logger.getLogger(InteriorTourMarkerService.class.getName());

    private final WorldLabelService worldLabelService;
    private final Map<UUID, List<Ref<EntityStore>>> markerRefs = new ConcurrentHashMap<>();

    public InteriorTourMarkerService(WorldLabelService worldLabelService) {
        this.worldLabelService = worldLabelService;
    }

    public void ensureTourMarkers(UUID playerId, World world, InteriorLayout layout, boolean tutorialPending) {
        if (!tutorialPending) {
            clearTourMarkers(playerId);
            return;
        }

        List<Ref<EntityStore>> existing = markerRefs.get(playerId);
        if (existing != null && existing.stream().allMatch(ref -> ref != null && ref.isValid())) {
            return;
        }

        clearTourMarkers(playerId);
        List<Ref<EntityStore>> refs = new ArrayList<>();
        for (InteriorTourStop stop : layout.tourStops()) {
            Ref<EntityStore> ref = worldLabelService.spawnLabel(world, stop.position().add(0.0D, 1.8D, 0.0D), stop.displayLabel());
            if (ref != null && ref.isValid()) {
                refs.add(ref);
            }
        }
        markerRefs.put(playerId, List.copyOf(refs));
        LOGGER.info(() -> String.format(
                "Interior tour labels ready for %s in world %s. stops=%s",
                playerId,
                world.getName(),
                refs.size()
        ));
    }

    public void clearTourMarkers(UUID playerId) {
        List<Ref<EntityStore>> refs = markerRefs.remove(playerId);
        if (refs == null) {
            return;
        }
        for (Ref<EntityStore> ref : refs) {
            if (ref != null && ref.isValid()) {
                ref.getStore().removeEntity(ref, RemoveReason.REMOVE);
            }
        }
    }
}
