package com.tavall.hytale.resourcegame.dependency;

/**
 * Composition root contract for repo-local DI setup.
 */
public interface IDependencyModule extends IDependencyInjectableConcrete {
    void registerDependencies();
}