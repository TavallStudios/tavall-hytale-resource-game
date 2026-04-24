# DI and Cache System

## Purpose
Keep object wiring and fast state access aligned with Tavall patterns used elsewhere, without introducing uncontrolled service construction or ad hoc singletons.

## DI shape
- `ResourceGamePlugin` implements the repo domain interface.
- `DependencyInjectorHelper.setupDISystem(new ResourceGameDependencyModule(this))` boots the graph.
- `IResourceGameDomainGenerated` exposes token-based domain getters used by the plugin entrypoint.
- Services depend on interfaces where mocking or system boundaries justify them.
- Command supports are now DI-managed as well, instead of being constructed ad hoc inside the command root.

## Why repo-local DI exists
- The shared `tavall-di` module is not currently stable enough to consume directly here.
- This repo mirrors the same domain/token/module pattern locally so the usage style still matches Tavall conventions.

## Cache shape
- `SemanticCacheFactory` builds semantic cache instances.
- `JacksonCacheCodec` handles strongly typed serialization.
- Player profile and game state both use semantic cache keys and TTLs.
- Redis is the fast-access layer when configured.

## Current enforcement
- New services in this repo should be added through `ResourceGameDependencyModule` and registered on interfaces where appropriate.
- New stateful gameplay flows should consume `PlayerSessionStore`, not bypass it.
- Cache-aware services should use the existing semantic cache toolchain instead of inventing another cache wrapper.
- Focus, placement, node, and command helpers should be resolved through the DI graph rather than manually instantiated in runtime classes.
