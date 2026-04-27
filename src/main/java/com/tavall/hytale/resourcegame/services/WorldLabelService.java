package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.Frozen;
import com.hypixel.hytale.server.core.modules.entity.component.AudioComponent;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.MovementAudioComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Spawns non-NPC world labels backed by Hytale nameplates.
 */
public final class WorldLabelService implements IDependencyInjectableConcrete {
    private static final Logger LOGGER = Logger.getLogger(WorldLabelService.class.getName());
    private static final String DEFAULT_MARKER_MODEL_ID = "Objective_Location_Marker";
    private static final List<String> MARKER_MODEL_CANDIDATES = List.of(
            "Objective_Location_Marker",
            "resource_game:props/hologram_pedestal",
            "resource_game:hologram_pedestal",
            "resource_game:building_marker"
    );
    private static final double DEFAULT_LINE_SPACING = 0.42D;

    private volatile Model markerModel;
    private volatile boolean markerModelLookupComplete;

    public Ref<EntityStore> spawnLabel(World world, Vector3d position, String text) {
        if (world == null || position == null || text == null || text.isBlank()) {
            LOGGER.warning(() -> "Skipping world label spawn because world, position, or text is missing.");
            return null;
        }
        return spawnSingleLabel(world, position, text);
    }

    public List<Ref<EntityStore>> spawnLabelStack(World world, Vector3d topPosition, List<String> lines) {
        if (world == null || topPosition == null || lines == null || lines.isEmpty()) {
            LOGGER.warning(() -> "Skipping world label stack spawn because world, position, or lines are missing.");
            return List.of();
        }
        List<Ref<EntityStore>> refs = new ArrayList<>();
        double currentY = topPosition.getY();
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            Ref<EntityStore> ref = spawnSingleLabel(world, new Vector3d(topPosition.getX(), currentY, topPosition.getZ()), line);
            if (ref != null) {
                refs.add(ref);
            }
            currentY -= DEFAULT_LINE_SPACING;
        }
        return List.copyOf(refs);
    }

    public void updateLabel(Ref<EntityStore> ref, String text) {
        if (ref == null || !ref.isValid() || text == null || text.isBlank()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        try {
            store.putComponent(ref, DisplayNameComponent.getComponentType(), new DisplayNameComponent(Message.raw(text)));
            store.putComponent(ref, Nameplate.getComponentType(), new Nameplate(text));
            store.ensureComponent(ref, Frozen.getComponentType());
            store.ensureComponent(ref, Intangible.getComponentType());
            store.ensureComponent(ref, Invulnerable.getComponentType());
            silenceEntity(store, ref);
            LOGGER.info(() -> "Updated world label nameplate text to '" + text + "'.");
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Failed to update world label text.", ex);
        }
    }

    private Ref<EntityStore> spawnSingleLabel(World world, Vector3d position, String text) {
        Model model = resolveMarkerModel();
        try {
            Store<EntityStore> store = world.getEntityStore().getStore();
            Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
            holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(position, Vector3f.ZERO));
            holder.ensureComponent(UUIDComponent.getComponentType());
            holder.ensureComponent(Intangible.getComponentType());
            holder.ensureComponent(Invulnerable.getComponentType());
            holder.ensureComponent(Frozen.getComponentType());
            holder.addComponent(DisplayNameComponent.getComponentType(), new DisplayNameComponent(Message.raw(text)));
            holder.addComponent(Nameplate.getComponentType(), new Nameplate(text));
            if (model != null) {
                holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
                holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
            }
            Ref<EntityStore> ref = store.addEntity(holder, AddReason.SPAWN);
            LOGGER.info(() -> "Spawned world label '" + text + "' at "
                    + position.getX() + ", " + position.getY() + ", " + position.getZ()
                    + " with " + (model == null ? "nameplate-only fallback" : "marker model") + ".");
            return ref;
        } catch (RuntimeException ex) {
            LOGGER.log(Level.SEVERE, "Failed to spawn world label '" + text + "'.", ex);
            return null;
        }
    }

    private Model resolveMarkerModel() {
        if (markerModelLookupComplete) {
            return markerModel;
        }
        Model cached = markerModel;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (markerModelLookupComplete) {
                return markerModel;
            }
            for (String candidateId : MARKER_MODEL_CANDIDATES) {
                ModelAsset asset = (ModelAsset) ModelAsset.getAssetMap().getAsset(candidateId);
                if (asset == null) {
                    continue;
                }
                Model created = Model.createUnitScaleModel(asset);
                markerModel = created;
                markerModelLookupComplete = true;
                LOGGER.info(() -> "Resolved world-label marker model '" + candidateId + "'.");
                return created;
            }
            markerModel = null;
            markerModelLookupComplete = false;
            LOGGER.warning(() -> "Unable to resolve any world-label marker model from " + MARKER_MODEL_CANDIDATES
                    + ". Falling back to a nameplate-only hologram entity.");
            return null;
        }
    }

    private void silenceEntity(Store<EntityStore> store, Ref<EntityStore> ref) {
        if (store == null || ref == null) {
            return;
        }
        try {
            store.removeComponent(ref, AudioComponent.getComponentType());
        } catch (IllegalArgumentException ignored) {
        }
        try {
            store.removeComponent(ref, MovementAudioComponent.getComponentType());
        } catch (IllegalArgumentException ignored) {
        }
    }
}
