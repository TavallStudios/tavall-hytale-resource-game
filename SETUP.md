# Setup

## Requirements
- Java 25 (matches Hytale server runtime)
- Postgres
- Redis

## Environment Variables
- TAVALL_POSTGRES_URL
- TAVALL_POSTGRES_USER
- TAVALL_POSTGRES_PASSWORD
- TAVALL_REDIS_HOST
- TAVALL_REDIS_PORT
- TAVALL_REDIS_PASSWORD
- TAVALL_REDIS_TLS
- TAVALL_KINGDOM_TIMEZONE

## Schema
Apply files in schema/postgres in order:
1. 001_player_profile.sql
2. 002_player_game_state.sql

## Testing
- The remote Hytale server should run with `--transport QUIC` for real-client parity.
- The existing TypeScript bot harness remains TCP-only, so the remote wrappers boot a repo-owned local TCP-to-QUIC bridge on the remote host before each scenario.
- Use external TS bot harness to run the join/interact/interior/resource flows.
- Use /kd debug help for in-game command validation.
- Repo-local bot harness wrapper: `powershell -ExecutionPolicy Bypass -File .\scripts\run-bot-harness.ps1`
- Remote Hytale harness wrapper: `powershell -ExecutionPolicy Bypass -File .\scripts\run-remote-bot-harness.ps1`
- Remote castle interaction flow runner: `powershell -ExecutionPolicy Bypass -File .\scripts\run-remote-castle-interaction-flow.ps1`
- Remote resource-game flow runner: `powershell -ExecutionPolicy Bypass -File .\scripts\run-remote-resource-game-flow.ps1`
- Remote persistence flow runner: `powershell -ExecutionPolicy Bypass -File .\scripts\run-remote-persistence-flow.ps1`
- Remote command alias flow runner: `powershell -ExecutionPolicy Bypass -File .\scripts\run-remote-command-alias-flow.ps1`
- Remote UI edge flow runner: `powershell -ExecutionPolicy Bypass -File .\scripts\run-remote-ui-edge-flow.ps1`
- Remote visual counter flow runner: `powershell -ExecutionPolicy Bypass -File .\scripts\run-remote-visual-counter-flow.ps1`
- Remote full suite runner: `powershell -ExecutionPolicy Bypass -File .\scripts\run-remote-full-suite.ps1`
- Generic QUIC smoke runner: `powershell -ExecutionPolicy Bypass -File .\scripts\run-remote-bot-harness.ps1 -Scenario connect-only`
- Bot harness logs and run summaries are written to `bot-logs/`.
- QUIC bridge source lives at `scripts/HytaleQuicTcpBridge.java`; remote bootstrap logic lives at `scripts/remote-quic-harness.ps1`.
- The shared Java smoke harness expects a server on `127.0.0.1:25565`.
