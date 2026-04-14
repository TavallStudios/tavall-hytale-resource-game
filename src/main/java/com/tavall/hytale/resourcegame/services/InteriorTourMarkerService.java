package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.tavall.hytale.resourcegame.config.PopulationDisplayConfig;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.interior.InteriorLayout;
import com.tavall.hytale.resourcegame.interior.InteriorTourStop;
import it.unimi.dsi.fastutil.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Spawns and clears the first-join interior tour markers.
 */
public final class InteriorTourMarkerService implements IDependencyInjectableConcrete {
    private static final Logger LOGGER = Logger.getLogger(InteriorTourMarkerService.class.getName());

    private final PopulationDisplayConfig displayConfig;
    private final Map<UUID, List<Ref<EntityStore>>> markerRefs = new ConcurrentHashMap<>();

    public InteriorTourMarkerService(PopulationDisplayConfig displayConfig) {
        this.displayConfig = displayConfig;
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
        Store<EntityStore> store = world.getEntityStore().getStore();
        NPCPlugin npcPlugin = NPCPlugin.get();
        int roleIndex = npcPlugin.getIndex(displayConfig.npcRoleName());
        if (roleIndex < 0) {
            LOGGER.warning(() -> "Unable to spawn interior tour markers because NPC role '" + displayConfig.npcRoleName() + "' was not found.");
            return;
        }

        List<Ref<EntityStore>> refs = new ArrayList<>();
        for (InteriorTourStop stop : layout.tourStops()) {
            Pair<Ref<EntityStore>, ?> pair = npcPlugin.spawnEntity(store, roleIndex, stop.position(), Vector3f.ZERO, null, null);
            Ref<EntityStore> ref = pair.first();
            if (ref != null && ref.isValid()) {
                store.putComponent(ref, DisplayNameComponent.getComponentType(), new DisplayNameComponent(Message.raw(stop.displayLabel())));
                refs.add(ref);
            }
        }
        markerRefs.put(playerId, List.copyOf(refs));
        LOGGER.info(() -> String.format(
                "Interior tour markers ready for %s in world %s. stops=%s",
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
