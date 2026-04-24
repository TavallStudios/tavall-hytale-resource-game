# Resource System

## Purpose
Own Food, Wood, and Iron as the first real economy loop for the vertical slice.

## Responsibilities
- Persist and mutate player resource inventory.
- Feed upgrade costs and blocked states.
- Receive passive gain from castle economy and node assignment ticks.
- Refresh dependent world visuals whenever inventory changes.

## Main classes
- `ResourceService`
- `ResourceInventory`
- `ResourceType`
- `CastleEconomyPlanner`
- `CastleEconomySimulationService`

## Current gain sources
- Castle-side gatherer allocation from the economy planner.
- Assigned troop extraction from resource nodes.

## World feedback
- Castle stockpile scale and visible pile counts respond to stored resource totals.
- Resource stations show crew count and gain per tick.
- Remote node visuals show local stock and depletion state.

## Links to other systems
- Population system for promotions and workforce allocation.
- Node system for distributed harvesting.
- UI system for resources pages and upgrade availability.
- Persistence system for durable inventory state.

## Next intended expansions
- Building/resource stations inside the castle interior.
- Resource sinks for castle upgrades and build placement.
- Distinct gather loops for citizens versus troops if the design splits them later.