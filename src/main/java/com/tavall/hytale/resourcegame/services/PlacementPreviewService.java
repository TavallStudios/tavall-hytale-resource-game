package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.tavall.hytale.resourcegame.config.PopulationDisplayConfig;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlacementPreviewService;
import com.tavall.hytale.resourcegame.domain.PlacementModeType;
import com.tavall.hytale.resourcegame.domain.PlacementRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Draws temporary in-world preview markers for armed placements.
 */
public final class PlacementPreviewService implements IPlacementPreviewService, IDependencyInjectableConcrete {
    private static final Logger LOGGER = Logger.getLogger(PlacementPreviewService.class.getName());

    private final PopulationDisplayConfig displayConfig;
    private final NpcVisualSpawner npcVisualSpawner;
    private final Map<UUID, List<Ref<EntityStore>>> previewRefs = new ConcurrentHashMap<>();

    public PlacementPreviewService(PopulationDisplayConfig displayConfig, NpcVisualSpawner npcVisualSpawner) {
        this.displayConfig = displayConfig;
        this.npcVisualSpawner = npcVisualSpawner;
    }

    @Override
    public void showPreview(Player player, PlacementRequest request, Vector3i targetBlock) {
        if (player == null || request == null || targetBlock == null) {
            return;
        }
        player.getWorld().execute(() -> {
            clearPreview(player.getUuid());
            Store<EntityStore> store = player.getWorld().getEntityStore().getStore();
            int roleIndex = NPCPlugin.get().getIndex(displayConfig.npcRoleName());
            if (roleIndex < 0) {
                LOGGER.warning(() -> "Unable to render placement preview because NPC role '" + displayConfig.npcRoleName() + "' was not found.");
                return;
            }
            Vector3d previewBase = new Vector3d(targetBlock.getX() + 0.5D, targetBlock.getY() + 1.0D, targetBlock.getZ() + 0.5D);
            List<Ref<EntityStore>> refs = new ArrayList<>();
            refs.add(npcVisualSpawner.spawnNamed(store, roleIndex, previewBase.add(0.0D, 1.4D, 0.0D), previewLabel(request), anchorScale(request)));
            refs.addAll(npcVisualSpawner.spawnGroup(store, roleIndex, previewPositions(previewBase, request.modeType()), previewCount(request.modeType()), ghostScale(request.modeType())));
            previewRefs.put(player.getUuid(), refs.stream().filter(ref -> ref != null && ref.isValid()).toList());
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
        if (request.modeType() == PlacementModeType.CASTLE) {
            return "Castle Preview | Click ground to place";
        }
        return request.resourceType() + " Node Preview | Click ground to place";
    }

    private float anchorScale(PlacementRequest request) {
        return request.modeType() == PlacementModeType.CASTLE ? 1.9F : 1.35F;
    }

    private float ghostScale(PlacementModeType modeType) {
        return modeType == PlacementModeType.CASTLE ? 0.95F : 0.75F;
    }

    private int previewCount(PlacementModeType modeType) {
        return modeType == PlacementModeType.CASTLE ? 4 : 3;
    }

    private List<Vector3d> previewPositions(Vector3d base, PlacementModeType modeType) {
        if (modeType == PlacementModeType.CASTLE) {
            return List.of(
                    new Vector3d(base.getX() + 2.5D, base.getY(), base.getZ()),
                    new Vector3d(base.getX() - 2.5D, base.getY(), base.getZ()),
                    new Vector3d(base.getX(), base.getY(), base.getZ() + 2.5D),
                    new Vector3d(base.getX(), base.getY(), base.getZ() - 2.5D)
            );
        }
        return List.of(
                new Vector3d(base.getX() + 1.2D, base.getY(), base.getZ()),
                new Vector3d(base.getX() - 1.2D, base.getY(), base.getZ()),
                new Vector3d(base.getX(), base.getY(), base.getZ() + 1.2D)
        );
    }
}
