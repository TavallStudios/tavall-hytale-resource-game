# Population System

## Purpose
Model the citizen-to-troop continuum as one population with upgrade state, jobs, aging scaffolding, and aggregate statistics.

## Responsibilities
- Track citizen count and troop count.
- Calculate aggregate Might for military readability.
- Promote and demote between citizen and troop states.
- Maintain job counts for idle, gatherer, hunter, cook, miner, blacksmith, architect, grunt builder, trainee, and soldier-facing behaviors.
- Preserve aggregate citizen/troop metadata for future richer simulation.
- Feed economy planners and UI summaries.

## Main classes
- `PopulationService`
- `PopulationSummary`
- `CitizenMetaData`
- `TroopMetaData`
- `CitizenJobType`
- `AgingState`

## Current runtime behavior
- Promotions consume Food, Wood, and Iron.
- Demotions move troops back into the citizen pool.
- Might currently treats all aggregate troops as tier 1, so each troop contributes 1 Might.
- Job counts are derived and refreshed by the economy planner.
- `BUILDER` remains as a legacy aggregate metadata bucket; new planning uses `BLACKSMITH`, `ARCHITECT`, and `GRUNT_BUILDER`.
- Interior worker visuals spawn one stationary anchor NPC per job type. Task-specific movement will use copies so the anchor and count are always visible.
- Aging is scaffolded in metadata and ticks over real time, but final balancing is still intentionally deferred.

## Links to other systems
- Resource system gates promotions.
- Node system consumes troop availability for assignment and eligible worker roles for automatic gathering.
- UI system surfaces job and action state.
- Persistence system serializes all aggregate metadata into JSON-safe structures.

## Next intended expansions
- Individual simulated population members built on top of the existing aggregated metadata.
- Median and percentile combat/productivity summaries for combat and work-resolution systems.
- More explicit soldier job state once troop pathing and combat loops arrive.
- Persisted troop-tier distributions so Might becomes tier-weighted instead of a tier-1 fallback.
