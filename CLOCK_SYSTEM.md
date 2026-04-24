# Clock System

## Purpose
Provide one kingdom-time authority for day/night logic and later NPC behavior.

## Responsibilities
- Resolve the configured timezone.
- Expose a `KingdomClockState` snapshot.
- Apply the current day/night state to worlds.

## Main classes
- `KingdomClockService`
- `KingdomClockState`
- `KingdomClockConfig`

## Current behavior
- Uses a real 24-hour day.
- Defaults to day between 06:00 and 18:00 in the configured timezone.
- Applies world mood during player bootstrap.

## Links to other systems
- Player bootstrap applies clock state when sessions are initialized.
- Future population, node, and castle mood systems should consume this service instead of recalculating time on their own.

## Deferred work
- More nuanced lighting/mood transitions.
- Behavior schedules keyed to jobs or guard shifts.