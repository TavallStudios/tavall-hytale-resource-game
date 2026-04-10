package com.tavall.hytale.resourcegame.dependency;

import com.hypixel.hytale.server.core.universe.world.World;
import com.tavall.hytale.resourcegame.dependency.interfaces.IKingdomClockService;
import com.tavall.hytale.resourcegame.domain.KingdomClockState;

import java.time.Instant;

/**
 * Test stub for kingdom clock access.
 */
public final class TestKingdomClockService implements IKingdomClockService {
    @Override
    public KingdomClockState snapshot() {
        return new KingdomClockState(Instant.parse("2026-04-09T00:00:00Z"), true, "UTC");
    }

    @Override
    public void applyToWorld(World world) {
    }
}