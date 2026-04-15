package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;

public interface IResourceNodeVisualPulseService extends IDependencyInjectableInterface {
    void start();

    void shutdown();
}
