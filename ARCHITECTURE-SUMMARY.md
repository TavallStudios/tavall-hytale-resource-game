# Architecture Summary

## Core Systems
- ResourceGamePlugin bootstraps all services and registers commands/events.
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

## Persistence
- PlayerProfileRepository and PlayerGameStateRepository use explicit Postgres tables.
- Semantic cache (hot memory + Redis) is used for read-through caching.
- AsyncTask is used for all persistence writes off the main thread.

## Bot Testing
- Repo-local wrapper scripts in `scripts/` invoke the shared TypeScript smoke harness.
- Dedicated remote wrappers cover castle interaction, resource flow, persistence rehydration, `/kd` alias navigation, and interior population display updates on `/srv/hytale`.
- Dedicated remote wrappers also cover UI event navigation and upgrade edge cases on `/srv/hytale`.
- Run output is captured in `bot-logs/` so bot failures can be reviewed per run.
