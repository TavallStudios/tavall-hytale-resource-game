# Agent Notes

## Repo Rules
- Use MCP tools first for repo inspection, file reads, and targeted edits when practical.
- Preserve the project DI pattern and existing clean Java structure. Do not introduce nested classes.
- Never change the Hytale server auth mode from this repository. Keep the server on its jar-default auth behavior.
- Do not patch over ad-hoc jars on the dev server. Build the plugin here and deploy it through the local server workflow.

## Runtime Rules
- Treat the local dev server at `C:\Users\TJ\Documents\HyTaleDevServer` as the primary live test target unless the task explicitly calls for the remote server.
- Keep Custom UI assets and plugin resources packaged through the normal Maven jar.
- Prefer block-first visuals for castles, buildings, and nodes. Interior NPCs should remain stationary anchor visuals only.

## Testing Rules
- Favor automated Java tests first, then targeted bot or live-server verification for gameplay flows.
- Keep bot logs bounded and human-readable. Prefer `.txt` summaries over raw high-volume dumps where possible.
