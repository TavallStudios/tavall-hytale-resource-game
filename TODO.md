# TODO

## Castle & Placement
- Replace the temporary "spawn at player location" castle flow with a proper placement experience.
- Swap the placeholder NPC castle asset with a true castle model + interaction collider.
- Add ownership transfer and multi-castle support.
- Add real castle attack/scouting flow, including target validation, battle entry, cooldowns, and readable defender feedback.
- Add friend/trust access management for castle interaction options.
- Add guild ownership/access permissions and guild role enforcement before exposing guild castle actions.
- Persist tiered troop counts so Might can calculate tier 1 = 1, tier 2 = 2, and higher tiers without relying on the current tier-1 aggregate fallback.

## Interiors
- Move interiors to dedicated interior server instances instead of same-process coordinates.
- Add interior generation templates and upgradeable layouts.
- Add interior construction stations and visual upgrades.
- Replace the placeholder worker portal with real task-copy pathfinding, portal effects, and cross-world worker task state.
- Re-enable entity-backed interior tour labels and stationary worker/NPC anchors after the Hytale hologram/nameplate path is validated without world-thread crashes.
- Add worker-specific right-click UIs with job assignment, task dispatch, and debug controls instead of routing to the broader citizens/troops pages.

## Population & Jobs
- Implement per-citizen simulation with individual stats feeding aggregated medians.
- Build full job loops for gatherer, hunter, cook, miner, blacksmith, architect, grunt builder, trainee, and soldier roles.
- Add job assignment UI and persistent job metadata.
- Add production-safe worker task copies that leave the stationary interior anchor intact while visual jobs run in the interior or main world.

## Citizen Aging
- Finalize the real-world aging cadence ("1 Real world da..." pending).
- Add lifecycle milestones and retirement handling.

## Resources & Economy
- Add production buildings for Food/Wood/Iron generation.
- Add storage limits, decay, and transport logic.
- Add pillage cooldowns, node defense risk, and richer reward tables once combat pressure exists.
- Replace safe single-block node silhouettes with verified Hytale static assets/prefabs after asset keys are stable.

## UI & UX
- Replace placeholder UI visuals with final art and flow.

## Persistence & Infrastructure
- Add migrations for future population metadata and per-citizen tables.
- Add Redis/Postgres health checks and reconnection handling.
- Add metrics around cache hit rates and save latency.
- Upgrade the Hytale bot harness to authenticate and hydrate shipped asset packs the same way as the real client before relying on it for local UI coverage.
