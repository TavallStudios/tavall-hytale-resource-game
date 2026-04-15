# Hytale Commands (Reference)

Captured from the IGN Hytale commands list on 2026-04-13. Use `/help` in-game for the authoritative list on your server.

## Permissions

| Command | Alias | Subcommands | Parameters | Description | Required Parameter Details | Optional Parameter Details |
| --- | --- | --- | --- | --- | --- | --- |
| `/OP` |  |  |  | Commands for managing operator permissions |  |  |
| `/OP` |  | `Self` |  | Adds you to the OP permission group |  |  |

## Player & Navigation

| Command | Alias | Subcommands | Parameters | Description | Required Parameter Details | Optional Parameter Details |
| --- | --- | --- | --- | --- | --- | --- |
| `/hub` | `/converge, convergence` |  |  | Return to the Cosmos of Creativity hub |  |  |
| `/move` |  |  | `[--options]` | Move your selection and its contents by the specified amount in the direction you're looking, or in a specified direction |  | <direction> <distance> |
| `/ping` |  |  | `[--options]` | Network ping information |  | --player: An online player --detail: show detailed ping information |
| `/ping` |  | `clear` | `[--options]` | Clear ping information history |  |  |
| `/ping` |  | `graph` | `[--options]` | Print ping information as a graph |  |  |
| `/tp` | `/teleport` |  |  | Teleports a player to a location |  |  |
| `/tp` | `/teleport` | `home` |  | Teleports you to your home |  |  |
| `/tp` | `/teleport` | `top` |  | Teleport to the highest block above you |  |  |
| `/tp` | `/teleport` | `back` | `[--options]` | Teleport to the most recent teleport location |  | --count: Number of locations to move back in history |
| `/tp` | `/teleport` | `forward` | `[--options]` | Teleport to the next teleport location |  | --count: Number of locations to move forward in history |
| `/tp` | `/teleport` | `history` |  | Dumps teleport history to console |  |  |
| `/whereami` |  |  |  | Prints the current location of a player |  |  |
| `/who` |  |  |  | List who is on the server |  |  |
| `/whoami` | `/uuid` |  |  | Prints your player information |  | <player> |

## Time

| Command | Alias | Subcommands | Parameters | Description | Required Parameter Details | Optional Parameter Details |
| --- | --- | --- | --- | --- | --- | --- |
| `/time` |  |  | `[--options]` | Get the world time |  | --world: The world to operate this command on |
| `/time` | `/daytime` | `<time>` | `[--options]` | Set the world time | time: The hour of the day (between 0 and 24) to set the time to | --world: The world to operate this command on |

## Blocks & World

| Command | Alias | Subcommands | Parameters | Description | Required Parameter Details | Optional Parameter Details |
| --- | --- | --- | --- | --- | --- | --- |
| `/block` | `/blocks` |  |  | block-related commands |  |  |
| `/block` | `/blocks` | `set` | `<x y z> <block> [--options]` | set a block | x y z: The block position block: What block to set to | --world: The world to operate this command on |
| `/block` | `/blocks` | `get` | `<x y z> [--options]` | get a block | x y z: The block position | --world: The world to operate this command on |
| `/block` | `/blocks` | `getstate` | `<x y z> [--options]` | Get a blockstate | x y z: The block position | --world: The world to operate this command on |
| `/block` | `/blocks` | `row` | `<wildcard block query>` | Spawn base blocks matching a wildcard in a line from your feet in the direction you're facing | Wildcard block query: Example: "Furniture_*_Table" |  |
| `/block` | `/blocks` | `setstate` | `<x y z> <state> [--options]` |  | x y z: The block position State: State | --world: The world to operate this command on |
| `/environment` | `/setenvironment, /setenv` |  |  | Sets the environment in the selected area |  |  |

## Selection & Builder Tools

| Command | Alias | Subcommands | Parameters | Description | Required Parameter Details | Optional Parameter Details |
| --- | --- | --- | --- | --- | --- | --- |
| `/buildertoolslegend` |  |  | `[--options]` | Show or hide the builder tools legend |  | --hide: Hide the builder tools legend instead of showing it |
| `/clearblocks` | `/clear` |  |  | Set all blocks in your selection to Empty, or all blocks within a specified area |  |  |
| `/clearblocks` | `/clear` |  | `<positionOne> <positionTwo>` | Set all blocks in your selection to Empty, or all blocks within a specified area | positionOne: The first position of the cube that you want to clear positionTwo: The second position of the cube that you want to clear |  |
| `/clearedithistory` |  |  |  | Clear clipboard history |  |  |
| `/contractselection` | `/contract` |  | `<distance> [--options]` | Make your selection smaller in all directions, or by the specified amount in a direction. | <distance> is an integer (a whole number) - the number of blocks you'll move your selection inwards | --axis |
| `/copy` |  |  | `<xMin> <yMin> <zMin> <xMax> <yMax> <zMax> [--options]` | Copy the contents of your selection in the world, storing them in your clipboard to paste |  | xMin: The minimum X coordinate yMin: The minimum Y coordinate zMin: The minimum Z coordinate xMax: The maximum X coordinate yMax: The maximum Y coordinate zMax: The maximum Z coordinate --keepanchors to keep anchor blocks in the copy, --empty to include empty blocks, --noEntities to exclude entities (included by default) and --onlyEntities to copy only entities, no blocks. |
| `/cut` |  |  | `<xMin> <yMin> <zMin> <xMax> <yMax> <zMax> [--options]` | Cut the contents of your selection in the world, storing them in your clipboard to paste. |  | xMin: The minimum X coordinate yMin: The minimum Y coordinate zMin: The minimum Z coordinate xMax: The maximum X coordinate yMax: The maximum Y coordinate zMax: The maximum Z coordinate--keepanchors to keep anchor blocks in the copy, --empty to include empty blocks, --noEntities to exclude entities (included by default) and --onlyEntities to copy only entities, no blocks. |
| `/deselect` | `/clearselection` |  |  | Clear your current selection |  |  |
| `/editline` |  |  | `<start> <end> <material> [--options]` | Draw a line of blocks between two points | Start and end position (x y z, supports - for relative), material: block material pattern (e.g. Rock_Stone or Rock_Stone, Soil_Grass) | --origin: Shape origin point (Center, Bottom, Top) --density: Random chance (1-100) that each block is placed (default: 100) --wallThickness: Wall thickness for hollow lines (0= solid) --spacing: Block interval along the line (default: 1) --height: Line height (default: 1) --width Line width (default: 1) --shape: Line shape (Cube, Sphere, Cylinder, Cone, etc) |
| `/expand` |  |  | `--axis, --distance` | Expand your selection in all directions, or by the specified amount in a direction |  | --axis: The axis to expand along --distance: The distance to expand the selection |
| `/extendface` |  |  |  | Extend the target face |  |  |
| `/extendface` |  |  | `<x> <y> <z> <normalX> <normalY> <normalZ> <toolParam> <shapeRange> <blockType> <xMin> <yMin> <zMin> <xMax> <yMax> <zMax>` | Extend the target face | X: the X coordinate y: The Y coordinate z: The Z coordinate normalX: the normal X component normalY: The normal Y component normalZ: The normal Z component toolParam: The tool parameter (extrude depth) shapeRange: The shape range (radius allowed) blockType: The block type key xMin: The minimum X coordinate yMin: The minimum Y coordinate zMin: The minimum Z coordinate xMax: The maximum X coordinate yMax: The maximum Y coordinate zMax: The maximum Z coordinate |  |
| `/fillblocks` | `/fill` |  | `<pattern>` | Fill all air within your selection with the specified block types | Pattern: A list of block types to replace air with. Examples: [Rock_Stone] [20%Rock_Stone,80%Rock_Shale]. [50%Rock_Stone\ | Yaw=90, 50%Fluid_Water] |
| `/flip` |  |  |  | Flip the blocks in your clipboard |  | <direction> A direction relative to the player (Forward, backward, left, right, up, down) |
| `/gmask` |  |  | `<mask>` | Sets the global block mask |  | mask: the block mask to set |
| `/gmask` |  | `clear` |  | Clears/disables the global block mask |  |  |
| `/hollow` |  |  | `[--options]` | Sets the material on the inside of the selection, excluding walls of the set thickness |  | --thickness: The thickness of the walls to be left alone (Default: 1) --blockType: The type of block to set the inside to (Default: air) --perimeter: Includes the top and bottom of the selection in the operation --roof: Includes the top of the selection in the operation --floor: Includes the bottom of the selection in the operation |
| `/pos1` |  |  | `[--options]` | Set the first position of your selection |  | --Z --x --y to specify coordinates (uses current position if not specified) |
| `/pos2` |  |  | `[--options]` | Set the second position of your selection |  | --Z --x --y to specify coordinates (uses current position if not specified) |
| `/redo` | `/r` |  |  | Redo last change |  | <count> |
| `/repairfillers` |  |  |  | Repair filler blocks in selection |  |  |
| `/replace` |  |  | `<to> [--options]` | Replace a selection from specified block(s) with a block | to: the block type to replace with | --substringSwap: Perform substring replacement on block names (e.g. Stone to Granite replaces Rock_stone with Rock_Granite) --regex: Use regex matching (from arument is treated as regex pattern) <from> |
| `/rotate` |  |  | `<angle> [options] OR <yaw> <pitch> <roll>` | Rotate your clipboard |  |  |
| `/selectchunk` |  |  |  | Select a chunk |  |  |
| `/selectchunksection` |  |  |  | Select a chunk section |  |  |
| `/selectionhistory` |  |  | `<enabled>` | Record selection box changes in the undo/redo history | <enabled> : Whether to enable selection history recording (true/false) |  |
| `/setblocks` | `/set` |  | `<pattern>` | Set all blocks in your selection to the specified blocktype | <pattern> The blocktype to set your selection to. Examples: [Rock_Stone] [20%Rock_Stone,80%Rock_Shale]. [50%Rock_Stone\ | Yaw=90, 50%Fluid_Water] |
| `/settoolhistorysize` |  |  | `<historylength>` | Set the amount of historical builder tool actions that can be stored in history | historyLength: The length of the builder tool history |  |
| `/shift` |  |  | `[--options]` | Shift selection |  | --axis: The axis to shift along --distance: The distance to shift the selection |
| `/stack` |  |  | `[--options]` | Stack selection |  | <direction> <count> --spacing --empty: Stack only empty blocks |
| `/submerge` | `/flood` |  | `<fluid-item>` | Submerges the selection with a fluid. Use 'Empty' to unsubmerge the section. | fluid-item: The type of fluid to submerge with. |  |
| `/tint` |  |  | `<color>` | Tints the selection with the specified color | color: The color to tint with |  |
| `/updateselection` |  |  | `<xMin> <yMin> <zMin> <xMax> <yMax> <zMax>` | Update selection | xMin: The minimum X coordinate yMin: The minimum Y coordinate zMin: The minimum Z coordinate xMax: The maximum X coordinate yMax: The maximum Y coordinate zMax: The maximum Z coordinate |  |

## Prefabs

| Command | Alias | Subcommands | Parameters | Description | Required Parameter Details | Optional Parameter Details |
| --- | --- | --- | --- | --- | --- | --- |
| `/prefab` | `/p` | `save` |  | Save a prefab |  |  |
| `/prefab` | `/p` | `load` |  | Load a prefab |  |  |
| `/prefab` | `/p` | `delete` | `<name> [--options]` | Delete a prefab |  |  |
| `/prefab` | `/p` | `list` | `[--options]` | List prefabs |  |  |

## Utilities & Import

| Command | Alias | Subcommands | Parameters | Description | Required Parameter Details | Optional Parameter Details |
| --- | --- | --- | --- | --- | --- | --- |
| `/emote` |  |  | `<emote>` | Play an emote | Emote: The emote you wish to play |  |
| `/help` | `/?` |  |  | Display command help |  | <command> |
| `/importimage` |  |  |  | Open the image to blocks import tool |  |  |
| `/importobj` | `/obj` |  |  | Open the OBJ to voxel import tool |  |  |
| `/itemstate` |  |  | `<state>` | Set the state of the item currently held | state: The item state to set |  |

## Entities

| Command | Alias | Subcommands | Parameters | Description | Required Parameter Details | Optional Parameter Details |
| --- | --- | --- | --- | --- | --- | --- |
| `/clearentities` |  |  |  |  |  |  |

## Other

| Command | Alias | Subcommands | Parameters | Description | Required Parameter Details | Optional Parameter Details |
| --- | --- | --- | --- | --- | --- | --- |
| `/hotbar` |  |  | `<hotbarSlot> [--options]` | Sets your hotbar to a previously saved value | hotbarSlot: The hotbar slot between 0 and 9 to set your hotbar to | --save: Save the hotbar slot to a number instead of loading it |
| `/paste` |  |  |  | Paste the contents of your clipboard |  | <position> |
| `/undo` | `/u` |  |  | Undo last change |  |  |
| `/undo` | `/u` |  | `<count>` | Undo last change | count: The number of operations to undo |  |
| `/wall` | `/side, /walls, /sides` |  | `<pattern> [--options]` | Sets the material on the walls of your selection with the specified thickness | pattern: The type of block to set the walls to | --thickness: The thickness of the wall (default of 1) --perimeter: Includes the top and bottom of the selection in the operation --roof: Includes the top of the selection in the operation --floor: Includes the bottom of the selection in the operation |
