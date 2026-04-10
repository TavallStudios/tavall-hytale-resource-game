package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.hypixel.hytale.server.core.universe.world.World;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;
import com.tavall.hytale.resourcegame.domain.KingdomClockState;

public interface IKingdomClockService extends IDependencyInjectableInterface {
    KingdomClockState snapshot();

    void applyToWorld(World world);
}