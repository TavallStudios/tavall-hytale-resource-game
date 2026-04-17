# Node System

## Purpose
Own player-placed resource nodes, worker/troop assignment, depletion, regeneration, manual pillage rewards, and readable world feedback for off-castle gathering.

## Responsibilities
- Persist placed node metadata in player game-state metadata.
- Resolve node selection by index or node-id prefix.
- Resolve node focus from world-space interaction when the player is actually looking at a node.
- Assign and recall troops.
- Route eligible citizen workers automatically through the economy planner.
- Allow manual pillage for immediate rewards that drain stock faster than passive collection.
- Drain and regenerate node stock during economy ticks.
- Build route and worker visuals.
- Open the node detail UI from world interaction or command-driven selection.

## Main classes
- `ResourceNodeService`
- `ResourceNodeVisualService`
- `ResourceNodeVisualPulseService`
- `ResourceNodeRoutePlanner`
- `ResourceNodeInteractionService`
- `ResourceNodeStructureService`
- `KingdomNodeCommandSupport`
- `ResourceNodeData`
- `ResourceNodeSummary`

## Data flow
- Node state lives inside `GameStateMetadata.resourceNodes`.
- `ResourceNodeService` rewrites the metadata JSON when node state changes.
- Node summary objects derive UI- and visual-friendly values like stock status, gain per tick, available troops, and visible route count.

## World behavior
- Nodes have a placed pad, stock beacon, anchor label, worker crowd, and supply-lane visuals.
- Scale is used to reinforce node importance and activity.
- Assigned troops and assigned workers increase the visible worker presence and convoy route density.
- Nodes use Hytale block/static structure placeholders for the pad, stock marker, and resource silhouette. The current safe block key is intentionally conservative to avoid client asset load failures.
- Node stock level affects status labels and beacon shape.
- Prompt lanes provide a deterministic place for players and bots to align with a node before interacting.

## UI behavior
- `ResourceNodePage` shows assignment, reserve troops, auto workers, gain per tick, pillage reward, stock, regen, and route status.
- Node UI actions route through `UiActionService`, not directly to repositories or page classes.

## Links to other systems
- Depends on castle location from the castle system for route rendering.
- Depends on the interaction system for focus resolution, node prompt lanes, and explicit world-target interaction.
- Depends on population counts from the population system for available troop calculations.
- Depends on the resource system for Food/Wood/Iron mutations from automatic gathering and manual pillage.
- Depends on persistence metadata handling from the persistence system.

## Next intended expansions
- Replace pulse markers with actual moving carriers.
- Move from command-assisted placement toward direct interaction-first node placement once the shared bot client supports native world-click behavior.
- Node exhaustion props by resource type instead of generic stone-only pads.
- Add cooldowns, defensive risk, and event hooks around manual pillage.
