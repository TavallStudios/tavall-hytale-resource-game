package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;

public interface ICastleEconomySimulationService extends IDependencyInjectableInterface {
    void start();

    void shutdown();
}
