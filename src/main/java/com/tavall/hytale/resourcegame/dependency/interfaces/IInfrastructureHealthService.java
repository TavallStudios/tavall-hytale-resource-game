package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;
import com.tavall.hytale.resourcegame.domain.InfrastructureHealthSnapshot;

/**
 * Reports current cache and persistence operating modes for debug surfaces.
 */
public interface IInfrastructureHealthService extends IDependencyInjectableInterface {
    InfrastructureHealthSnapshot snapshot();
}
