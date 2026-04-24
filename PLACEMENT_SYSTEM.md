# Placement System

## Purpose
Provide one consistent interaction-first placement flow for world objects instead of ad hoc command-only spawning.

## Responsibilities
- Arm placement modes for castles and resource nodes.
- Show preview markers at the current aimed block.
- Consume world interaction while placement is active.
- Support aim-confirm fallback for bots and admin workflows.
- Keep placement consistent with the broader focus-and-interact model.
- Clear placement state cleanly on confirm or cancel.

## Main classes
- `PlacementModeService`
- `PlacementInteractionService`
- `PlacementPreviewService`
- `KingdomPlacementCommandSupport`
- `PlacementRequest`
- `PlacementResult`
- `PlacementModeType`

## Current placement-capable systems
- Castle placement
- Resource node placement

## Interaction model
- Arm via command: `/kd place castle` or `/kd place node <type>`.
- Preview updates from the player's aimed block.
- Confirm by clicking a block in-world.
- Bot/admin fallback: `/kd place confirm` resolves the same aimed block server-side through `TargetUtil`.
- Cancel via `/kd place cancel`.
- Alignment helpers from the interaction system make it easier for bots and admins to reproduce the same flow the client is expected to use.

## Dependencies
- Castle placement delegates to `CastlePlacementService`.
- Node placement delegates to `ResourceNodeService`.
- Preview visuals use scaled NPC placeholders through `NpcVisualSpawner`.
- Castle and node prompt lanes feed into the same player-facing placement rhythm.
- Castle proximity prompt suppresses itself while placement is active.

## Why this exists
The repo originally mixed direct command placement with focus-driven interaction. That made it hard to grow into real gameplay placement. This system gives one path that future build stations, castle upgrades, work yards, and node placement can all share.

## Next intended expansions
- Placement for build stations and future upgrade props.
- Placement for any future castle-surface buildings should enter through this stack instead of adding new one-off spawn commands.
- Better preview rules for invalid terrain and overlap rejection.
- Real click-driven bot coverage once native world-click packets are available in the shared bot client.
