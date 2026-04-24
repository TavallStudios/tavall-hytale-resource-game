package com.tavall.hytale.resourcegame.support;

import com.hypixel.hytale.server.core.universe.world.World;
import com.tavall.hytale.resourcegame.domain.PopulationSummary;
import com.tavall.hytale.resourcegame.interior.InteriorLayout;
import com.tavall.hytale.resourcegame.services.PopulationDisplayGateway;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RecordingPopulationDisplayGateway implements PopulationDisplayGateway {
    private final Map<UUID, PopulationSummary> summaries;

    public RecordingPopulationDisplayGateway() {
        this.summaries = new ConcurrentHashMap<>();
    }

    @Override
    public void ensureDisplays(UUID playerId, World world, InteriorLayout layout, PopulationSummary summary) {
        summaries.put(playerId, summary);
    }

    @Override
    public void updateDisplays(UUID playerId, PopulationSummary summary) {
        summaries.put(playerId, summary);
    }

    @Override
    public void clearDisplays(UUID playerId) {
        summaries.remove(playerId);
    }

    public PopulationSummary lastSummary(UUID playerId) {
        return summaries.get(playerId);
    }
}
