package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.tavall.hytale.resourcegame.config.PopulationDisplayConfig;
import com.tavall.hytale.resourcegame.interior.InteriorLayout;
import com.tavall.hytale.resourcegame.population.PopulationDisplayRefs;
import com.tavall.hytale.resourcegame.domain.PopulationSummary;
import it.unimi.dsi.fastutil.Pair;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Spawns and updates population display entities inside interiors.
 */
public final class PopulationDisplayService implements PopulationDisplayGateway {
    private static final Logger LOGGER = Logger.getLogger(PopulationDisplayService.class.getName());
    private final PopulationDisplayConfig displayConfig;
    private final Map<UUID, PopulationDisplayRefs> displayRefs = new ConcurrentHashMap<>();

    public PopulationDisplayService(PopulationDisplayConfig displayConfig) {
        this.displayConfig = displayConfig;
    }

    @Override
    public void ensureDisplays(UUID playerId, World world, InteriorLayout layout, PopulationSummary summary) {
        PopulationDisplayRefs existing = displayRefs.get(playerId);
        if (existing != null && existing.citizensRef() != null && existing.troopsRef() != null) {
            updateDisplays(playerId, summary);
            return;
        }
        Store<EntityStore> store = world.getEntityStore().getStore();
        Ref<EntityStore> citizens = spawnAnchor(store, layout.citizenAnchor(), displayConfig.citizenLabel(), summary.citizenCount());
        Ref<EntityStore> troops = spawnAnchor(store, layout.troopAnchor(), displayConfig.troopLabel(), summary.troopCount());
        displayRefs.put(playerId, new PopulationDisplayRefs(citizens, troops));
        LOGGER.info(() -> String.format(
                "Population displays ready for %s in world %s. citizens=%s troops=%s",
                playerId,
                world.getName(),
                summary.citizenCount(),
                summary.troopCount()
        ));
    }

    @Override
    public void updateDisplays(UUID playerId, PopulationSummary summary) {
        PopulationDisplayRefs refs = displayRefs.get(playerId);
        if (refs == null) {
            return;
        }
        updateDisplay(refs.citizensRef(), displayConfig.citizenLabel(), summary.citizenCount());
        updateDisplay(refs.troopsRef(), displayConfig.troopLabel(), summary.troopCount());
        LOGGER.info(() -> String.format(
                "Population displays updated for %s. citizens=%s troops=%s",
                playerId,
                summary.citizenCount(),
                summary.troopCount()
        ));
    }

    private Ref<EntityStore> spawnAnchor(Store<EntityStore> store, Vector3d position, String label, int count) {
        NPCPlugin npcPlugin = NPCPlugin.get();
        int roleIndex = new NpcRoleResolver().resolveRoleIndex(displayConfig.npcRoleName());
        if (roleIndex < 0) {
            return null;
        }
        Pair<Ref<EntityStore>, NPCEntity> pair = npcPlugin.spawnEntity(store, roleIndex, position, Vector3f.ZERO, null, null);
        Ref<EntityStore> ref = pair.first();
        if (ref != null && ref.isValid()) {
            store.putComponent(ref, DisplayNameComponent.getComponentType(), new DisplayNameComponent(Message.raw(label + ": " + count)));
        }
        return ref;
    }

    private void updateDisplay(Ref<EntityStore> ref, String label, int count) {
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        store.putComponent(ref, DisplayNameComponent.getComponentType(), new DisplayNameComponent(Message.raw(label + ": " + count)));
    }
}
