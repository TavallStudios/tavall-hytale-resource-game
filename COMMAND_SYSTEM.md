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
- `account`
- `hologram`
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
- `/kd account debug on|off|status [player_name|uuid_prefix]`
- `/kd hologram spawn <text>`
- `/kd hologram stack <line1|line2|...>`
- `/kd hologram status`
- `/kd hologram clear`

## Design notes
- Commands are intentionally grouped by system instead of dumping every mutation into one flat namespace.
- Focus and interact commands exist so bots can behave more like players instead of opening everything through direct command-only shortcuts.
- Placement commands exist so bots can use the same server-side targeting logic as real players until native click packets are integrated.
- Mutation commands refresh tracked castle/resource/node pages when possible so live operator sessions and bot scenarios can observe state changes without manual page teardown.
- Command handlers should mutate services, not repositories or raw metadata directly.
## Account Progression Commands
- `/kd account status [player_name|uuid_prefix]` prints the account level, current XP, required XP, total XP, and debug restriction state.
- `/kd account addxp <amount> [player_name|uuid_prefix]` grants account XP and carries overflow through as many levels as needed.
- `/kd account setlevel <level> [player_name|uuid_prefix]` is a debug/admin shortcut for validating building unlocks and interior building lots.
- `/kd account debug on|off|status [player_name|uuid_prefix]` persists a debug mode flag that lets the targeted player ignore account-level building restrictions during testing.

## Hologram Debug Commands
- `/kd hologram spawn <text>` spawns one nameplate-backed test label above the player.
- `/kd hologram stack <line1|line2|...>` spawns multiple label lines; empty pipe sections are ignored.
- `/kd hologram status` reports the active hologram refs owned by the player.
- `/kd hologram clear` removes the player's active debug holograms.
- Hologram spawns log model resolution, nameplate-only fallback use, successful refs, and spawn/removal failures so console output explains why a label did not appear.
