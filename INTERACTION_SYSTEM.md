# Interaction System

## Purpose
Own explicit world-target interaction so castles, nodes, and future placeable systems can all be selected through the same player-facing flow.

## Responsibilities
- Resolve the best focused world target from player position, world, and look direction.
- Keep prompt-lane alignment deterministic for castles and nodes.
- Open the correct UI or action path when the player interacts with a focused target.
- Provide bot-friendly commands that still mirror player behavior instead of bypassing it.

## Main classes
- `FocusedWorldInteractionService`
- `FocusedWorldTargetPlanner`
- `FocusedWorldTarget`
- `FocusedWorldTargetType`
- `CastlePromptLaneService`
- `ResourceNodePromptLaneService`
- `KingdomInteractionCommandSupport`

## Runtime flow
- The player is aligned to a prompt lane for a castle or node.
- The target planner scores visible candidates by distance and alignment.
- `/kd focus` reports the current focused target.
- `/kd interact` executes the same world-target interaction path that the player is expected to use in normal gameplay.

## Current target types
- Castle
- Resource node

## Links to other systems
- Castle system provides castle location and castle UI entry.
- Node system provides selectable node metadata and node UI entry.
- Placement system uses prompt-lane alignment and look-based confirmation as the bridge toward interaction-first placement.
- Command system exposes focus/interact/scan for admins and bots.
- UI system receives the resolved world target and opens the correct page.

## Why this matters
Without a dedicated interaction system, node selection, castle selection, and future build placement drift into separate ad hoc command surfaces. This system keeps those flows converging instead of diverging.

## Extension points
- Add future building pads, upgrade props, and work stations as additional focused target types.
- Replace command-driven bot assists with native click/input once the shared bot client supports them cleanly.
