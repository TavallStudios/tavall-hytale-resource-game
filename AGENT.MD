# AGENT

Repo-local guidance for this module:

- Prefer MCP tools for filesystem, search, and git work.
- Keep Hytale plugin changes modular and avoid god classes or nested classes.
- Record deferred work in `TODO.md` or `TODO-FIRST-JOINERS.md` instead of dropping it.
- Keep bot harness output in `bot-logs/` through repo-local wrapper scripts.
- Call out shared API or convention changes before widening their usage.
- Never pass or change Hytale server auth mode from this repo; launch scripts must let the server jar/start script use its default auth behavior.
