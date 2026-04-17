# TODO

## Castle & Placement
- Replace the temporary "spawn at player location" castle flow with a proper placement experience.
- Swap the placeholder NPC castle asset with a true castle model + interaction collider.
- Add ownership transfer and multi-castle support.

## Interiors
- Move interiors to dedicated interior server instances instead of same-process coordinates.
- Add interior generation templates and upgradeable layouts.
- Add interior construction stations and visual upgrades.

## Population & Jobs
- Implement per-citizen simulation with individual stats feeding aggregated medians.
- Build full job loops for gatherer, hunter, cook, miner, blacksmith, architect, grunt builder, trainee, and soldier roles.
- Add job assignment UI and persistent job metadata.

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
