package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlacementPreviewService;
import com.tavall.hytale.resourcegame.domain.PlacementRequest;
import com.tavall.hytale.resourcegame.tasks.WorldTasks;

import java.util.ArrayList;
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
        WorldTasks.executeSafe(player.getWorld(), "PlacementPreviewService.showPreview", () -> {
            clearPreview(player.getUuid());
            List<Ref<EntityStore>> refs = new ArrayList<>();
            Vector3d previewBase = new Vector3d(targetBlock.getX() + 0.5D, targetBlock.getY() + 2.8D, targetBlock.getZ() + 0.5D);
            Ref<EntityStore> labelRef = worldLabelService.spawnLabel(player.getWorld(), previewBase, previewLabel(request));
            if (labelRef != null && labelRef.isValid()) {
                refs.add(labelRef);
            }
            if (request.modeType() == com.tavall.hytale.resourcegame.domain.PlacementModeType.BUILDING) {
                refs.addAll(spawnBuildingSelectionMarkers(player, targetBlock));
            }
            if (!refs.isEmpty()) {
                previewRefs.put(player.getUuid(), List.copyOf(refs));
            }
        });
    }

    @Override
    public void clearPreview(UUID playerId) {
        List<Ref<EntityStore>> refs = previewRefs.remove(playerId);
        if (refs == null) {
            return;
        }
        refs.forEach(this::removeSafely);
    }

    private void removeSafely(Ref<EntityStore> ref) {
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        Runnable remove = () -> {
            if (ref.isValid()) {
                store.removeEntity(ref, RemoveReason.REMOVE);
            }
        };
        if (store.getExternalData() instanceof EntityStore entityStore) {
            World world = entityStore.getWorld();
            if (world != null) {
                WorldTasks.executeSafe(world, "PlacementPreviewService.removeSafely", remove);
                return;
            }
        }
        remove.run();
    }

    private String previewLabel(PlacementRequest request) {
        if (request.modeType() == com.tavall.hytale.resourcegame.domain.PlacementModeType.CASTLE) {
            return "Castle Preview | /kd place confirm";
        }
        if (request.modeType() == com.tavall.hytale.resourcegame.domain.PlacementModeType.BUILDING) {
            return (request.buildingType() == null ? "Building" : request.buildingType().displayName())
                    + " Preview | /kd place move <dx> <dz> | /kd place confirm";
        }
        return request.resourceType() + " Node Preview | /kd place confirm";
    }

    private List<Ref<EntityStore>> spawnBuildingSelectionMarkers(Player player, Vector3i targetBlock) {
        if (player == null || player.getWorld() == null || targetBlock == null) {
            return List.of();
        }
        int halfSize = 2;
        double centerX = targetBlock.getX() + 0.5D;
        double centerZ = targetBlock.getZ() + 0.5D;
        double baseY = targetBlock.getY() + 1.2D;
        double topY = baseY + 3.0D;
        String marker = "■";
        List<Ref<EntityStore>> refs = new ArrayList<>();
        refs.add(markerRef(player, centerX - halfSize, baseY, centerZ - halfSize, marker));
        refs.add(markerRef(player, centerX - halfSize, baseY, centerZ + halfSize, marker));
        refs.add(markerRef(player, centerX + halfSize, baseY, centerZ - halfSize, marker));
        refs.add(markerRef(player, centerX + halfSize, baseY, centerZ + halfSize, marker));
        refs.add(markerRef(player, centerX - halfSize, topY, centerZ - halfSize, marker));
        refs.add(markerRef(player, centerX - halfSize, topY, centerZ + halfSize, marker));
        refs.add(markerRef(player, centerX + halfSize, topY, centerZ - halfSize, marker));
        refs.add(markerRef(player, centerX + halfSize, topY, centerZ + halfSize, marker));
        refs.removeIf(ref -> ref == null || !ref.isValid());
        return List.copyOf(refs);
    }

    private Ref<EntityStore> markerRef(Player player, double x, double y, double z, String text) {
        return worldLabelService.spawnLabel(player.getWorld(), new Vector3d(x, y, z), text);
    }
}
