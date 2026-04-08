package com.tavall.hytale.resourcegame.services;

import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.ResourceInventory;
import com.tavall.hytale.resourcegame.resources.ResourceType;

import com.tavall.hytale.resourcegame.tasks.AsyncTask;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Mutates resources for a player session.
 */
public final class ResourceService {
    private final PlayerSessionStore sessionStore;
    private final PlayerGameStateService gameStateService;

    public ResourceService(PlayerSessionStore sessionStore, PlayerGameStateService gameStateService) {
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.gameStateService = Objects.requireNonNull(gameStateService, "gameStateService");
    }

    public PlayerGameState addResource(UUID playerId, ResourceType type, int amount) {
        return updateResource(playerId, type, currentValue(playerId, type) + amount);
    }

    public PlayerGameState setResource(UUID playerId, ResourceType type, int amount) {
        return updateResource(playerId, type, amount);
    }

    private int currentValue(UUID playerId, ResourceType type) {
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return 0;
        }
        ResourceInventory resources = session.gameState().resources();
        return switch (type) {
            case FOOD -> resources.food();
            case WOOD -> resources.wood();
            case IRON -> resources.iron();
        };
    }

    private PlayerGameState updateResource(UUID playerId, ResourceType type, int amount) {
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return null;
        }
        ResourceInventory resources = session.gameState().resources();
        ResourceInventory updated = switch (type) {
            case FOOD -> resources.withFood(amount);
            case WOOD -> resources.withWood(amount);
            case IRON -> resources.withIron(amount);
        };
        PlayerGameState updatedState = session.gameState().withResources(updated, Instant.now());
        session.updateGameState(updatedState);
        gameStateService.cacheState(playerId, updatedState);
        AsyncTask.runAsync(() -> gameStateService.persistState(updatedState, Instant.now()));
        return updatedState;
    }
}
