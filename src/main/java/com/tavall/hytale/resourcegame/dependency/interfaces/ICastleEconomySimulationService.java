package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;

import java.time.Instant;

public interface ICastleEconomySimulationService extends IDependencyInjectableInterface {
    void start();

    void shutdown();

    void runTick(Instant now);
}
