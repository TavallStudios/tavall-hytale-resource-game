# Interior System

## Purpose
Own the same-process castle interior prototype as a structured but intentionally bland playable space.

## Responsibilities
- Create per-player interior worlds.
- Teleport players into and out of those worlds safely.
- Spawn interior anchor displays and tutorial markers.
- Build an outward worker platform beyond the main room.
- Keep one stationary NPC per worker type on that platform with count/name text above each anchor.
- Provide a placeholder portal that later task-copy NPCs can visually enter before being teleported to main-world task locations.
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
- Worker anchors are stationary. When real work visualization expands, task copies should move while the original anchors remain visible.
- Right-clicking worker anchors currently routes to the citizens/troops UI with worker-specific feedback; fully dedicated worker pages are deferred.
- `/kd interior exit` sends the player back out cleanly.

## Links to other systems
- UI system opens the interior page.
- Population display system renders citizen/troop counters inside the interior.
- Onboarding/tutorial state in persistence tracks first interior steps.

## Deferred work
- Dedicated interior servers remain a TODO, not current runtime behavior.
- The current layout leaves room for future stations, upgrade props, and more specialized interior scenes.
## Interior Building Lots
- Castle buildings now belong to the castle interior. Resource nodes remain in the surface castle world.
- `InteriorLayoutService` reserves deterministic lots for `FARMSTEAD`, `LUMBER_MILL`, `IRON_WORKS`, `BARRACKS`, and `WORKSHOP`.
- Building staging and spawn commands should use `BuildingPlacementPlanner`, which resolves the active interior instance and returns the reserved lot for the selected building.
- Players start without building unlocks. `AccountProgression` unlocks buildings by account level, with every building unlocked by level 50.
- Account level has no maximum. After level 50 the XP requirement stops scaling and stays at the high fixed post-unlock requirement.
