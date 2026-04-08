package com.tavall.hytale.resourcegame.population;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Holds anchor entity references for population displays.
 */
public final class PopulationDisplayRefs {
    private final Ref<EntityStore> citizensRef;
    private final Ref<EntityStore> troopsRef;

    public PopulationDisplayRefs(Ref<EntityStore> citizensRef, Ref<EntityStore> troopsRef) {
        this.citizensRef = citizensRef;
        this.troopsRef = troopsRef;
    }

    public Ref<EntityStore> citizensRef() {
        return citizensRef;
    }

    public Ref<EntityStore> troopsRef() {
        return troopsRef;
    }
}
