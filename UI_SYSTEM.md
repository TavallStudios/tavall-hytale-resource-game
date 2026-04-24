# UI System

## Purpose
Provide readable prototype UIs using Hytale CustomUI pages without pushing gameplay state into page files.

## Responsibilities
- Register and open typed pages.
- Keep page logic in Java page models and UI action routing.
- Preserve placeholder polish while staying replaceable.
- Support debug/admin navigation and node detail workflows.

## Main classes
- `UiPageRegistry`
- `UiNavigator`
- `UiActionService`
- Page models in `ui/`
- `.ui` documents in `src/main/resources/Common/UI/Custom/Pages/`

## Current pages
- Castle main, info, citizens, troops, resources, upgrades
- Interior main
- Debug navigator
- Resource node detail

## Important boundary
- The `.ui` files define visual structure.
- Page models compute values.
- `UiActionService` performs side effects and reopens pages with updated state.
- `UiNavigator` now tracks the last open page per player so economy ticks and admin mutations can refresh live castle/resource/node pages without duplicating page-routing logic elsewhere.

## Links to other systems
- Castle system and node system open pages from world interaction.
- Interaction system resolves which world target should open a page in the first place.
- Population/resource systems provide computed action state.
- Debug command system uses UI navigation as a fast development tool.

## Notes
- CustomUI document loading was previously a failure point. Keep standalone page documents simple and asset-pack shipping explicit.
- Join-time auto-open is intentionally disabled so players can enter the world normally.
