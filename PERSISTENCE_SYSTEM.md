# Persistence System

## Purpose
Persist core player identity and gameplay state through in-memory session state, Tavall semantic cache, Redis, and Postgres.

## Responsibilities
- Load or create `PlayerProfile`.
- Load or create `PlayerGameState`.
- Maintain Redis-first cache reads through semantic cache adapters.
- Serialize extensible metadata for onboarding, aging, jobs, and nodes.
- Persist off the main game loop through `AsyncTask`.

## Main classes
- `PlayerProfileService`
- `PlayerGameStateService`
- `PlayerDataService`
- `PostgresConnectionProvider`
- `PlayerProfileRepository`
- `PlayerGameStateRepository`
- `SemanticCacheFactory`
- `JacksonCacheCodec`

## Data shape
- `player_profile` stores identity and timing basics.
- `player_game_state` stores castle location, counts, resources, interior world, and metadata JSON.
- `GameStateMetadata` currently carries population metadata, onboarding flags, and resource nodes.

## Read/write path
- Active gameplay uses in-memory session state.
- Cache lookups occur before repository reads.
- Async persistence is used for post-mutation durability.
- Rehydration reconstructs population metadata and onboarding state from JSON.

## Links to other systems
- Every gameplay system depends on this layer for authoritative state.
- The test system validates Redis-first behavior and Postgres round-trips remotely.

## Notes
- The repo already uses Tavall cache tooling (`SemanticCache`, `SemanticCacheFactory`, `JacksonCacheCodec`).
- The shared Tavall DI module is still not the live dependency here; the repo mirrors the same composition pattern locally.