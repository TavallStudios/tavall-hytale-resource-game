# Systems

This repo is split into explicit gameplay and infrastructure systems. `SYSTEMS.md` is the entry map. Each detailed system file owns the verbose notes, cross-system dependencies, runtime behavior, and extension points for that area.

## Current runtime shape
- One Hytale plugin process owns player bootstrap, castle surface gameplay, UI, interior worlds, and economy ticks.
- Castle interiors are separate worlds in the same server process, not separate servers.
- Persistence uses Tavall semantic cache plus Redis/Postgres-backed stores when configured.
- Placement is moving toward interaction-first flows for every world placement feature.
- Bot coverage is split between Java unit tests and remote QUIC scenario runs.

## System index
- [CASTLE_SYSTEM.md](./CASTLE_SYSTEM.md): Castle spawn, relocation, focus detection, and surface scene state.
- [INTERACTION_SYSTEM.md](./INTERACTION_SYSTEM.md): Focus resolution, prompt lanes, and explicit world-target interaction.
- [PLACEMENT_SYSTEM.md](./PLACEMENT_SYSTEM.md): Armed placement modes, previews, click confirmation, and placement-admin flows.
- [NODE_SYSTEM.md](./NODE_SYSTEM.md): Resource nodes, assignment, depletion, route visuals, and node UI.
- [POPULATION_SYSTEM.md](./POPULATION_SYSTEM.md): Citizen/troop continuum, jobs, promotions, and aggregated metadata.
- [RESOURCE_SYSTEM.md](./RESOURCE_SYSTEM.md): Resource inventory, passive gain, node yield, and mutation commands.
- [INTERIOR_SYSTEM.md](./INTERIOR_SYSTEM.md): Interior world instances, teleport flow, anchor displays, and tutorial pathing.
- [UI_SYSTEM.md](./UI_SYSTEM.md): Custom UI page registry, page models, actions, and page-event routing.
- [PERSISTENCE_SYSTEM.md](./PERSISTENCE_SYSTEM.md): Player profile/game-state persistence, metadata hydration, and async writes.
- [CLOCK_SYSTEM.md](./CLOCK_SYSTEM.md): Kingdom clock, timezone ownership, and world day/night application.
- [COMMAND_SYSTEM.md](./COMMAND_SYSTEM.md): Debug/admin command surface, aliases, placement helpers, and bot-friendly controls.
- [DI_CACHE_SYSTEM.md](./DI_CACHE_SYSTEM.md): Tavall DI usage style, repo-local DI composition root, and Tavall cache integration.
- [TEST_SYSTEM.md](./TEST_SYSTEM.md): Java tests, remote bot suites, compact log handling, and deployment verification.

## Main cross-system flows
- Player join: persistence -> session bootstrap -> castle spawn -> castle/node visuals -> clock apply.
- Castle or node interaction: prompt-lane alignment -> focus planner -> interaction service -> UI navigator -> page model -> UI action service.
- Placement: command arms placement -> preview markers -> interact or aim-confirm -> placement service -> visuals/persistence refresh.
- Economy tick: planner -> population/resource updates -> node depletion/regeneration -> castle/node visual refresh -> async persist.
- Interior: command/UI -> interior world service -> teleport -> interior visuals/tutorial -> exit back to castle world.

## Current gaps that still matter
- Native world-click bot input is still limited by the shared bot client surface, so placement bots use align -> look -> focus -> interact or aim-confirm instead of direct click packets.
- Placement is now explicit for castles and nodes. Future build stations, castle props, and upgrade pads should be routed through the same placement stack instead of adding one-off commands.
- Placeholder NPC visuals are still doing a lot of explanatory work. The system boundaries are intended to make replacing those with better assets straightforward.
