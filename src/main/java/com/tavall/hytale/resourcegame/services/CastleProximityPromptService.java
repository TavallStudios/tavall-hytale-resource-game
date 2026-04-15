package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleInteractionService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleProximityPromptService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlacementModeService;

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
        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) {
                focusedPlayers.remove(playerRef.getUuid());
                continue;
            }
            ref.getStore().getExternalData().getWorld().execute(() -> evaluateFocus(playerRef));
        }
    }

    private void evaluateFocus(PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            focusedPlayers.remove(playerRef.getUuid());
            return;
        }
        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        if (player == null) {
            focusedPlayers.remove(playerRef.getUuid());
            return;
        }
        UUID playerId = player.getUuid();
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
