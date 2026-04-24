package com.tavall.hytale.resourcegame.support;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeVisualService;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RecordingResourceNodeVisualService implements IResourceNodeVisualService {
    private final Map<UUID, PlayerGameState> lastState = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> refreshCount = new ConcurrentHashMap<>();

    @Override
    public void ensureNodes(UUID playerId, PlayerGameState state) {
        refreshNodes(playerId, state);
    }

    @Override
    public void refreshNodes(UUID playerId, PlayerGameState state) {
        if (playerId == null || state == null) {
            return;
        }
        lastState.put(playerId, state);
        refreshCount.merge(playerId, 1, Integer::sum);
    }

    @Override
    public void clearNodes(UUID playerId) {
        lastState.remove(playerId);
        refreshCount.remove(playerId);
    }

    @Override
    public Optional<UUID> findNodeId(UUID playerId, Ref<EntityStore> targetRef) {
        return Optional.empty();
    }

    public PlayerGameState lastState(UUID playerId) {
        return lastState.get(playerId);
    }

    public int refreshCount(UUID playerId) {
        return refreshCount.getOrDefault(playerId, 0);
    }
}
