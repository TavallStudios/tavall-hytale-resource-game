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
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Spawns non-NPC world labels backed by Hytale nameplates.
 */
public final class WorldLabelService implements IDependencyInjectableConcrete {
    private static final Logger LOGGER = Logger.getLogger(WorldLabelService.class.getName());
    private static final String DEFAULT_MARKER_MODEL_ID = "Objective_Location_Marker";
    private static final double DEFAULT_LINE_SPACING = 0.42D;

    private volatile Model markerModel;

    public Ref<EntityStore> spawnLabel(World world, Vector3d position, String text) {
        if (world == null || position == null || text == null || text.isBlank()) {
            return null;
        }
        return spawnSingleLabel(world, position, text);
    }

    public List<Ref<EntityStore>> spawnLabelStack(World world, Vector3d topPosition, List<String> lines) {
        if (world == null || topPosition == null || lines == null || lines.isEmpty()) {
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
        store.putComponent(ref, Nameplate.getComponentType(), new Nameplate(text));
    }

    private Ref<EntityStore> spawnSingleLabel(World world, Vector3d position, String text) {
        Store<EntityStore> store = world.getEntityStore().getStore();
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(position, Vector3f.ZERO));
        holder.ensureComponent(UUIDComponent.getComponentType());
        holder.ensureComponent(Intangible.getComponentType());
        holder.addComponent(Nameplate.getComponentType(), new Nameplate(text));
        Model model = resolveMarkerModel();
        if (model != null) {
            holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
            holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
        }
        return store.addEntity(holder, AddReason.SPAWN);
    }

    private Model resolveMarkerModel() {
        Model cached = markerModel;
        if (cached != null) {
            return cached;
        }
        ModelAsset asset = (ModelAsset) ModelAsset.getAssetMap().getAsset(DEFAULT_MARKER_MODEL_ID);
        if (asset == null) {
            LOGGER.warning(() -> "Unable to resolve default world-label model '" + DEFAULT_MARKER_MODEL_ID + "'. Labels will render nameplates only.");
            return null;
        }
        Model created = Model.createUnitScaleModel(asset);
        markerModel = created;
        return created;
    }
}
