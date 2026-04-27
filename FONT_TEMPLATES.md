# Font Templates

## Purpose
Define the custom game-font direction for generated UI art and future hand-authored glyph assets.

The current font direction is `Noble Frontier`: carved medieval serif forms, dark ink fills, warm gold edge accents, and high contrast against parchment or dark wood panels.

## Template Sheets
- Big display alphabet: `src/main/resources/Common/UI/Custom/Textures/ResourceGame/fonts/font_template_big_alphabet.png`
- Menu alphabet: `src/main/resources/Common/UI/Custom/Textures/ResourceGame/fonts/font_template_menu_alphabet.png`
- Numbers and symbols: `src/main/resources/Common/UI/Custom/Textures/ResourceGame/fonts/font_template_numbers_symbols.png`

## Usage
- Big display glyphs are for title banners, castle names, building headers, and major state callouts.
- Menu glyphs are for normal labels, button text, stat labels, timers, tooltips, and debug menu copy.
- Number and symbol glyphs are for resources, costs, countdowns, levels, coordinates, and compact status lines.

## Production Rules
- Keep glyphs centered in symmetrical cells when making new templates.
- Do not bake game copy into panel or button images.
- Use dark ink fills with gold beveling for parchment and stone panels.
- Use light parchment fills with dark outline only when the glyph sits on dark wood or war-table panels.
- Keep `0/O`, `1/I`, `5/S`, `8/B`, `+`, `-`, and `%` readable at small menu size before approving a font set.
