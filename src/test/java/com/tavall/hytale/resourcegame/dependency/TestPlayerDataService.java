package com.tavall.hytale.resourcegame.dependency;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerDataService;
import com.tavall.hytale.resourcegame.services.PlayerSession;

import java.util.concurrent.CompletableFuture;

/**
 * Test stub for player data access.
 */
public final class TestPlayerDataService implements IPlayerDataService {
    @Override
    public void handlePlayerReady(PlayerReadyEvent event) {
    }

    @Override
    public void handlePlayerDisconnect(PlayerDisconnectEvent event) {
    }

    @Override
    public CompletableFuture<PlayerSession> ensureSession(Player player) {
        return CompletableFuture.completedFuture(null);
    }
}