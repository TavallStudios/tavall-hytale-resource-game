# Test System

## Purpose
Cover gameplay and infrastructure behavior with both in-memory Java tests and remote bot scenarios that exercise the real Hytale runtime.

## Layers
- Java tests: fast, deterministic, in-memory coverage for planners, metadata, persistence behavior, and service orchestration.
- Remote QUIC bot flows: integration coverage against the running remote Hytale server.
- Local deployment verification: rebuild plugin jar, deploy only the plugin jar, restart dev server, and verify server boot.

## Main Java coverage areas
- Cache round-trip behavior
- Profile/game-state load-or-create
- Metadata hydration and onboarding flags
- Economy tick behavior
- Node stock depletion/regeneration
- Castle placement persistence and visual refresh fan-out
- Scene planner scaling and readability math

## Main bot flow areas
- Connect-only and world join
- Castle near/look interaction
- Focus-and-interact world targeting
- Resource-game UI flow
- Interior cycle and tour
- Persistence and rehydration
- Placement flow
- Node assignment flow
- UI edge behavior
- Visual counter flow

## Log handling
- Normal runs keep compact artifacts in `bot-logs/`.
- Heavy transcripts are minimized unless explicitly requested.
- `prune-bot-logs.ps1` removes stale artifacts so bot logs do not grow without bound.
- `run-remote-full-suite.ps1` now retries each remote step once before failing the aggregate suite, because the QUIC harness still has intermittent disconnects that do not reflect plugin regressions.

## Known limitations
- Shared bot client support for native world-click packets is still incomplete for this repo's needs.
- Placement scenarios currently use align -> look -> focus/interact or aim-confirm to stay close to player behavior without patching over dirty shared bot code.
- Restart-sensitive remote suites wait for an explicit remote boot marker before they run, not just an open socket.
- Some remote flows intentionally use admin commands after a real UI/world open so the scenario can validate gameplay state deterministically even when the shared bot client does not fire reliable CustomUI button events.
