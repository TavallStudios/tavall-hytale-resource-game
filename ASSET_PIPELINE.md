# Asset Pipeline

## Purpose
Keep UI images, font reference sheets, Blockbench source models, and prefab recipes reproducible inside the plugin asset pack.

## Generator
- Run `python scripts/generate-resource-game-assets.py` from the repo root.
- Outputs are deterministic and overwrite only generated asset files.
- Decorative UI assets are mirrored horizontally and vertically so panels, buttons, icons, selectors, and inner ornamentation remain symmetrical.
- Font sheets keep glyphs readable, so symmetry is applied to their cells and frames rather than to each glyph shape.

## UI Texture Outputs
- `src/main/resources/Common/UI/Custom/Textures/ResourceGame/panels/`
- `src/main/resources/Common/UI/Custom/Textures/ResourceGame/buttons/`
- `src/main/resources/Common/UI/Custom/Textures/ResourceGame/icons/`
- `src/main/resources/Common/UI/Custom/Textures/ResourceGame/selectors/`
- `src/main/resources/Common/UI/Custom/Textures/ResourceGame/fonts/`
- `src/main/resources/Common/UI/Custom/Textures/ResourceGame/examples/`
- `src/main/resources/Common/UI/Custom/Textures/ResourceGame/resource-game-ui-assets.json`

## Font References
The font template documents are tracked as PNG sheets and described in `FONT_TEMPLATES.md`.

Current sets:
- Big display alphabet for page titles, banners, and building-name hero text.
- Menu alphabet for standard labels, timers, stats, and action buttons.
- Numbers and symbols for resource counts, costs, timers, coordinates, and debug/status text.

## Blockbench Sources
Blockbench model sources live under `src/main/resources/Common/Models/ResourceGame/`.

Legacy overview models remain available for quick inspection:
- `castle_keep.bbmodel`
- `farmstead.bbmodel`
- `lumber_mill.bbmodel`
- `iron_works.bbmodel`
- `barracks.bbmodel`
- `workshop.bbmodel`

## Imported Blockbench Models
- `src/main/resources/Common/Models/ResourceGame/farmstead.bbmodel` is the farmstead Blockbench source imported from the local Downloads handoff file.
- Farmstead spawn/stage commands resolve the interior farmstead lot through `BuildingPlacementPlanner`; visual refresh uses the packaged model asset alongside the protected building scene.
- `resource_node.bbmodel`

Production-facing generated sources are expanded by gameplay role:
- `castles/castle_keep/level_01.bbmodel` through `level_30.bbmodel`
- `buildings/{building}/level_01.bbmodel` through `level_30.bbmodel` for every `BuildingType`
- `buildings/{building}/construction/{foundation,scaffolding,shell,complete}.bbmodel`
- `nodes/{food,wood,iron}/{full,depleted}.bbmodel`
- `props/{citizen_anchor,troop_anchor,worker_platform,exit_portal,hologram_pedestal,placement_selector_valid,placement_selector_blocked,castle_radius_marker,node_radius_marker}.bbmodel`

`resource-game-model-assets.json` records every generated model and prefab recipe, category counts, and the maximum building level. Tests use this manifest to catch missing Blockbench files before packaging.

## Prefab Recipes
Prefab source recipes live under `src/main/resources/Common/Prefabs/ResourceGame/`.

These recipes intentionally use a repo-local schema, `resource-game-prefab-recipe/v1`, so we can keep source-of-truth block plans versioned before serializing official Hytale `.prefab.json` files.

The Hytale runtime target for each recipe is listed in its `hytalePrefabTarget` field. Level and construction recipes mirror the Blockbench source tree so building placement can choose the exact visual state for a level, selector, node state, or interior prop. Once the live dev workflow can save or convert them through `PrefabStore`/`BlockSelection`, generated `.prefab.json` files should be published under the Hytale prefab path instead of editing live server files by hand.

## Runtime Direction
- Custom UI pages load packaged textures from `Common/UI/Custom/Textures/ResourceGame`.
- In-world buildings remain block-first until the official prefab serialization pass is wired into the local server workflow.
- Hytale prefab placement should use `PrefabStore.getAssetPrefab(...)` or `PrefabStore.getAssetPrefabFromAnyPack(...)`, then place the returned `BlockSelection` on the correct world thread.

## Local Hot Asset Reload
- Run `powershell -ExecutionPolicy Bypass -File scripts/sync-local-hot-assets.ps1 -GenerateAssets -WaitForAssetMonitor` to mirror generated `Common/` and `Server/` assets into `C:\Users\TJ\Documents\HyTaleDevServer\mods\tavall-hytale-resource-game-hot-assets`.
- This creates a folder-based development asset pack with its own `manifest.json`, matching Hytale's documented asset-pack format of a zip or folder with assets in the correct directories.
- Use this for UI textures, Blockbench sources, and prefab recipe iteration. Java code changes still require plugin reload or server restart.

## Hytale Visual Verification
- Run `powershell -ExecutionPolicy Bypass -File scripts/run-hytale-ui-visual-verification.ps1` while the installed Hytale client is open and connected to the local dev server.
- By default, the script writes `visual-control/resource-game-ui-request.properties` into the local server root. The plugin opens the requested UI from the server side, so the Hytale client does not need keyboard focus.
- The script captures before/after screenshots, checks the expected menu region for visual change, checks for Resource Game UI colors, restores the previous foreground window when Hytale steals focus, and writes bounded summaries under `bot-logs/hytale-visual-verification-*`.
- The verification fails when the capture is black/flat, the client is on an auth error window, the server-side control file is not acknowledged, the expected center menu region does not change, or the Resource Game UI overlay colors are not visible after opening the menu.
