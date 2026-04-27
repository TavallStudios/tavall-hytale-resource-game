from __future__ import annotations

import json
import math
import uuid
from pathlib import Path
from typing import Callable

from PIL import Image, ImageDraw, ImageFont


REPO_ROOT = Path(__file__).resolve().parents[1]
RESOURCE_ROOT = REPO_ROOT / "src" / "main" / "resources"
TEXTURE_ROOT = RESOURCE_ROOT / "Common" / "UI" / "Custom" / "Textures" / "ResourceGame"
MODEL_ROOT = RESOURCE_ROOT / "Common" / "Models" / "ResourceGame"
PREFAB_ROOT = RESOURCE_ROOT / "Common" / "Prefabs" / "ResourceGame"
ASSET_MANIFEST = TEXTURE_ROOT / "resource-game-ui-assets.json"
MODEL_ASSET_MANIFEST = MODEL_ROOT / "resource-game-model-assets.json"
MAX_BUILDING_LEVEL = 30

PALETTE = {
    "parchment": (216, 195, 148, 255),
    "parchment_dark": (126, 94, 50, 255),
    "wood": (70, 57, 44, 255),
    "wood_dark": (45, 38, 31, 255),
    "wood_light": (106, 81, 54, 255),
    "stone": (141, 138, 125, 255),
    "stone_dark": (93, 92, 85, 255),
    "stone_light": (208, 206, 192, 255),
    "iron": (77, 89, 96, 255),
    "iron_light": (200, 208, 210, 255),
    "blue": (46, 95, 147, 255),
    "blue_light": (159, 208, 255, 255),
    "green": (63, 125, 74, 255),
    "red": (141, 52, 52, 255),
    "red_light": (199, 102, 84, 255),
    "gold": (184, 138, 47, 255),
    "gold_light": (224, 189, 99, 255),
    "transparent": (0, 0, 0, 0),
}


def ensure_dirs() -> None:
    for directory in [
        TEXTURE_ROOT / "panels",
        TEXTURE_ROOT / "buttons",
        TEXTURE_ROOT / "icons",
        TEXTURE_ROOT / "selectors",
        TEXTURE_ROOT / "fonts",
        TEXTURE_ROOT / "examples",
        MODEL_ROOT / "textures",
        PREFAB_ROOT,
    ]:
        directory.mkdir(parents=True, exist_ok=True)


def enforce_symmetry(image: Image.Image) -> Image.Image:
    pixels = image.load()
    width, height = image.size
    for y in range((height + 1) // 2):
        for x in range((width + 1) // 2):
            color = pixels[x, y]
            pixels[width - 1 - x, y] = color
            pixels[x, height - 1 - y] = color
            pixels[width - 1 - x, height - 1 - y] = color
    return image


def draw_centered_text(
        draw: ImageDraw.ImageDraw,
        box: tuple[int, int, int, int],
        text: str,
        font: ImageFont.FreeTypeFont | ImageFont.ImageFont,
        fill: tuple[int, int, int, int],
        stroke_fill: tuple[int, int, int, int],
        stroke_width: int,
) -> None:
    text_box = draw.textbbox((0, 0), text, font=font, stroke_width=stroke_width)
    text_width = text_box[2] - text_box[0]
    text_height = text_box[3] - text_box[1]
    x = box[0] + ((box[2] - box[0]) - text_width) // 2 - text_box[0]
    y = box[1] + ((box[3] - box[1]) - text_height) // 2 - text_box[1]
    draw.text((x, y), text, font=font, fill=fill, stroke_width=stroke_width, stroke_fill=stroke_fill)


def font_candidates() -> list[Path]:
    windows_font_dir = Path("C:/Windows/Fonts")
    return [
        windows_font_dir / "georgiab.ttf",
        windows_font_dir / "trebucbd.ttf",
        windows_font_dir / "arialbd.ttf",
        windows_font_dir / "segoeuib.ttf",
    ]


def load_font(size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    for candidate in font_candidates():
        if candidate.exists():
            return ImageFont.truetype(str(candidate), size)
    return ImageFont.load_default()


def save_image(path: Path, image: Image.Image, symmetrical: bool = True) -> dict[str, object]:
    path.parent.mkdir(parents=True, exist_ok=True)
    output = enforce_symmetry(image) if symmetrical else image
    output.save(path)
    return {
        "path": str(path.relative_to(RESOURCE_ROOT)).replace("\\", "/"),
        "width": output.size[0],
        "height": output.size[1],
        "symmetry": "horizontal+vertical" if symmetrical else "centered-reference-grid",
    }


def make_panel(
        size: tuple[int, int],
        center: tuple[int, int, int, int],
        frame: tuple[int, int, int, int],
        trim: tuple[int, int, int, int],
        accent: tuple[int, int, int, int],
) -> Image.Image:
    image = Image.new("RGBA", size, PALETTE["transparent"])
    draw = ImageDraw.Draw(image)
    width, height = size
    draw.rounded_rectangle((0, 0, width - 1, height - 1), radius=36, fill=frame)
    draw.rounded_rectangle((12, 12, width - 13, height - 13), radius=28, fill=trim)
    draw.rounded_rectangle((28, 28, width - 29, height - 29), radius=20, fill=center)
    for inset in (8, 22):
        draw.rectangle((inset, height // 2 - 2, width - inset - 1, height // 2 + 1), fill=accent)
        draw.rectangle((width // 2 - 2, inset, width // 2 + 1, height - inset - 1), fill=accent)
    for radius in (9, 15, 21):
        draw.ellipse((28 - radius, 28 - radius, 28 + radius, 28 + radius), fill=accent)
    for step in range(0, min(width, height) // 2, 28):
        color = tuple(max(0, channel - 12) if index < 3 else channel for index, channel in enumerate(center))
        draw.rectangle((width // 2 - step // 2, height // 2 - 1, width // 2 + step // 2, height // 2 + 1), fill=color)
    return image


def make_button(
        size: tuple[int, int],
        center: tuple[int, int, int, int],
        frame: tuple[int, int, int, int],
        accent: tuple[int, int, int, int],
        pressed: bool = False,
) -> Image.Image:
    image = Image.new("RGBA", size, PALETTE["transparent"])
    draw = ImageDraw.Draw(image)
    width, height = size
    top = 5 if pressed else 2
    bottom = height - (3 if pressed else 6)
    draw.rounded_rectangle((0, top, width - 1, bottom), radius=12, fill=frame)
    draw.rounded_rectangle((5, top + 5, width - 6, bottom - 5), radius=8, fill=center)
    draw.rectangle((14, top + 8, width - 15, top + 12), fill=accent)
    draw.rectangle((14, bottom - 12, width - 15, bottom - 8), fill=accent)
    draw.ellipse((14, height // 2 - 6, 26, height // 2 + 6), fill=accent)
    draw.ellipse((width // 2 - 5, top + 10, width // 2 + 5, top + 20), fill=accent)
    return image


def mirror_icon(draw_top_left: Callable[[ImageDraw.ImageDraw, int], None], size: int = 128) -> Image.Image:
    half = size // 2
    quadrant = Image.new("RGBA", (half, half), PALETTE["transparent"])
    draw = ImageDraw.Draw(quadrant)
    draw_top_left(draw, half)
    image = Image.new("RGBA", (size, size), PALETTE["transparent"])
    image.alpha_composite(quadrant, (0, 0))
    image.alpha_composite(quadrant.transpose(Image.Transpose.FLIP_LEFT_RIGHT), (half, 0))
    image.alpha_composite(quadrant.transpose(Image.Transpose.FLIP_TOP_BOTTOM), (0, half))
    image.alpha_composite(quadrant.transpose(Image.Transpose.FLIP_LEFT_RIGHT).transpose(Image.Transpose.FLIP_TOP_BOTTOM), (half, half))
    return image


def icon_castle(draw: ImageDraw.ImageDraw, half: int) -> None:
    draw.rectangle((20, 34, half, half), fill=PALETTE["stone"])
    draw.rectangle((9, 20, 26, half), fill=PALETTE["stone_dark"])
    draw.rectangle((9, 13, 16, 22), fill=PALETTE["stone_light"])
    draw.rectangle((24, 13, 31, 22), fill=PALETTE["stone_light"])
    draw.rectangle((38, 12, half, 24), fill=PALETTE["blue"])
    draw.rectangle((42, 24, half, half), fill=PALETTE["stone_light"])
    draw.rectangle((half - 16, half - 24, half, half), fill=PALETTE["wood_dark"])


def icon_resource(fill: tuple[int, int, int, int], trim: tuple[int, int, int, int]) -> Callable[[ImageDraw.ImageDraw, int], None]:
    def draw_icon(draw: ImageDraw.ImageDraw, half: int) -> None:
        draw.rounded_rectangle((12, 18, half, half - 6), radius=10, fill=fill)
        draw.rectangle((20, 26, half, 31), fill=trim)
        draw.ellipse((half - 22, half - 22, half, half), fill=trim)
        draw.line((half - 28, half - 6, half, half - 6), fill=trim, width=4)
    return draw_icon


def icon_worker(draw: ImageDraw.ImageDraw, half: int) -> None:
    draw.ellipse((25, 12, half, 42), fill=PALETTE["gold_light"])
    draw.rectangle((31, 39, half, half), fill=PALETTE["green"])
    draw.rectangle((14, 49, half, half), fill=PALETTE["wood"])
    draw.rectangle((48, 22, half, 27), fill=PALETTE["wood_dark"])


def icon_troop(draw: ImageDraw.ImageDraw, half: int) -> None:
    draw.pieslice((14, 10, half + 16, half + 12), 180, 270, fill=PALETTE["iron"])
    draw.rectangle((20, 38, half, half), fill=PALETTE["iron_light"])
    draw.rectangle((33, 14, half, 24), fill=PALETTE["blue"])
    draw.rectangle((half - 12, 42, half, half), fill=PALETTE["wood_dark"])


def icon_hammer(draw: ImageDraw.ImageDraw, half: int) -> None:
    draw.rectangle((16, 45, half, 55), fill=PALETTE["wood_light"])
    draw.rectangle((42, 20, half, 36), fill=PALETTE["iron"])
    draw.rectangle((51, 11, half, 43), fill=PALETTE["iron_light"])


def icon_hourglass(draw: ImageDraw.ImageDraw, half: int) -> None:
    draw.rectangle((22, 12, half, 20), fill=PALETTE["iron_light"])
    draw.polygon([(28, 23), (half, 23), (half, half), (47, half)], fill=PALETTE["gold_light"])
    draw.rectangle((22, half - 7, half, half), fill=PALETTE["iron"])


def icon_blocked(draw: ImageDraw.ImageDraw, half: int) -> None:
    draw.pieslice((12, 12, half + 34, half + 34), 180, 270, fill=PALETTE["red"])
    draw.rectangle((30, 30, half, 40), fill=PALETTE["red_light"])
    draw.rectangle((40, 30, 50, half), fill=PALETTE["red_light"])


def icon_move(draw: ImageDraw.ImageDraw, half: int) -> None:
    draw.polygon([(half - 8, 10), (half, 2), (half, 44), (half - 8, 36)], fill=PALETTE["blue_light"])
    draw.polygon([(10, half - 8), (2, half), (44, half), (36, half - 8)], fill=PALETTE["blue_light"])
    draw.rectangle((half - 12, half - 12, half, half), fill=PALETTE["iron_light"])


def icon_building(fill: tuple[int, int, int, int], roof: tuple[int, int, int, int]) -> Callable[[ImageDraw.ImageDraw, int], None]:
    def draw_icon(draw: ImageDraw.ImageDraw, half: int) -> None:
        draw.rectangle((15, half - 14, half, half), fill=PALETTE["stone_dark"])
        draw.rectangle((20, 34, half, half), fill=fill)
        draw.polygon([(15, 36), (half, 12), (half, 36)], fill=roof)
        draw.rectangle((half - 14, half - 24, half, half), fill=PALETTE["wood_dark"])
    return draw_icon


def make_selector(valid: bool) -> Image.Image:
    image = Image.new("RGBA", (256, 256), PALETTE["transparent"])
    draw = ImageDraw.Draw(image)
    body = (60, 139, 220, 72) if valid else (198, 54, 54, 72)
    rail = PALETTE["blue_light"] if valid else PALETTE["red_light"]
    draw.rectangle((54, 54, 202, 202), outline=rail, width=5, fill=body)
    for x in (54, 176):
        for y in (54, 176):
            draw.rectangle((x, y, x + 26, y + 6), fill=rail)
            draw.rectangle((x, y, x + 6, y + 26), fill=rail)
    draw.rectangle((84, 84, 172, 172), outline=rail, width=3)
    draw.line((128, 54, 128, 202), fill=rail, width=2)
    draw.line((54, 128, 202, 128), fill=rail, width=2)
    return image


def make_radius_ring() -> Image.Image:
    image = Image.new("RGBA", (256, 256), PALETTE["transparent"])
    draw = ImageDraw.Draw(image)
    for inset, alpha in ((18, 150), (42, 90), (66, 55)):
        color = (159, 208, 255, alpha)
        draw.ellipse((inset, inset, 255 - inset, 255 - inset), outline=color, width=7)
    draw.line((128, 18, 128, 238), fill=(159, 208, 255, 130), width=3)
    draw.line((18, 128, 238, 128), fill=(159, 208, 255, 130), width=3)
    return image


def make_font_template(path: Path, title: str, glyphs: str, font_size: int, columns: int, cell: tuple[int, int]) -> dict[str, object]:
    rows = math.ceil(len(glyphs) / columns)
    width = columns * cell[0] + 64
    height = rows * cell[1] + 128
    image = Image.new("RGBA", (width, height), PALETTE["wood_dark"])
    draw = ImageDraw.Draw(image)
    font = load_font(font_size)
    title_font = load_font(36)
    draw.rounded_rectangle((12, 12, width - 13, height - 13), radius=28, fill=PALETTE["wood"])
    draw.rounded_rectangle((28, 28, width - 29, height - 29), radius=20, outline=PALETTE["gold_light"], width=4)
    draw_centered_text(draw, (28, 24, width - 28, 84), title, title_font, PALETTE["gold_light"], PALETTE["wood_dark"], 2)
    for index, glyph in enumerate(glyphs):
        col = index % columns
        row = index // columns
        x0 = 32 + col * cell[0]
        y0 = 96 + row * cell[1]
        x1 = x0 + cell[0] - 8
        y1 = y0 + cell[1] - 8
        draw.rounded_rectangle((x0, y0, x1, y1), radius=10, fill=PALETTE["parchment"])
        draw.rounded_rectangle((x0 + 5, y0 + 5, x1 - 5, y1 - 5), radius=7, outline=PALETTE["parchment_dark"], width=2)
        draw_centered_text(draw, (x0, y0, x1, y1), glyph, font, PALETTE["wood_dark"], PALETTE["gold_light"], max(1, font_size // 16))
    return save_image(path, image, symmetrical=False)


def make_palette_texture(path: Path, base: tuple[int, int, int, int], accent: tuple[int, int, int, int]) -> None:
    image = Image.new("RGBA", (64, 64), base)
    draw = ImageDraw.Draw(image)
    for offset in range(0, 64, 16):
        draw.rectangle((offset, 0, offset + 6, 64), fill=accent)
        draw.rectangle((0, offset, 64, offset + 6), fill=accent)
    enforce_symmetry(image).save(path)


def cube_element(
        name: str,
        start: tuple[float, float, float],
        end: tuple[float, float, float],
        color: int,
        texture: int = 0,
) -> dict[str, object]:
    element_uuid = str(uuid.uuid5(uuid.NAMESPACE_URL, f"resource-game:{name}:{start}:{end}"))
    faces = {
        face: {"uv": [0, 0, 16, 16], "texture": texture}
        for face in ["north", "east", "south", "west", "up", "down"]
    }
    return {
        "name": name,
        "uuid": element_uuid,
        "type": "cube",
        "from": list(start),
        "to": list(end),
        "origin": [0, 0, 0],
        "color": color,
        "faces": faces,
    }


def write_bbmodel(
        name: str,
        display_name: str,
        elements: list[dict[str, object]],
        texture_file: str,
        output_path: Path | None = None,
        identifier: str | None = None,
        texture_reference: str | None = None,
) -> Path:
    texture_path = texture_reference
    if texture_path is None:
        texture_path = texture_file if "/" in texture_file else f"textures/{texture_file}"
    model = {
        "meta": {
            "format_version": "4.10",
            "model_format": "free",
            "box_uv": True,
        },
        "name": display_name,
        "model_identifier": identifier or f"resource_game:{name}",
        "visible_box": [3, 3, 3],
        "variable_placeholders": "",
        "resolution": {"width": 64, "height": 64},
        "elements": elements,
        "outliner": [element["uuid"] for element in elements],
        "textures": [
            {
                "path": texture_path,
                "name": texture_file,
                "folder": "textures",
                "namespace": "",
                "id": "0",
                "particle": True,
                "render_mode": "default",
                "visible": True,
                "mode": "bitmap",
                "saved": True,
                "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, f"resource-game-texture:{name}")),
                "relative_path": texture_path,
                "source": texture_path,
            }
        ],
    }
    output = output_path or (MODEL_ROOT / f"{name}.bbmodel")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
    return output


def write_prefab_recipe(
        name: str,
        anchor: tuple[int, int, int],
        blocks: list[dict[str, object]],
        output_path: Path | None = None,
        prefab_target: str | None = None,
) -> Path:
    recipe = {
        "schema": "resource-game-prefab-recipe/v1",
        "name": f"resource_game:{name}",
        "hytalePrefabTarget": prefab_target or f"prefabs/resource_game/{name}.prefab.json",
        "anchor": {"x": anchor[0], "y": anchor[1], "z": anchor[2]},
        "symmetry": "horizontal+vertical around anchor",
        "notes": [
            "This recipe is a source-of-truth block plan for generating a Hytale BlockSelection prefab.",
            "Use PrefabStore.getAssetPrefab or an in-game /editprefab save pass once the official prefab serializer is available in the dev workflow.",
        ],
        "blocks": blocks,
    }
    output = output_path or (PREFAB_ROOT / f"{name}.resource-prefab.json")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(recipe, indent=2) + "\n", encoding="utf-8")
    return output


def symmetrical_blocks(name: str, base_block: str, accent_block: str, radius: int, height: int) -> list[dict[str, object]]:
    blocks: list[dict[str, object]] = []
    seen: set[tuple[int, int, int]] = set()

    def add(x: int, y: int, z: int, block: str) -> None:
        for sx in {x, -x}:
            for sz in {z, -z}:
                key = (sx, y, sz)
                if key not in seen:
                    seen.add(key)
                    blocks.append({"x": sx, "y": y, "z": sz, "block": block})

    for x in range(0, radius + 1):
        for z in range(0, radius + 1):
            add(x, 0, z, base_block)
    for level in range(1, height + 1):
        add(radius, level, radius, accent_block)
        add(0, level, radius, accent_block)
        add(radius, level, 0, accent_block)
    add(0, height + 1, 0, accent_block)
    return blocks


BUILDING_ASSET_CONFIGS = {
    "farmstead": {
        "display": "Farmstead",
        "texture": "farmstead_texture.png",
        "base": "Rock_Shale",
        "accent": "Rock_Stone",
        "core_color": 2,
        "roof_color": 5,
        "accent_color": 4,
    },
    "lumber_mill": {
        "display": "Lumber Mill",
        "texture": "lumber_mill_texture.png",
        "base": "Rock_Shale",
        "accent": "Rock_Stone_Brick",
        "core_color": 2,
        "roof_color": 4,
        "accent_color": 5,
    },
    "iron_works": {
        "display": "Iron Works",
        "texture": "iron_works_texture.png",
        "base": "Rock_Stone_Brick",
        "accent": "Rock_Quartzite",
        "core_color": 3,
        "roof_color": 4,
        "accent_color": 5,
    },
    "barracks": {
        "display": "Barracks",
        "texture": "barracks_texture.png",
        "base": "Rock_Stone_Brick",
        "accent": "Rock_Stone",
        "core_color": 2,
        "roof_color": 3,
        "accent_color": 4,
    },
    "workshop": {
        "display": "Workshop",
        "texture": "workshop_texture.png",
        "base": "Rock_Shale",
        "accent": "Rock_Quartzite",
        "core_color": 2,
        "roof_color": 4,
        "accent_color": 5,
    },
}

CONSTRUCTION_STAGES = ("foundation", "scaffolding", "shell", "complete")

RESOURCE_NODE_ASSET_CONFIGS = {
    "food": {
        "display": "Food Node",
        "texture": "food_node_texture.png",
        "base": "Soil_Grass",
        "accent": "Rock_Shale",
        "base_color": 2,
        "accent_color": 5,
        "state_color": 4,
    },
    "wood": {
        "display": "Wood Node",
        "texture": "wood_node_texture.png",
        "base": "Rock_Shale",
        "accent": "Rock_Stone",
        "base_color": 2,
        "accent_color": 4,
        "state_color": 5,
    },
    "iron": {
        "display": "Iron Node",
        "texture": "iron_node_texture.png",
        "base": "Rock_Stone",
        "accent": "Rock_Quartzite",
        "base_color": 3,
        "accent_color": 5,
        "state_color": 4,
    },
}

INTERIOR_PROP_ASSET_CONFIGS = {
    "citizen_anchor": "Citizen Anchor",
    "troop_anchor": "Troop Anchor",
    "worker_platform": "Worker Platform",
    "exit_portal": "Exit Portal",
    "hologram_pedestal": "Hologram Pedestal",
    "placement_selector_valid": "Valid Placement Selector",
    "placement_selector_blocked": "Blocked Placement Selector",
    "castle_radius_marker": "Castle Radius Marker",
    "node_radius_marker": "Node Radius Marker",
}


def relative_repo(path: Path) -> str:
    return str(path.relative_to(REPO_ROOT)).replace("\\", "/")


def tier_for_level(level: int) -> int:
    return min(6, 1 + ((level - 1) // 5))


def building_level_elements(building_name: str, level: int, config: dict[str, object]) -> list[dict[str, object]]:
    tier = tier_for_level(level)
    radius = 13 + tier
    core_height = 9 + tier * 3 + (level % 5)
    roof_y = core_height + 3
    core_color = int(config["core_color"])
    roof_color = int(config["roof_color"])
    accent_color = int(config["accent_color"])
    elements = [
        cube_element(f"{building_name}_level_{level:02d}_pad", (-radius, 0, -radius), (radius, 3, radius), 1),
        cube_element(f"{building_name}_level_{level:02d}_core", (-8 - tier, 3, -8 - tier), (8 + tier, core_height, 8 + tier), core_color),
        cube_element(f"{building_name}_level_{level:02d}_roof", (-10 - tier, core_height, -10 - tier), (10 + tier, roof_y, 10 + tier), roof_color),
        cube_element(f"{building_name}_level_{level:02d}_north_wing", (-5, 4, -radius), (5, 6 + tier, -9), accent_color),
        cube_element(f"{building_name}_level_{level:02d}_south_wing", (-5, 4, 9), (5, 6 + tier, radius), accent_color),
        cube_element(f"{building_name}_level_{level:02d}_east_wing", (9, 4, -5), (radius, 6 + tier, 5), accent_color),
        cube_element(f"{building_name}_level_{level:02d}_west_wing", (-radius, 4, -5), (-9, 6 + tier, 5), accent_color),
        cube_element(f"{building_name}_level_{level:02d}_cap", (-4, roof_y, -4), (4, roof_y + 3 + tier, 4), accent_color),
    ]
    if building_name == "farmstead":
        elements.extend([
            cube_element(f"crop_x_{level:02d}", (-radius - 4, 2, -2), (radius + 4, 4, 2), accent_color),
            cube_element(f"crop_z_{level:02d}", (-2, 2, -radius - 4), (2, 4, radius + 4), accent_color),
        ])
    elif building_name == "lumber_mill":
        elements.extend([
            cube_element(f"saw_x_{level:02d}", (-radius - 6, 8 + tier, -2), (radius + 6, 11 + tier, 2), accent_color),
            cube_element(f"saw_z_{level:02d}", (-2, 8 + tier, -radius - 6), (2, 11 + tier, radius + 6), accent_color),
        ])
    elif building_name == "iron_works":
        chimney_height = roof_y + 9 + tier
        elements.extend([
            cube_element(f"chimney_ne_{level:02d}", (radius - 7, 3, radius - 7), (radius - 1, chimney_height, radius - 1), accent_color),
            cube_element(f"chimney_sw_{level:02d}", (-radius + 1, 3, -radius + 1), (-radius + 7, chimney_height, -radius + 7), accent_color),
        ])
    elif building_name == "barracks":
        tower_height = roof_y + 5 + tier
        elements.extend([
            cube_element(f"tower_ne_{level:02d}", (radius - 6, 3, radius - 6), (radius, tower_height, radius), accent_color),
            cube_element(f"tower_sw_{level:02d}", (-radius, 3, -radius), (-radius + 6, tower_height, -radius + 6), accent_color),
        ])
    elif building_name == "workshop":
        elements.extend([
            cube_element(f"tool_rack_x_{level:02d}", (-radius - 2, 7 + tier, -2), (radius + 2, 10 + tier, 2), accent_color),
            cube_element(f"tool_rack_z_{level:02d}", (-2, 7 + tier, -radius - 2), (2, 10 + tier, radius + 2), accent_color),
        ])
    return elements


def castle_level_elements(level: int) -> list[dict[str, object]]:
    tier = tier_for_level(level)
    radius = 18 + tier
    keep_height = 18 + tier * 4 + (level % 5)
    tower_height = keep_height + 8 + tier
    return [
        cube_element(f"castle_level_{level:02d}_foundation", (-radius, 0, -radius), (radius, 5, radius), 1),
        cube_element(f"castle_level_{level:02d}_keep", (-10 - tier, 5, -10 - tier), (10 + tier, keep_height, 10 + tier), 2),
        cube_element(f"castle_level_{level:02d}_north_tower", (-radius, 5, -radius), (-radius + 8, tower_height, -radius + 8), 3),
        cube_element(f"castle_level_{level:02d}_south_tower", (radius - 8, 5, radius - 8), (radius, tower_height, radius), 3),
        cube_element(f"castle_level_{level:02d}_east_tower", (radius - 8, 5, -radius), (radius, tower_height, -radius + 8), 3),
        cube_element(f"castle_level_{level:02d}_west_tower", (-radius, 5, radius - 8), (-radius + 8, tower_height, radius), 3),
        cube_element(f"castle_level_{level:02d}_banner_x", (-radius - 2, keep_height - 2, -2), (radius + 2, keep_height + 3, 2), 4),
        cube_element(f"castle_level_{level:02d}_banner_z", (-2, keep_height - 2, -radius - 2), (2, keep_height + 3, radius + 2), 4),
        cube_element(f"castle_level_{level:02d}_crown", (-5, keep_height, -5), (5, keep_height + 5 + tier, 5), 5),
    ]


def construction_stage_elements(building_name: str, stage: str, config: dict[str, object]) -> list[dict[str, object]]:
    core_color = int(config["core_color"])
    accent_color = int(config["accent_color"])
    elements = [cube_element(f"{building_name}_{stage}_pad", (-15, 0, -15), (15, 3, 15), 1)]
    if stage in {"scaffolding", "shell", "complete"}:
        elements.extend([
            cube_element(f"{building_name}_{stage}_post_ne", (11, 3, 11), (15, 18, 15), accent_color),
            cube_element(f"{building_name}_{stage}_post_nw", (-15, 3, 11), (-11, 18, 15), accent_color),
            cube_element(f"{building_name}_{stage}_post_se", (11, 3, -15), (15, 18, -11), accent_color),
            cube_element(f"{building_name}_{stage}_post_sw", (-15, 3, -15), (-11, 18, -11), accent_color),
        ])
    if stage in {"shell", "complete"}:
        elements.extend([
            cube_element(f"{building_name}_{stage}_shell_core", (-9, 3, -9), (9, 15, 9), core_color),
            cube_element(f"{building_name}_{stage}_roof_rim_x", (-15, 15, -3), (15, 19, 3), accent_color),
            cube_element(f"{building_name}_{stage}_roof_rim_z", (-3, 15, -15), (3, 19, 15), accent_color),
        ])
    if stage == "complete":
        elements.append(cube_element(f"{building_name}_{stage}_cap", (-5, 19, -5), (5, 23, 5), accent_color))
    return elements


def level_prefab_blocks(base_block: str, accent_block: str, level: int, castle: bool = False) -> list[dict[str, object]]:
    radius = (3 if castle else 2) + ((level - 1) // 10)
    height = (4 if castle else 2) + ((level - 1) // 5)
    blocks = symmetrical_blocks("level", base_block, accent_block, radius, height)
    for marker in range(1, tier_for_level(level) + 1):
        blocks.append({"x": marker, "y": height + 2, "z": 0, "block": accent_block})
        blocks.append({"x": -marker, "y": height + 2, "z": 0, "block": accent_block})
        blocks.append({"x": 0, "y": height + 2, "z": marker, "block": accent_block})
        blocks.append({"x": 0, "y": height + 2, "z": -marker, "block": accent_block})
    return blocks


def node_state_elements(resource_name: str, state: str, config: dict[str, object]) -> list[dict[str, object]]:
    height = 18 if state == "full" else 10
    radius = 13 if state == "full" else 10
    base_color = int(config["base_color"])
    accent_color = int(config["accent_color"])
    state_color = int(config["state_color"])
    return [
        cube_element(f"{resource_name}_{state}_base", (-radius, 0, -radius), (radius, 4, radius), base_color),
        cube_element(f"{resource_name}_{state}_core", (-7, 4, -7), (7, height, 7), accent_color),
        cube_element(f"{resource_name}_{state}_vein_x", (-radius - 4, 8, -2), (radius + 4, 12, 2), state_color),
        cube_element(f"{resource_name}_{state}_vein_z", (-2, 8, -radius - 4), (2, 12, radius + 4), state_color),
        cube_element(f"{resource_name}_{state}_cap", (-4, height, -4), (4, height + 4, 4), state_color),
    ]


def prop_elements(prop_name: str) -> list[dict[str, object]]:
    if "selector" in prop_name:
        color = 4 if "valid" in prop_name else 5
        return [
            cube_element(f"{prop_name}_floor", (-16, 0, -16), (16, 1, 16), color),
            cube_element(f"{prop_name}_north", (-16, 1, -16), (16, 3, -14), color),
            cube_element(f"{prop_name}_south", (-16, 1, 14), (16, 3, 16), color),
            cube_element(f"{prop_name}_east", (14, 1, -16), (16, 3, 16), color),
            cube_element(f"{prop_name}_west", (-16, 1, -16), (-14, 3, 16), color),
        ]
    if "radius" in prop_name:
        return [
            cube_element(f"{prop_name}_ring_north", (-20, 0, -20), (20, 2, -17), 4),
            cube_element(f"{prop_name}_ring_south", (-20, 0, 17), (20, 2, 20), 4),
            cube_element(f"{prop_name}_ring_east", (17, 0, -20), (20, 2, 20), 4),
            cube_element(f"{prop_name}_ring_west", (-20, 0, -20), (-17, 2, 20), 4),
            cube_element(f"{prop_name}_center", (-2, 0, -2), (2, 5, 2), 5),
        ]
    if prop_name == "exit_portal":
        return [
            cube_element("exit_portal_left", (-10, 0, -2), (-6, 22, 2), 4),
            cube_element("exit_portal_right", (6, 0, -2), (10, 22, 2), 4),
            cube_element("exit_portal_top", (-10, 18, -2), (10, 22, 2), 5),
            cube_element("exit_portal_core", (-5, 4, -1), (5, 18, 1), 3),
        ]
    if prop_name == "worker_platform":
        return [
            cube_element("worker_platform_pad", (-18, 0, -12), (18, 3, 12), 2),
            cube_element("worker_platform_table", (-8, 3, -5), (8, 7, 5), 3),
            cube_element("worker_platform_banner_x", (-18, 7, -1), (18, 10, 1), 4),
            cube_element("worker_platform_banner_z", (-1, 7, -12), (1, 10, 12), 4),
        ]
    return [
        cube_element(f"{prop_name}_base", (-8, 0, -8), (8, 4, 8), 2),
        cube_element(f"{prop_name}_post", (-3, 4, -3), (3, 18, 3), 3),
        cube_element(f"{prop_name}_cap", (-6, 18, -6), (6, 23, 6), 4),
        cube_element(f"{prop_name}_marker_x", (-12, 11, -1), (12, 14, 1), 5),
        cube_element(f"{prop_name}_marker_z", (-1, 11, -12), (1, 14, 12), 5),
    ]


def prop_prefab_blocks(prop_name: str) -> list[dict[str, object]]:
    if "selector" in prop_name or "radius" in prop_name:
        return symmetrical_blocks(prop_name, "Rock_Quartzite", "Rock_Stone", 3, 1)
    if prop_name == "exit_portal":
        return symmetrical_blocks(prop_name, "Metal_Iron", "Rock_Quartzite", 1, 4)
    return symmetrical_blocks(prop_name, "Rock_Stone", "Rock_Quartzite", 2, 3)


def append_model_entry(entries: list[dict[str, object]], category: str, name: str, model_path: Path, prefab_path: Path | None = None, **metadata: object) -> None:
    entry: dict[str, object] = {
        "category": category,
        "name": name,
        "model": relative_repo(model_path),
    }
    if prefab_path is not None:
        entry["prefabRecipe"] = relative_repo(prefab_path)
    entry.update(metadata)
    entries.append(entry)


def generate_models() -> list[str]:
    model_specs = {
        "castle_keep": {
            "display": "Resource Game Castle Keep",
            "texture": ("castle_keep_texture.png", PALETTE["stone"], PALETTE["blue"]),
            "elements": [
                cube_element("foundation", (-18, 0, -18), (18, 4, 18), 2),
                cube_element("keep_core", (-10, 4, -10), (10, 26, 10), 2),
                cube_element("north_tower", (-18, 4, -18), (-10, 30, -10), 3),
                cube_element("south_tower", (10, 4, 10), (18, 30, 18), 3),
                cube_element("east_tower", (10, 4, -18), (18, 30, -10), 3),
                cube_element("west_tower", (-18, 4, 10), (-10, 30, 18), 3),
                cube_element("crest_cap", (-6, 26, -6), (6, 34, 6), 4),
            ],
        },
        "farmstead": {
            "display": "Resource Game Farmstead",
            "texture": ("farmstead_texture.png", PALETTE["wood_light"], PALETTE["green"]),
            "elements": [
                cube_element("field_pad", (-18, 0, -18), (18, 2, 18), 1),
                cube_element("farmhouse", (-9, 2, -9), (9, 13, 9), 2),
                cube_element("roof", (-13, 13, -13), (13, 19, 13), 4),
                cube_element("crop_cross_x", (-18, 2, -3), (18, 4, 3), 5),
                cube_element("crop_cross_z", (-3, 2, -18), (3, 4, 18), 5),
            ],
        },
        "lumber_mill": {
            "display": "Resource Game Lumber Mill",
            "texture": ("lumber_mill_texture.png", PALETTE["wood"], PALETTE["gold"]),
            "elements": [
                cube_element("log_pad", (-16, 0, -16), (16, 3, 16), 1),
                cube_element("mill_house", (-8, 3, -8), (8, 15, 8), 2),
                cube_element("saw_beam_x", (-20, 9, -3), (20, 13, 3), 4),
                cube_element("saw_beam_z", (-3, 9, -20), (3, 13, 20), 4),
                cube_element("roof", (-12, 15, -12), (12, 20, 12), 3),
            ],
        },
        "iron_works": {
            "display": "Resource Game Iron Works",
            "texture": ("iron_works_texture.png", PALETTE["iron"], PALETTE["red"]),
            "elements": [
                cube_element("forge_pad", (-16, 0, -16), (16, 3, 16), 1),
                cube_element("forge_core", (-10, 3, -10), (10, 14, 10), 2),
                cube_element("chimney_a", (-16, 3, -16), (-9, 28, -9), 3),
                cube_element("chimney_b", (9, 3, 9), (16, 28, 16), 3),
                cube_element("ember_cross_x", (-16, 4, -3), (16, 7, 3), 4),
                cube_element("ember_cross_z", (-3, 4, -16), (3, 7, 16), 4),
            ],
        },
        "barracks": {
            "display": "Resource Game Barracks",
            "texture": ("barracks_texture.png", PALETTE["stone"], PALETTE["blue"]),
            "elements": [
                cube_element("training_pad", (-18, 0, -18), (18, 3, 18), 1),
                cube_element("hall", (-14, 3, -8), (14, 16, 8), 2),
                cube_element("tower_a", (-18, 3, -18), (-10, 24, -10), 3),
                cube_element("tower_b", (10, 3, 10), (18, 24, 18), 3),
                cube_element("banner_cross_x", (-18, 16, -2), (18, 21, 2), 4),
                cube_element("banner_cross_z", (-2, 16, -18), (2, 21, 18), 4),
            ],
        },
        "workshop": {
            "display": "Resource Game Workshop",
            "texture": ("workshop_texture.png", PALETTE["wood"], PALETTE["gold_light"]),
            "elements": [
                cube_element("work_pad", (-15, 0, -15), (15, 3, 15), 1),
                cube_element("workshop_core", (-10, 3, -10), (10, 15, 10), 2),
                cube_element("tool_rack_x", (-18, 8, -2), (18, 13, 2), 4),
                cube_element("tool_rack_z", (-2, 8, -18), (2, 13, 18), 4),
                cube_element("roof", (-13, 15, -13), (13, 20, 13), 3),
            ],
        },
        "resource_node": {
            "display": "Resource Game Resource Node",
            "texture": ("resource_node_texture.png", PALETTE["stone_dark"], PALETTE["gold_light"]),
            "elements": [
                cube_element("node_base", (-14, 0, -14), (14, 4, 14), 1),
                cube_element("node_core", (-8, 4, -8), (8, 18, 8), 2),
                cube_element("vein_x", (-18, 8, -3), (18, 12, 3), 4),
                cube_element("vein_z", (-3, 8, -18), (3, 12, 18), 4),
                cube_element("cap", (-5, 18, -5), (5, 24, 5), 3),
            ],
        },
    }
    generated: list[str] = []
    model_manifest_entries: list[dict[str, object]] = []
    for name, spec in model_specs.items():
        texture_file, base, accent = spec["texture"]
        make_palette_texture(MODEL_ROOT / "textures" / texture_file, base, accent)
        model_path = write_bbmodel(name, spec["display"], spec["elements"], texture_file)
        generated.append(relative_repo(model_path))
        append_model_entry(model_manifest_entries, "legacy-overview", name, model_path)
    recipes = {
        "castle_keep": symmetrical_blocks("castle_keep", "Rock_Stone", "Rock_Stone_Brick", 3, 5),
        "farmstead": symmetrical_blocks("farmstead", "Rock_Shale", "Rock_Stone", 3, 3),
        "lumber_mill": symmetrical_blocks("lumber_mill", "Rock_Shale", "Rock_Stone_Brick", 3, 4),
        "iron_works": symmetrical_blocks("iron_works", "Rock_Stone_Brick", "Rock_Quartzite", 3, 5),
        "barracks": symmetrical_blocks("barracks", "Rock_Stone_Brick", "Rock_Stone", 3, 4),
        "workshop": symmetrical_blocks("workshop", "Rock_Shale", "Rock_Quartzite", 3, 4),
        "resource_node": symmetrical_blocks("resource_node", "Rock_Stone", "Rock_Quartzite", 2, 3),
    }
    for name, blocks in recipes.items():
        prefab_path = write_prefab_recipe(name, (0, 0, 0), blocks)
        generated.append(relative_repo(prefab_path))
        matching = next((entry for entry in model_manifest_entries if entry["category"] == "legacy-overview" and entry["name"] == name), None)
        if matching is not None:
            matching["prefabRecipe"] = relative_repo(prefab_path)

    for level in range(1, MAX_BUILDING_LEVEL + 1):
        model_path = write_bbmodel(
            f"castle_keep_level_{level:02d}",
            f"Resource Game Castle Keep Level {level:02d}",
            castle_level_elements(level),
            "castle_keep_texture.png",
            MODEL_ROOT / "castles" / "castle_keep" / f"level_{level:02d}.bbmodel",
            f"resource_game:castles/castle_keep/level_{level:02d}",
            "../../textures/castle_keep_texture.png",
        )
        prefab_path = write_prefab_recipe(
            f"castle_keep_level_{level:02d}",
            (0, 0, 0),
            level_prefab_blocks("Rock_Stone", "Rock_Stone_Brick", level, castle=True),
            PREFAB_ROOT / "castles" / "castle_keep" / f"level_{level:02d}.resource-prefab.json",
            f"prefabs/resource_game/castles/castle_keep/level_{level:02d}.prefab.json",
        )
        generated.extend([relative_repo(model_path), relative_repo(prefab_path)])
        append_model_entry(model_manifest_entries, "castle-level", "castle_keep", model_path, prefab_path, level=level, maxLevel=MAX_BUILDING_LEVEL)

    for building_name, config in BUILDING_ASSET_CONFIGS.items():
        for stage in CONSTRUCTION_STAGES:
            model_path = write_bbmodel(
                f"{building_name}_{stage}",
                f"{config['display']} {stage.title()}",
                construction_stage_elements(building_name, stage, config),
                str(config["texture"]),
                MODEL_ROOT / "buildings" / building_name / "construction" / f"{stage}.bbmodel",
                f"resource_game:buildings/{building_name}/construction/{stage}",
                "../../../textures/" + str(config["texture"]),
            )
            prefab_path = write_prefab_recipe(
                f"{building_name}_{stage}",
                (0, 0, 0),
                level_prefab_blocks(str(config["base"]), str(config["accent"]), max(1, CONSTRUCTION_STAGES.index(stage) + 1)),
                PREFAB_ROOT / "buildings" / building_name / "construction" / f"{stage}.resource-prefab.json",
                f"prefabs/resource_game/buildings/{building_name}/construction/{stage}.prefab.json",
            )
            generated.extend([relative_repo(model_path), relative_repo(prefab_path)])
            append_model_entry(model_manifest_entries, "building-construction", building_name, model_path, prefab_path, stage=stage)

        for level in range(1, MAX_BUILDING_LEVEL + 1):
            model_path = write_bbmodel(
                f"{building_name}_level_{level:02d}",
                f"{config['display']} Level {level:02d}",
                building_level_elements(building_name, level, config),
                str(config["texture"]),
                MODEL_ROOT / "buildings" / building_name / f"level_{level:02d}.bbmodel",
                f"resource_game:buildings/{building_name}/level_{level:02d}",
                "../../textures/" + str(config["texture"]),
            )
            prefab_path = write_prefab_recipe(
                f"{building_name}_level_{level:02d}",
                (0, 0, 0),
                level_prefab_blocks(str(config["base"]), str(config["accent"]), level),
                PREFAB_ROOT / "buildings" / building_name / f"level_{level:02d}.resource-prefab.json",
                f"prefabs/resource_game/buildings/{building_name}/level_{level:02d}.prefab.json",
            )
            generated.extend([relative_repo(model_path), relative_repo(prefab_path)])
            append_model_entry(model_manifest_entries, "building-level", building_name, model_path, prefab_path, level=level, maxLevel=MAX_BUILDING_LEVEL)

    for resource_name, config in RESOURCE_NODE_ASSET_CONFIGS.items():
        make_palette_texture(MODEL_ROOT / "textures" / str(config["texture"]), PALETTE["stone_dark"], PALETTE["gold_light"])
        for state in ("full", "depleted"):
            model_path = write_bbmodel(
                f"{resource_name}_node_{state}",
                f"{config['display']} {state.title()}",
                node_state_elements(resource_name, state, config),
                str(config["texture"]),
                MODEL_ROOT / "nodes" / resource_name / f"{state}.bbmodel",
                f"resource_game:nodes/{resource_name}/{state}",
                "../../textures/" + str(config["texture"]),
            )
            prefab_path = write_prefab_recipe(
                f"{resource_name}_node_{state}",
                (0, 0, 0),
                level_prefab_blocks(str(config["base"]), str(config["accent"]), 8 if state == "full" else 2),
                PREFAB_ROOT / "nodes" / resource_name / f"{state}.resource-prefab.json",
                f"prefabs/resource_game/nodes/{resource_name}/{state}.prefab.json",
            )
            generated.extend([relative_repo(model_path), relative_repo(prefab_path)])
            append_model_entry(model_manifest_entries, "resource-node", resource_name, model_path, prefab_path, state=state)

    make_palette_texture(MODEL_ROOT / "textures" / "interior_prop_texture.png", PALETTE["wood"], PALETTE["blue_light"])
    for prop_name, display_name in INTERIOR_PROP_ASSET_CONFIGS.items():
        model_path = write_bbmodel(
            prop_name,
            f"Resource Game {display_name}",
            prop_elements(prop_name),
            "interior_prop_texture.png",
            MODEL_ROOT / "props" / f"{prop_name}.bbmodel",
            f"resource_game:props/{prop_name}",
            "../textures/interior_prop_texture.png",
        )
        prefab_path = write_prefab_recipe(
            prop_name,
            (0, 0, 0),
            prop_prefab_blocks(prop_name),
            PREFAB_ROOT / "props" / f"{prop_name}.resource-prefab.json",
            f"prefabs/resource_game/props/{prop_name}.prefab.json",
        )
        generated.extend([relative_repo(model_path), relative_repo(prefab_path)])
        append_model_entry(model_manifest_entries, "interior-prop", prop_name, model_path, prefab_path)

    write_model_manifest(model_manifest_entries)
    return generated


def write_model_manifest(entries: list[dict[str, object]]) -> None:
    category_counts: dict[str, int] = {}
    for entry in entries:
        category = str(entry["category"])
        category_counts[category] = category_counts.get(category, 0) + 1
    manifest = {
        "schema": "resource-game-model-assets/v1",
        "generatedBy": "scripts/generate-resource-game-assets.py",
        "assetRoot": "Common/Models/ResourceGame",
        "prefabRoot": "Common/Prefabs/ResourceGame",
        "maxBuildingLevel": MAX_BUILDING_LEVEL,
        "categoryCounts": category_counts,
        "assets": entries,
    }
    MODEL_ASSET_MANIFEST.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")


def generate_textures() -> list[dict[str, object]]:
    generated: list[dict[str, object]] = []
    panels = {
        "ui_panel_castle_ledger_base.png": make_panel((512, 512), PALETTE["parchment"], PALETTE["wood_dark"], PALETTE["wood"], PALETTE["blue"]),
        "ui_panel_war_table_base.png": make_panel((512, 512), PALETTE["wood_dark"], PALETTE["iron"], PALETTE["wood"], PALETTE["blue_light"]),
        "ui_panel_workshop_base.png": make_panel((512, 512), PALETTE["stone"], PALETTE["wood_dark"], PALETTE["iron"], PALETTE["gold"]),
        "ui_panel_node_detail_base.png": make_panel((512, 512), PALETTE["stone_dark"], PALETTE["wood"], PALETTE["stone"], PALETTE["green"]),
        "ui_panel_interior_base.png": make_panel((512, 512), PALETTE["parchment"], PALETTE["stone_dark"], PALETTE["wood_light"], PALETTE["gold_light"]),
    }
    for filename, image in panels.items():
        generated.append(save_image(TEXTURE_ROOT / "panels" / filename, image))

    button_specs = {
        "ui_button_primary_normal.png": (PALETTE["blue"], PALETTE["iron"], PALETTE["blue_light"], False),
        "ui_button_primary_hover.png": (PALETTE["blue_light"], PALETTE["iron"], PALETTE["gold_light"], False),
        "ui_button_primary_pressed.png": (PALETTE["blue"], PALETTE["stone_dark"], PALETTE["gold"], True),
        "ui_button_primary_disabled.png": (PALETTE["stone_dark"], PALETTE["iron"], PALETTE["stone"], False),
        "ui_button_secondary_normal.png": (PALETTE["wood"], PALETTE["iron"], PALETTE["gold"], False),
        "ui_button_confirm_normal.png": (PALETTE["green"], PALETTE["iron"], PALETTE["gold_light"], False),
        "ui_button_danger_normal.png": (PALETTE["red"], PALETTE["iron"], PALETTE["red_light"], False),
        "ui_button_icon_square_normal.png": (PALETTE["stone"], PALETTE["iron"], PALETTE["gold"], False),
    }
    for filename, spec in button_specs.items():
        generated.append(save_image(TEXTURE_ROOT / "buttons" / filename, make_button((256, 64), *spec)))

    icon_specs: dict[str, Callable[[ImageDraw.ImageDraw, int], None]] = {
        "ui_icon_kingdom_castle.png": icon_castle,
        "ui_icon_resource_wood.png": icon_resource(PALETTE["wood_light"], PALETTE["wood_dark"]),
        "ui_icon_resource_stone.png": icon_resource(PALETTE["stone"], PALETTE["stone_light"]),
        "ui_icon_resource_iron.png": icon_resource(PALETTE["iron"], PALETTE["iron_light"]),
        "ui_icon_resource_food.png": icon_resource(PALETTE["green"], PALETTE["gold_light"]),
        "ui_icon_resource_gold.png": icon_resource(PALETTE["gold"], PALETTE["gold_light"]),
        "ui_icon_population_worker.png": icon_worker,
        "ui_icon_population_troop.png": icon_troop,
        "ui_icon_action_upgrade.png": icon_hammer,
        "ui_icon_action_time.png": icon_hourglass,
        "ui_icon_action_move.png": icon_move,
        "ui_icon_action_blocked.png": icon_blocked,
        "ui_icon_building_farmstead.png": icon_building(PALETTE["wood_light"], PALETTE["green"]),
        "ui_icon_building_lumber_mill.png": icon_building(PALETTE["wood"], PALETTE["gold"]),
        "ui_icon_building_iron_works.png": icon_building(PALETTE["iron"], PALETTE["red"]),
        "ui_icon_building_barracks.png": icon_building(PALETTE["stone"], PALETTE["blue"]),
        "ui_icon_building_workshop.png": icon_building(PALETTE["wood"], PALETTE["gold_light"]),
        "ui_icon_node_marker.png": icon_building(PALETTE["stone_dark"], PALETTE["gold_light"]),
    }
    for filename, draw_func in icon_specs.items():
        generated.append(save_image(TEXTURE_ROOT / "icons" / filename, mirror_icon(draw_func)))

    selectors = {
        "ui_selector_building_valid.png": make_selector(True),
        "ui_selector_building_blocked.png": make_selector(False),
        "ui_selector_radius_ring.png": make_radius_ring(),
        "ui_selector_corner_valid.png": make_selector(True).resize((128, 128), Image.Resampling.NEAREST),
    }
    for filename, image in selectors.items():
        generated.append(save_image(TEXTURE_ROOT / "selectors" / filename, image))

    generated.append(make_font_template(
        TEXTURE_ROOT / "fonts" / "font_template_big_alphabet.png",
        "Noble Frontier Display Alphabet",
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
        72,
        7,
        (220, 150),
    ))
    generated.append(make_font_template(
        TEXTURE_ROOT / "fonts" / "font_template_menu_alphabet.png",
        "Noble Frontier Menu Alphabet",
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz",
        40,
        8,
        (180, 110),
    ))
    generated.append(make_font_template(
        TEXTURE_ROOT / "fonts" / "font_template_numbers_symbols.png",
        "Noble Frontier Numbers Symbols",
        "0123456789+-*/=%#@!?.,:;()[]{}<>",
        48,
        8,
        (160, 110),
    ))

    examples = {
        "ui_example_castle_steward_ledger.png": panels["ui_panel_castle_ledger_base.png"].resize((768, 432), Image.Resampling.BICUBIC),
        "ui_example_war_table_controls.png": panels["ui_panel_war_table_base.png"].resize((768, 432), Image.Resampling.BICUBIC),
        "ui_example_workshop_upgrade.png": panels["ui_panel_workshop_base.png"].resize((768, 432), Image.Resampling.BICUBIC),
    }
    for filename, image in examples.items():
        generated.append(save_image(TEXTURE_ROOT / "examples" / filename, image))

    return generated


def write_manifest(texture_entries: list[dict[str, object]], model_entries: list[str]) -> None:
    manifest = {
        "schema": "resource-game-ui-assets/v1",
        "generatedBy": "scripts/generate-resource-game-assets.py",
        "assetRoot": "Common/UI/Custom/Textures/ResourceGame",
        "symmetryPolicy": "Decorative UI assets are mirrored horizontally and vertically. Font reference sheets keep glyphs readable inside symmetrical cells.",
        "uiTextures": texture_entries,
        "blockbenchAndPrefabSources": model_entries,
    }
    ASSET_MANIFEST.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    ensure_dirs()
    texture_entries = generate_textures()
    model_entries = generate_models()
    write_manifest(texture_entries, model_entries)
    print(f"Generated {len(texture_entries)} UI texture assets")
    print(f"Generated {len(model_entries)} model/prefab source assets")


if __name__ == "__main__":
    main()
