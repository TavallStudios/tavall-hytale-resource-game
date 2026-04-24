package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;

/**
 * Handles interaction-driven placement confirmation.
 */
public interface IPlacementInteractionService extends IDependencyInjectableInterface {
    void handleInteract(PlayerInteractEvent event);
}
