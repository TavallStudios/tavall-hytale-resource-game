package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlacementPreviewService;
import com.tavall.hytale.resourcegame.domain.PlacementRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws temporary world labels for armed placements without spawning preview NPCs.
 */
public final class PlacementPreviewService implements IPlacementPreviewService, IDependencyInjectableConcrete {
    private final WorldLabelService worldLabelService;
    private final Map<UUID, List<Ref<EntityStore>>> previewRefs = new ConcurrentHashMap<>();

    public PlacementPreviewService(WorldLabelService worldLabelService) {
        this.worldLabelService = worldLabelService;
    }

    @Override
    public void showPreview(Player player, PlacementRequest request, Vector3i targetBlock) {
        if (player == null || request == null || targetBlock == null || player.getWorld() == null) {
            return;
        }
        player.getWorld().execute(() -> {
            clearPreview(player.getUuid());
            Vector3d previewBase = new Vector3d(targetBlock.getX() + 0.5D, targetBlock.getY() + 2.8D, targetBlock.getZ() + 0.5D);
            Ref<EntityStore> labelRef = worldLabelService.spawnLabel(player.getWorld(), previewBase, previewLabel(request));
            if (labelRef != null && labelRef.isValid()) {
                previewRefs.put(player.getUuid(), List.of(labelRef));
            }
        });
    }

    @Override
    public void clearPreview(UUID playerId) {
        List<Ref<EntityStore>> refs = previewRefs.remove(playerId);
        if (refs == null) {
            return;
        }
        for (Ref<EntityStore> ref : refs) {
            if (ref != null && ref.isValid()) {
                ref.getStore().removeEntity(ref, RemoveReason.REMOVE);
            }
        }
    }

    private String previewLabel(PlacementRequest request) {
        if (request.modeType() == com.tavall.hytale.resourcegame.domain.PlacementModeType.CASTLE) {
            return "Castle Preview | /kd place confirm";
        }
        if (request.modeType() == com.tavall.hytale.resourcegame.domain.PlacementModeType.BUILDING) {
            return (request.buildingType() == null ? "Building" : request.buildingType().displayName()) + " Preview | /kd place confirm";
        }
        return request.resourceType() + " Node Preview | /kd place confirm";
    }
}
