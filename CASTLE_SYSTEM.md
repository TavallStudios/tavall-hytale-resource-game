# Castle System

## Purpose
Own the player's castle as the main-world anchor for interaction, readable economy feedback, and future kingdom expansion.

## Responsibilities
- Spawn the placeholder castle entity for each player.
- Relocate the castle when placement mode confirms a new site.
- Keep castle focus detection and prompt-lane alignment coherent.
- Provide a predictable world-target interaction path for real players and bots.
- Keep the castle surface scene in sync with resources, jobs, troops, and stockpile state.

## Main classes
- `CastleSpawnService`
- `CastlePlacementService`
- `CastleInteractionService`
- `CastleProximityPromptService`
- `CastleSiteVisualService`
- `CastleSiteScenePlanner`
- `CastlePromptLaneService`
- `CastleEntityRegistry`

## Data owned here
- `CastleLocationData`
- `CastleEntityRegistry` refs
- Castle-site runtime visual refs

## Incoming dependencies
- Player bootstrap from `PlayerDataService`
- Placement confirmation from `PlacementModeService`
- Population/resource snapshots from economy and mutation services

## Outgoing dependencies
- UI system for opening the castle pages
- Interaction system for explicit focus-and-interact flows
- Node system because node routes depend on the current castle location
- Teleport system for `/kd castle goto` and prompt-lane alignment

## Current runtime behavior
- On first session bootstrap the castle is spawned near the player's initial join position.
- The current castle reads as a 3-block stone marker plus a nameplate showing owner, troop count, and Might.
- Castle focus uses a 15-block inspection range. The castle nameplate includes right-click guidance so the player gets readable feedback without proximity chat spam.
- Castle right-click opens the main castle UI when the player is targeting the castle entity or simply focused on the castle marker.
- `/kd castle align` plus `/kd focus` and `/kd interact` reproduce that same selection path for bots and admin testing.
- Castle relocation updates session state, cache, async persistence, castle entity refs, and dependent node visuals.
- Castle-site visuals use NPC scale and crowd counts to show stockpile size, citizen/troop presence, and production lanes.
- Castle UI currently exposes future attack, friend, and guild access actions as non-destructive placeholders.

## Extension points
- Replace the placeholder NPC castle entity with a proper Hytale asset without changing bootstrap or interaction logic.
- Move surface scene dressing into richer station props while keeping the same planner and refresh entrypoints.
- Add building upgrades as additional castle-surface sub-scenes instead of embedding that logic into the command layer.
