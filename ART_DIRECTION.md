# UI Art Direction

## Purpose
Define the visual direction for CustomUI menus, interactive buttons, selector previews, world interaction prompts, and future packaged UI assets.

The game is a kingdom resource-management experience inside Hytale. UI should feel like a playable kingdom command surface, not a generic web dashboard. Assets should make castle ownership, building upgrades, resources, troops, citizens, and placement actions readable at a glance.

## Recommended Direction: Noble Frontier

Use this as the default direction unless a specific feature calls for a variant.

### Mood
- Practical medieval frontier, not high-fantasy luxury.
- Built from stone, iron, carved wood, cloth banners, wax seals, and resource crates.
- Clean enough for fast reading, but textured enough to feel diegetic.
- UI should feel like a command table used by a castle steward.

### Shape Language
- Panels: squared parchment or dark carved-wood boards with subtle bevels.
- Buttons: compact stone, iron, or wood plates with a small icon socket on the left.
- Primary actions: iron-rimmed blue banners or polished stone buttons.
- Dangerous actions: muted red wax seal or cracked red stone accent.
- Disabled actions: desaturated stone with low-contrast icon and no glow.
- Upgrade/progress controls: horizontal ledger bars, not modern progress pills.
- Placement selectors: translucent blue-white glass volume, thin bright corner rails, and a small red invalid outline.

### Palette
- Parchment: `#d8c394`, `#b99b62`, `#6f5530`
- Charcoal wood: `#2d261f`, `#46392c`, `#6a5136`
- Castle stone: `#8d8a7d`, `#5d5c55`, `#d0cec0`
- Iron: `#4d5960`, `#77858c`, `#c8d0d2`
- Kingdom blue: `#2e5f93`, `#4c8cca`, `#9fd0ff`
- Action green: `#3f7d4a`, `#7fb069`
- Warning red: `#8d3434`, `#c76654`
- Gold accent: `#b88a2f`, `#e0bd63`

### Texture Rules
- Prefer hand-painted voxel-adjacent texture over flat vector art.
- Keep noise subtle; UI text and numbers must remain readable.
- Use edge highlights and corner caps to show click targets.
- Avoid glossy mobile-game gradients, neon sci-fi, or overly ornate fantasy trim.
- Avoid one-color palettes. Every menu should include stone/wood plus one role color.

## Direction Examples

### Example A: Castle Steward Ledger
Best for castle overview, citizens, troop assignments, resources, and economy pages.

- Background: parchment ledger over dark oak frame.
- Header: blue cloth banner with small castle crest.
- Buttons: carved wood plates with iron corner pins.
- Icons: simplified resource stamps, troop helmets, citizen silhouettes, crates, coins, and tools.
- State language: green wax check for available, gold hourglass for in progress, red cracked seal for blocked.
- Asset prompt: `hand-painted medieval kingdom UI ledger, parchment panel in dark oak frame, blue cloth banner header, small readable resource icons, iron corner pins, voxel fantasy game style, clean high-contrast game interface, no text`

### Example B: War Table Controls
Best for placement mode, building selection, castle radius rules, node claims, and command/admin UIs.

- Background: dark wood table with faint grid, map marks, and clipped parchment notes.
- Selector: translucent blue-white box with bright white corners and red invalid ghost outline.
- Buttons: iron plates with engraved placement symbols.
- Icons: hammer, move arrows, rotate, confirm check, cancel X, radius ring, blocked tile.
- State language: blue glow for selected, gold pulse for confirmable, red outline for invalid placement.
- Asset prompt: `voxel fantasy strategy game placement UI, dark wooden war table, translucent blue selection cube, white corner rails, red invalid outline, iron icon buttons, readable high contrast, Hytale inspired, no text`

### Example C: Castle Workshop
Best for per-building upgrade screens, timers, production, cancel controls, and building stats.

- Background: stone workshop panel with wood shelves and small material bins.
- Header: stone nameplate with building icon socket.
- Progress: forge-heated bar or parchment construction timeline.
- Buttons: tool-shaped icon plates for upgrade, cancel, assign, inspect, and collect.
- Icons: hammer/anvil, saw, crate, clock, worker, stone block, food basket, troop banner.
- State language: glowing ember edge for active upgrade, grey ash overlay for unavailable, red cracked tool for cancel.
- Asset prompt: `hand-painted medieval workshop game UI, stone and wood upgrade panel, forge ember progress bar, compact tool icon buttons, resource bins, voxel fantasy aesthetic, readable interface, no text`

## UI Asset Families

### Menu Panels
- `panel_castle_ledger`: parchment center, oak frame, blue banner top.
- `panel_war_table`: dark wood/map surface, strong grid readability.
- `panel_workshop`: stone/wood hybrid with resource-bin detail.
- `panel_node_detail`: rough stone/grass edge with resource vein accent.
- `panel_interior`: warmer candlelit wood and stone, less battlefield contrast.

### Buttons
- `button_primary`: blue banner strip over iron-rimmed stone.
- `button_secondary`: carved wood plate with iron pins.
- `button_confirm`: green wax seal or green-stained stone edge.
- `button_danger`: red wax seal or cracked red stone edge.
- `button_disabled`: grey stone, low saturation, no glow.
- `button_icon_square`: 32x32 or 48x48 icon plate for compact toolbars.

### Icons
- Resources: wood log, stone block, iron ingot, food basket, gold coin, crystal shard.
- Kingdom: castle, banner, crown, population silhouettes, worker hand, troop helmet.
- Buildings: quarry, farm, barracks, workshop, storehouse, watchtower, castle keep.
- Actions: upgrade hammer, assign hand, cancel X, move arrows, rotate, confirm check, inspect eye, timer hourglass.
- Placement: selector cube, radius ring, blocked tile, valid tile, node marker, castle marker.

### Interaction States
- Hover/focus: thin blue-white edge glow.
- Pressed: inset shadow and reduced top highlight.
- Selected: blue cloth tab or glowing corner caps.
- Available: clear icon, normal contrast, subtle edge light.
- In progress: gold hourglass or moving ledger stripe.
- Blocked: red border, cracked seal, desaturated center.
- Cooldown/time left: gold/iron progress bar with small hourglass icon.

## Menu-Specific Direction

### Castle Main
- Use `Castle Steward Ledger`.
- Show a crest, Might, resources, population, and building shortcuts as organized ledger rows.
- Primary actions should use banner-blue buttons.

### Resource Node Detail
- Use `panel_node_detail` with stone/grass/resource vein trim.
- Resource type should control a small accent icon, not the entire palette.
- Assignment buttons should use worker-hand and route icons.

### Building Upgrade UI
- Use `Castle Workshop`.
- Always show level, max level, current stats, next stats, timer/progress, cost, cancel state, and blocked reason when unavailable.
- The action area should reserve stable space so timer text and cancel buttons do not move the layout.

### Placement Selector UI
- Use `War Table Controls`.
- The preview should resemble Hytale-style selection tools: translucent volume, strong corner rails, visible size number, red invalid target outline.
- In-world blocked zones should read clearly around castle radius, node radius, and other protected placements.

### Interior UI
- Use a softer variant of `Castle Steward Ledger`.
- War-table styling should stay outside interiors unless the page is explicitly tactical.
- NPC interaction buttons should feel like small parchment dialogue options attached to the same kingdom UI language.

## Production Rules

- Every icon must work at small sizes first.
- Do not bake text into image assets; CustomUI should own text.
- Keep button states as separate assets or atlas regions.
- Prefer square power-of-two source assets where practical: 32, 64, 128, 256.
- Use transparent backgrounds for icons and button overlays.
- Use consistent lighting: top-left highlight, bottom-right shadow.
- Keep contrast high enough for resource counts, timers, and blocked placement warnings.
- Assets shipped with the plugin must live under the normal Maven resource packaging path.
- Generated production assets and the repeatable asset command live in [ASSET_PIPELINE.md](./ASSET_PIPELINE.md).
- Custom alphabet/font reference sheets live in [FONT_TEMPLATES.md](./FONT_TEMPLATES.md).

## Naming Convention

- Panels: `ui_panel_<context>_<variant>`
- Buttons: `ui_button_<role>_<state>`
- Icons: `ui_icon_<domain>_<name>`
- Selectors: `ui_selector_<purpose>_<state>`
- Badges: `ui_badge_<meaning>_<state>`

Examples:
- `ui_panel_castle_ledger_base`
- `ui_button_primary_hover`
- `ui_icon_resource_wood`
- `ui_icon_action_upgrade`
- `ui_selector_building_valid`
- `ui_selector_building_blocked`

## Future Asset Prompts

### Icon Sheet
`game UI icon sheet for medieval kingdom resource management, hand-painted voxel fantasy style, transparent background, wood log, stone block, iron ingot, food basket, gold coin, castle, worker, troop helmet, hammer, hourglass, route arrow, blocked tile, consistent top-left lighting, crisp readable silhouettes, no text`

### Button State Sheet
`medieval fantasy game UI button state sheet, compact rectangular buttons, carved wood, iron rim, blue cloth primary variant, green confirm variant, red danger variant, grey disabled variant, hover glow, pressed inset state, transparent background, no text`

### Placement Selector Sheet
`Hytale inspired building placement selector UI assets, translucent blue-white selection cube, white corner rails, red invalid outline, green valid tile marker, radius ring, blocked castle zone marker, voxel fantasy strategy game style, transparent background, no text`

### Building Upgrade Panel
`medieval castle workshop UI panel for game building upgrades, stone and wood frame, forge ember progress bar, resource bins, compact icon sockets, readable high contrast, hand-painted voxel fantasy style, no text`
