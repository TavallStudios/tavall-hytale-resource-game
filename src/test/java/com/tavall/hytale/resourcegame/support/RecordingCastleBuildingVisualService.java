package com.tavall.hytale.resourcegame.support;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleBuildingVisualService;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RecordingCastleBuildingVisualService implements ICastleBuildingVisualService {
    private final Map<UUID, PlayerGameState> lastState = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> refreshCount = new ConcurrentHashMap<>();

    @Override
    public void ensureBuildings(UUID playerId, PlayerGameState state) {
        refreshBuildings(playerId, state);
    }

    @Override
    public void refreshBuildings(UUID playerId, PlayerGameState state) {
        if (playerId == null || state == null) {
            return;
        }
        lastState.put(playerId, state);
        refreshCount.merge(playerId, 1, Integer::sum);
    }

    @Override
    public void clearBuildings(UUID playerId) {
        lastState.remove(playerId);
        refreshCount.remove(playerId);
    }

    @Override
    public Optional<UUID> findBuildingId(UUID playerId, Ref<EntityStore> targetRef) {
        return Optional.empty();
    }

    public PlayerGameState lastState(UUID playerId) {
        return lastState.get(playerId);
    }

    public int refreshCount(UUID playerId) {
        return refreshCount.getOrDefault(playerId, 0);
    }
}
