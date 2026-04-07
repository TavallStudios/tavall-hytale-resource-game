# Architecture Summary

## Goal
This repository implements the first vertical slice for a Hytale-style citizen-to-army resource loop with clean extension boundaries for visuals, persistence, and simulation depth.

## Core Layers
- `domain`: explicit state models for profile, game state, castle, interior, population continuum, resources, and UI descriptors.
- `service`: gameplay orchestration systems for initialization, castle spawn/interaction, interior transitions, population/resource mutations, UI assembly, and aging hooks.
- `persistence`: Redis fast-access store, Postgres durable store, and in-memory Postgres substitute used by local harness testing.
- `runtime`: Hytale runtime boundary interface and an in-memory runtime implementation for local bot and integration harness execution.
- `command`: `/kingdom` and `/kd` routing with grouped debug and mutation commands.
- `clock`: dedicated kingdom clock service with real 24-hour day/night state.

## Vertical Slice Flow
1. Player join request hydrates via hot cache -> Redis -> Postgres fallback.
2. First-init path creates profile/state defaults and spawns castle at current position (temporary flow).
3. Castle near/look detection opens castle UI.
4. Interior command enters same-process interior world with planned layout zones.
5. Citizen/troop anchor entities display live counts in interior.
6. Commands and UIs mutate population/resource values and persist asynchronously.

## Persistence Model
- `player_profile`: identity and tracking fields (uuid, name, timezone, transformed ip, timestamps).
- `player_game_state`: castle metadata, citizen/troop counts, starter resources, interior world marker, aging fields.
- `player_population_unit`: future-extensible unit metadata rows for continuum evolution.
- Redis is treated as fast-access hydration layer before Postgres.

## Testing Strategy
- In-memory harness validates end-to-end vertical slice behavior with bot-driven commands and UI interactions.
- Redis-first gateway harness validates read ordering and async persistence behavior.
- Live bot harness scenario validates rehydration across runtime instances using shared stores.
