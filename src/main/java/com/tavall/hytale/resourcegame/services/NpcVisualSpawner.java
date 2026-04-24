package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.Frozen;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import it.unimi.dsi.fastutil.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared NPC marker spawner for readable in-world prototype visuals.
 */
public final class NpcVisualSpawner implements IDependencyInjectableConcrete {
    public Ref<EntityStore> spawnNamed(Store<EntityStore> store, int roleIndex, Vector3d position, String label, float scale) {
        Pair<Ref<EntityStore>, ?> pair = NPCPlugin.get().spawnEntity(store, roleIndex, position, Vector3f.ZERO, null, null);
        Ref<EntityStore> ref = pair.first();
        if (ref != null && ref.isValid()) {
            store.putComponent(ref, DisplayNameComponent.getComponentType(), new DisplayNameComponent(Message.raw(label)));
            store.putComponent(ref, Nameplate.getComponentType(), new Nameplate(label));
            store.ensureComponent(ref, Frozen.getComponentType());
            applyScale(store, ref, scale);
        }
        return ref;
    }

    public List<Ref<EntityStore>> spawnGroup(Store<EntityStore> store, int roleIndex, List<Vector3d> positions, int visibleCount, float scale) {
        List<Ref<EntityStore>> refs = new ArrayList<>();
        for (int index = 0; index < visibleCount && index < positions.size(); index++) {
            Pair<Ref<EntityStore>, ?> pair = NPCPlugin.get().spawnEntity(store, roleIndex, positions.get(index), Vector3f.ZERO, null, null);
            Ref<EntityStore> ref = pair.first();
            if (ref != null && ref.isValid()) {
                store.ensureComponent(ref, Frozen.getComponentType());
                applyScale(store, ref, scale);
                refs.add(ref);
            }
        }
        return List.copyOf(refs);
    }

    public void applyScale(Store<EntityStore> store, Ref<EntityStore> ref, float scale) {
        if (ref == null || !ref.isValid()) {
            return;
        }
        float safeScale = Math.max(0.35F, scale);
        store.putComponent(ref, EntityScaleComponent.getComponentType(), new EntityScaleComponent(safeScale));
    }
}
