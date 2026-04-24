package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlacementInteractionService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlacementModeService;
import com.tavall.hytale.resourcegame.domain.PlacementResult;

import java.util.Objects;

/**
 * Consumes world interaction while placement mode is armed.
 */
public final class PlacementInteractionService implements IPlacementInteractionService, IDependencyInjectableConcrete {
    private final IPlacementModeService placementModeService;

    public PlacementInteractionService(IPlacementModeService placementModeService) {
        this.placementModeService = Objects.requireNonNull(placementModeService, "placementModeService");
    }

    @Override
    public void handleInteract(PlayerInteractEvent event) {
        if (event.isCancelled()) {
            return;
        }
        PlacementResult result = placementModeService.confirmPlacement(event.getPlayer(), event.getTargetBlock());
        if (!result.handled()) {
            return;
        }
        event.setCancelled(true);
        if (!result.message().isBlank()) {
            event.getPlayer().sendMessage(Message.raw(result.message()).color(result.success() ? "green" : "red"));
        }
    }
}
