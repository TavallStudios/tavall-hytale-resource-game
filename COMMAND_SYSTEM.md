# Command System

## Purpose
Provide a development-first admin and debug surface that accelerates gameplay iteration and bot testing without hard-coding one-off server logic into page classes.

## Main command root
- `/kingdom`
- alias: `/kd`

## Current command families
- `ui`
- `data`
- `castle`
- `interior`
- `citizens`
- `troops`
- `resources`
- `nodes`
- `place`
- `focus`
- `interact`
- `scan`
- `scene`
- `bootstrap`
- `tick`
- `tutorial`

## Main classes
- `DebugCommandService`
- `KingdomCommand`
- `KingdomPlacementCommandSupport`
- `KingdomNodeCommandSupport`
- `KingdomInteractionCommandSupport`

## Bot/admin-specific helpers
- `/kd place castle`
- `/kd place node <type>`
- `/kd place confirm`
- `/kd place cancel`
- `/kd place status`
- `/kd focus`
- `/kd interact`
- `/kd scan`
- `/kd castle goto`
- `/kd castle align`
- `/kd castle move`
- `/kd nodes goto <index|node_id_prefix>`
- `/kd nodes align <index|node_id_prefix|focus>`
- `/kd nodes status <index|node_id_prefix|focus>`
- `/kd scene refresh`
- `/kd bootstrap`
- `/kd tick run [count]`
- `/kd tutorial reset`

## Design notes
- Commands are intentionally grouped by system instead of dumping every mutation into one flat namespace.
- Focus and interact commands exist so bots can behave more like players instead of opening everything through direct command-only shortcuts.
- Placement commands exist so bots can use the same server-side targeting logic as real players until native click packets are integrated.
- Mutation commands refresh tracked castle/resource/node pages when possible so live operator sessions and bot scenarios can observe state changes without manual page teardown.
- Command handlers should mutate services, not repositories or raw metadata directly.
