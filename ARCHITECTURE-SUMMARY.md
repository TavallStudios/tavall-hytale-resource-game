# Architecture Summary

## Core Systems
- ResourceGamePlugin now boots through a repo-local Tavall-style DI composition root and resolves runtime services through `IResourceGameDomain`.
- PlayerDataService hydrates PlayerProfile + PlayerGameState via Redis-first cache and Postgres fallback.
- CastleSpawnService spawns the placeholder castle entity and tracks it in CastleEntityRegistry.
- CastleInteractionService listens for near/look interactions and opens the castle UI.
- InteriorWorldService handles enter/exit flows using same-server interior coordinates.
- InteriorWorldService and InteriorInstanceService place each player into a dedicated same-process interior world for clean enter/exit behavior.
- PopulationService manages citizen/troop continuum and promotion/demotion rules.
- PopulationDisplayService spawns anchored NPCs that show counts in the interior.
- ResourceService mutates Food/Wood/Iron inventory.
- KingdomClockService provides 24-hour day/night state.

## UI
- Custom .ui pages live under Common/UI/Custom/Pages.
- UiPageRegistry + UiNavigator build and open pages.
- UiActionService routes button actions to game services.

## Dependency Composition
- `dependency/` contains a repo-local compatibility layer that mirrors the shared Tavall token/domain access pattern while the upstream `tavall-di` module remains non-buildable in this monorepo.
- `ResourceGameDependencyModule` is the single composition root for service registration.
- Runtime-facing services resolve through interfaces first so the repo can swap to the shared DI package later with a smaller migration.

## Persistence
- PlayerProfileRepository and PlayerGameStateRepository use explicit Postgres tables.
- Semantic cache (hot memory + Redis) is used for read-through caching.
- AsyncTask is used for all persistence writes off the main thread.

## Bot Testing
- Repo-local wrapper scripts in `scripts/` invoke the shared TypeScript smoke harness.
- The real remote server now runs in QUIC mode for client parity.
- Because the shared TypeScript bot harness is still TCP-only, the remote wrappers bootstrap `scripts/HytaleQuicTcpBridge.java` through `scripts/remote-quic-harness.ps1` and expose the QUIC server to the bots through a local TCP bridge on the remote host.
- Dedicated remote wrappers cover castle interaction, resource flow, persistence rehydration, `/kd` alias navigation, UI edge cases, and interior population display updates on `/srv/hytale`.
- Run output is captured in `bot-logs/` so bot failures can be reviewed per run.
