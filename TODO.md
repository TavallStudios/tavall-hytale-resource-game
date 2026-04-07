# TODO

## Interiors
- Move interiors from same-process world instances to dedicated interior server instances.
- Add interior transfer handoff contracts for cross-process routing.
- Add interior server availability and failover handling.

## Visual Replacement Pipeline
- Replace placeholder castle/interior assets with production art packs.
- Add dedicated visual replacement registry for UI and entity skins by progression tier.
- Add animation paths for citizen and troop anchor entities.

## Castle and Interaction Expansion
- Expand castle info panel beyond placeholder values.
- Add permission layers and ownership checks for non-dev command access.
- Add interaction states for castle upgrades and unlock trees.

## Citizen/Troop Simulation
- Add full unit metadata persistence and loading for individual unit history beyond summary bootstrapping.
- Add richer citizen/troop simulation loops for movement, work, and battle preparation.
- Add aggregated stat pipelines for battle and productivity medians.
- Add branching promotion trees and specialization states.

## Jobs and Stations
- Implement job loops for gatherer, builder, trainee, and soldier.
- Add resource/work stations in interior with interaction hooks.
- Add castle action triggers tied to job outputs.

## Aging and Time
- Finalize aging cadence from unresolved requirement (`"1 Real world da..."` from prior notes) and tune progression impacts.
- Add age-based stat drift and role constraints once cadence is finalized.
- Link kingdom clock day/night state to citizen behavior and world mood.

## Persistence and Reliability
- Wire a real Redis client adapter and production Postgres DataSource configuration.
- Add migration tooling for schema evolution and index tuning.
- Add transactional guards and retry policies for persistence failures.
- Add cache invalidation policy tuning and observability metrics.

## Bot and Automation Testing
- Expand hytale bot harness scenarios for multi-player load and contention.
- Add deterministic replay runs for command mutation and UI navigation.
- Add failure-injection scenarios for Redis/Postgres unavailability.
