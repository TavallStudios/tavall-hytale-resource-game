package com.tavall.hytale.resourcegame.dependency.injection.helpers.interfaces;

import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.IDependencyModule;

/**
 * Bootstraps the repo-local DI registry.
 */
public interface IDependencyInjectorHelper extends IDependencyInjectableConcrete {
    void setupDISystem(IDependencyModule module);
}