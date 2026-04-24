package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleInteractionService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleProximityPromptService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlacementModeService;
import com.tavall.hytale.resourcegame.tasks.WorldTasks;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Tracks near-and-looking castle focus without auto-opening UI.
 */
public final class CastleProximityPromptService implements ICastleProximityPromptService, IDependencyInjectableConcrete {
    private static final long SCAN_INTERVAL_MILLIS = 250L;

    private final ICastleInteractionService castleInteractionService;
    private final IPlacementModeService placementModeService;
    private final Set<UUID> focusedPlayers = ConcurrentHashMap.newKeySet();
    private ScheduledFuture<?> scanTask;

    public CastleProximityPromptService(
            ICastleInteractionService castleInteractionService,
            IPlacementModeService placementModeService
    ) {
        this.castleInteractionService = Objects.requireNonNull(castleInteractionService, "castleInteractionService");
        this.placementModeService = Objects.requireNonNull(placementModeService, "placementModeService");
    }

    public void start() {
        if (scanTask != null) {
            return;
        }
        scanTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                this::scanPlayers,
                SCAN_INTERVAL_MILLIS,
                SCAN_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS
        );
    }

    public void shutdown() {
        if (scanTask != null) {
            scanTask.cancel(false);
            scanTask = null;
        }
        focusedPlayers.clear();
    }

    private void scanPlayers() {
        try {
            for (PlayerRef playerRef : Universe.get().getPlayers()) {
                UUID playerId = playerRef.getUuid();
                Ref<EntityStore> ref = playerRef.getReference();
                if (ref == null || !ref.isValid()) {
                    focusedPlayers.remove(playerId);
                    continue;
                }
                var store = ref.getStore();
                if (!(store.getExternalData() instanceof EntityStore entityStore)) {
                    focusedPlayers.remove(playerId);
                    continue;
                }
                World world = entityStore.getWorld();
                if (world == null) {
                    focusedPlayers.remove(playerId);
                    continue;
                }
                WorldTasks.executeSafe(world, "CastleProximityPromptService.evaluateFocus", () -> evaluateFocus(ref, playerId));
            }
        } catch (Throwable throwable) {
            // Never let proximity scanning crash the scheduler thread or a world thread.
        }
    }

    private void evaluateFocus(Ref<EntityStore> ref, UUID playerId) {
        if (playerId == null || ref == null || !ref.isValid()) {
            if (playerId != null) {
                focusedPlayers.remove(playerId);
            }
            return;
        }
        Player player;
        try {
            player = ref.getStore().getComponent(ref, Player.getComponentType());
        } catch (Throwable throwable) {
            focusedPlayers.remove(playerId);
            return;
        }
        if (player == null) {
            focusedPlayers.remove(playerId);
            return;
        }
        if (placementModeService.shouldSuppressPrompts(playerId, java.time.Instant.now())) {
            focusedPlayers.remove(playerId);
            return;
        }
        boolean focused = castleInteractionService.isPlayerFocusingOwnedCastle(player);
        if (!focused) {
            focusedPlayers.remove(playerId);
            return;
        }
        focusedPlayers.add(playerId);
    }
}
