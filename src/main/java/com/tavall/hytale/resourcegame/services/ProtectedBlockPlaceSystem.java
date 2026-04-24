package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Cancels manual placement into protected gameplay structure space.
 */
public final class ProtectedBlockPlaceSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
    private final StructureProtectionService protectionService;

    public ProtectedBlockPlaceSystem(StructureProtectionService protectionService) {
        super(PlaceBlockEvent.class);
        this.protectionService = protectionService;
    }

    @Override
    public void handle(
            int index,
            ArchetypeChunk<EntityStore> archetypeChunk,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer,
            PlaceBlockEvent event
    ) {
        World world = ((EntityStore) store.getExternalData()).getWorld();
        if (world == null) {
            return;
        }
        if (protectionService.isPlacementRestricted(world.getName(), event.getTargetBlock())) {
            event.setCancelled(true);
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
