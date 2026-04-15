# Interior System

## Purpose
Own the same-process castle interior prototype as a structured but intentionally bland playable space.

## Responsibilities
- Create per-player interior worlds.
- Teleport players into and out of those worlds safely.
- Spawn interior anchor displays and tutorial markers.
- Keep the interior on the same server process for now.

## Main classes
- `InteriorWorldService`
- `InteriorInstanceService`
- `InteriorLayoutService`
- `InteriorStructureService`
- `InteriorTourMarkerService`
- `PlayerTeleportService`

## Runtime model
- `/kd interior` creates or resolves a dedicated world for the player inside the same server process.
- The world name is deterministic per player.
- The player is cross-world teleported into the interior world, not moved to another area of the same world.
- `/kd interior exit` sends the player back out cleanly.

## Links to other systems
- UI system opens the interior page.
- Population display system renders citizen/troop counters inside the interior.
- Onboarding/tutorial state in persistence tracks first interior steps.

## Deferred work
- Dedicated interior servers remain a TODO, not current runtime behavior.
- The current layout leaves room for future stations, upgrade props, and more specialized interior scenes.