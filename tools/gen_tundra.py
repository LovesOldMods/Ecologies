"""Tundra and lichen woodland resources. Run from gen_resources.py."""
import json
import zipfile
from pathlib import Path

NS = "ecologies"
ROOT = Path(__file__).resolve().parent.parent
# Vanilla biome files come out of the jars Gradle sets up, so run a build once first.
ARTIFACTS = Path(__file__).resolve().parent.parent / "versions"


def rl(n):
    return n if ":" in n else f"{NS}:{n}"


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def vanilla_biome(v, name):
    entry = f"data/minecraft/worldgen/biome/{name}.json"
    for jar in sorted((ARTIFACTS / v / "build/moddev/artifacts").glob("*.jar")):
        if jar.name.endswith("-sources.jar"):
            continue
        with zipfile.ZipFile(jar) as z:
            if entry in z.namelist():
                return json.loads(z.read(entry))
    raise SystemExit(f"Couldn't find vanilla {name} for {v}. Run ./gradlew :{v}:build once first.")


# --------------------------------------------------------------------------- blocks

LOGS = ["dwarf_spruce_log", "stripped_dwarf_spruce_log"]
CROWNS = ["dwarf_spruce_log_post_crown", "stripped_dwarf_spruce_log_post_crown"]
SHRUBS = ["stunted_red_shrub", "orange_shrub", "yellow_shrub"]
MOSSES = ["red_moss_block", "orange_moss_block", "yellow_moss_block"]
CARPETS = [m.replace("_block", "_carpet") for m in MOSSES]

LANG = {
    "block.ecologies.smooth_basalt_iron_ore": "Smooth Basalt Iron Ore",
    "block.ecologies.dwarf_spruce_log": "Dwarf Spruce Log",
    "block.ecologies.stripped_dwarf_spruce_log": "Stripped Dwarf Spruce Log",
    "block.ecologies.dwarf_spruce_log_post_crown": "Dwarf Spruce Log Crown",
    "block.ecologies.stripped_dwarf_spruce_log_post_crown": "Stripped Dwarf Spruce Log Crown",
    "block.ecologies.stunted_red_shrub": "Stunted Red Shrub",
    "block.ecologies.orange_shrub": "Orange Shrub",
    "block.ecologies.dwarf_spruce_sapling": "Dwarf Spruce Sapling",
    "block.ecologies.yellow_shrub": "Yellow Shrub",
    "block.ecologies.red_shrub": "Red Shrub",
    "block.ecologies.red_moss_block": "Red Moss Block",
    "block.ecologies.orange_moss_block": "Orange Moss Block",
    "block.ecologies.yellow_moss_block": "Yellow Moss Block",
    "block.ecologies.red_moss_carpet": "Red Moss Carpet",
    "block.ecologies.orange_moss_carpet": "Orange Moss Carpet",
    "block.ecologies.yellow_moss_carpet": "Yellow Moss Carpet",
    "block.ecologies.white_ice": "White Ice",
    "block.ecologies.white_packed_ice": "White Packed Ice",
    "biome.ecologies.tundra": "Tundra",
    "biome.ecologies.lichen_woodland": "Lichen Woodland",
}


def face(uv, texture, cull=None):
    f = {"uv": uv, "texture": texture}
    if cull:
        f["cullface"] = cull
    return f


def box(frm, to, faces):
    return {"from": frm, "to": to, "faces": faces}


def templates():
    """Shared parent models (none right now)."""
    return {}


ROT = {"north": 0, "east": 90, "south": 180, "west": 270}


# keep in step with SkinnyLogBlock.sides()
SKINNY_ENDS = {"y": ("up", "down"), "x": ("east", "west"), "z": ("south", "north")}
SKINNY_SIDES = {"y": ["north", "east", "south", "west"], "x": ["up", "north", "down", "south"],
                "z": ["up", "east", "down", "west"]}


def block_assets():
    """{name: (blockstate, {model: json}, item model json)}."""
    out = {}
    for log in LOGS + CROWNS:
        width = 8 if log in LOGS else 4
        side = "minecraft:block/" + ("stripped_spruce_log" if log.startswith("stripped") else "spruce_log")
        t = {"side": side, "end": f"{NS}:block/{log}_end", "particle": side,
             # sloped top where a crown sits on the log
             "joined": f"{NS}:block/{log}_joined_top" if log in LOGS else side}
        a, b = 8 - width // 2, 8 + width // 2
        core = box([a, a, a], [b, b, b], {
            "down": face([a, a, b, b], "#end"), "up": face([a, a, b, b], "#end"),
            "north": face([a, a, b, b], "#side"), "south": face([a, a, b, b], "#side"),
            "west": face([a, a, b, b], "#side"), "east": face([a, a, b, b], "#side")})
        def arm(tip):
            return box([a, b, a], [b, 16, b], {
                "up": face([a, a, b, b], tip),
                "north": face([a, 0, b, 16 - b], "#side"), "south": face([a, 0, b, 16 - b], "#side"),
                "west": face([a, 0, b, 16 - b], "#side"), "east": face([a, 0, b, 16 - b], "#side")})
        full = box([a, 0, a], [b, 16, b], {
            "down": face([a, a, b, b], "#end", "down"), "up": face([a, a, b, b], "#end", "up"),
            "north": face([a, 0, b, 16], "#side"), "south": face([a, 0, b, 16], "#side"),
            "west": face([a, 0, b, 16], "#side"), "east": face([a, 0, b, 16], "#side")})
        # ends show rings, joins show bark
        models = {f"{log}_core": {"textures": t, "elements": [core]},
                  f"{log}_arm_end": {"textures": t, "elements": [arm("#end")]},
                  f"{log}_arm_joined": {"textures": t, "elements": [arm("#joined")]},
                  f"{log}_inventory": {"parent": "minecraft:block/block", "textures": t, "elements": [full]},
                  "skinny_log_leaves": {"parent": "minecraft:block/leaves", "render_type": "minecraft:cutout_mipped",
                                        "textures": {"all": "minecraft:block/spruce_leaves"}}}
        parts = []
        for axis, rot in (("y", {}), ("z", {"x": 90}), ("x", {"x": 90, "y": 90})):
            parts.append({"when": {"axis": axis}, "apply": {"model": f"{NS}:block/{log}_core", **rot}})
        arm_rot = {"up": {}, "down": {"x": 180}, "north": {"x": 90}, "south": {"x": 90, "y": 180},
                   "east": {"x": 90, "y": 90}, "west": {"x": 90, "y": 270}}
        # sides are relative to the axis, see SkinnyLogBlock.sides
        for axis, (plus, minus) in SKINNY_ENDS.items():
            for prop, d in (("plus", plus), ("minus", minus)):
                for kind in ("end", "joined"):
                    parts.append({"when": {"axis": axis, prop: kind}, "apply": {"model": f"{NS}:block/{log}_arm_{kind}", **arm_rot[d]}})
            for i, d in enumerate(SKINNY_SIDES[axis]):
                parts.append({"when": {"axis": axis, f"side_{i + 1}": "joined"},
                              "apply": {"model": f"{NS}:block/{log}_arm_joined", **arm_rot[d]}})
        # leafy: full leaf block drawn around the log
        parts.append({"when": {"leafy": "true"}, "apply": {"model": f"{NS}:block/skinny_log_leaves"}})
        out[log] = ({"multipart": parts}, models, {"parent": f"{NS}:block/{log}_inventory"})
    for s in SHRUBS + ["red_shrub", "dwarf_spruce_sapling"]:
        out[s] = ({"variants": {"": {"model": f"{NS}:block/{s}"}}},
                  {s: {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": f"{NS}:block/{s}"}}},
                  {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:block/{s}"}})
    for m, c in zip(MOSSES, CARPETS):
        tex = {"all": f"{NS}:block/{m}"}
        out[m] = ({"variants": {"": {"model": f"{NS}:block/{m}"}}},
                  {m: {"parent": "minecraft:block/cube_all", "textures": tex}}, {"parent": f"{NS}:block/{m}"})
        out[c] = ({"variants": {"": {"model": f"{NS}:block/{c}"}}},
                  {c: {"parent": "minecraft:block/carpet", "textures": {"wool": f"{NS}:block/{m}"}}}, {"parent": f"{NS}:block/{c}"})
    for b in ["smooth_basalt_iron_ore", "white_ice", "white_packed_ice"]:
        out[b] = ({"variants": {"": {"model": f"{NS}:block/{b}"}}},
                  {b: {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{b}"}}}, {"parent": f"{NS}:block/{b}"})
    return out


SHARED_BLOCKS = [b for b in ["smooth_basalt_iron_ore", "white_ice", "white_packed_ice", "dwarf_spruce_sapling"] + LOGS + CROWNS + SHRUBS + MOSSES + CARPETS]
ONLY_121 = ["red_shrub"]


# --------------------------------------------------------------------------- loot and recipes

def modern(v):
    return not v.startswith("1.21.1")


def loot(v, block):
    seq = {"random_sequence": f"{NS}:blocks/{block}"}
    silk = ({"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": rl(block)} if modern(v) else
            {"type": "minecraft:item", "name": rl(block), "conditions": [{"condition": "minecraft:match_tool", "predicate": {
                "predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]})
    if block == "smooth_basalt_iron_ore":
        if modern(v):
            drop = {"type": "minecraft:item", "name": "minecraft:raw_iron", "modifier": [
                {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2, "max": 4}},
                {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                {"type": "minecraft:explosion_decay"}]}
            pool = {"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [silk, drop]}]}
        else:
            drop = {"type": "minecraft:item", "name": "minecraft:raw_iron", "functions": [
                {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2.0, "max": 4.0}, "add": False},
                {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                {"function": "minecraft:explosion_decay"}]}
            pool = {"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:alternatives", "children": [silk, drop]}]}
        return {"type": "minecraft:block", "pools": [pool], **seq}
    if block in ("white_ice", "white_packed_ice"):
        pool = {"rolls": 1, "entries": [silk]}
        if not modern(v):
            pool = {"rolls": 1.0, "bonus_rolls": 0.0, "entries": [silk]}
        return {"type": "minecraft:block", "pools": [pool], **seq}
    pool = {"rolls": 1, "entries": [{"type": "minecraft:item", "name": rl(block)}]}
    if modern(v):
        pool["condition"] = {"type": "minecraft:survives_explosion"}
    else:
        pool.update({"rolls": 1.0, "bonus_rolls": 0.0, "conditions": [{"condition": "minecraft:survives_explosion"}]})
    return {"type": "minecraft:block", "pools": [pool], **seq}


def recipes():
    r = {
        "spruce_wood_from_dwarf_spruce_log": ("shapeless", ["dwarf_spruce_log"], "minecraft:spruce_wood", 1),
        "stripped_spruce_wood_from_stripped_dwarf_spruce_log": ("shapeless", ["stripped_dwarf_spruce_log"], "minecraft:stripped_spruce_wood", 1),
        "spruce_wood_from_dwarf_spruce_log_post_crowns": ("shapeless", ["dwarf_spruce_log_post_crown", "dwarf_spruce_log_post_crown"], "minecraft:spruce_wood", 1),
        "stripped_spruce_wood_from_stripped_dwarf_spruce_log_post_crowns": ("shapeless", ["stripped_dwarf_spruce_log_post_crown", "stripped_dwarf_spruce_log_post_crown"], "minecraft:stripped_spruce_wood", 1),
    }
    for m, c in zip(MOSSES, CARPETS):
        r[c] = ("shaped", ["##"], {"#": m}, c, 3)
    return r


# --------------------------------------------------------------------------- tags

def shared_tags(write_tag):
    write_tag("block", "minecraft:logs", [rl(w) for w in LOGS + CROWNS])
    write_tag("item", "minecraft:logs", [rl(w) for w in LOGS + CROWNS])
    write_tag("block", "minecraft:animals_spawnable_on", [rl(m) for m in MOSSES])
    write_tag("block", "minecraft:rabbits_spawnable_on", [rl(m) for m in MOSSES])
    write_tag("block", "minecraft:combination_step_sound_blocks", [rl(c) for c in CARPETS])
    write_tag("block", "minecraft:needs_stone_tool", [rl("smooth_basalt_iron_ore")])
    write_tag("block", "minecraft:iron_ores", [rl("smooth_basalt_iron_ore")])
    write_tag("item", "minecraft:iron_ores", [rl("smooth_basalt_iron_ore")])
    write_tag("block", "minecraft:ice", [rl("white_ice"), rl("white_packed_ice")])
    write_tag("block", "minecraft:enderman_holdable", [rl(m) for m in MOSSES])
    write_tag("block", "minecraft:saplings", [rl("dwarf_spruce_sapling")])
    write_tag("item", "minecraft:saplings", [rl("dwarf_spruce_sapling")])
    write_tag("block", "minecraft:replaceable_by_trees", [rl(s) for s in SHRUBS] + [rl(c) for c in CARPETS])
    write_tag("block", f"{NS}:moss", ["minecraft:moss_block"] + [rl(m) for m in MOSSES])
    write_tag("block", f"{NS}:moss_spreads_onto", ["#minecraft:base_stone_overworld", "minecraft:dirt", "minecraft:grass_block",
                                                   "minecraft:coarse_dirt", "minecraft:podzol", "minecraft:rooted_dirt", "minecraft:gravel",
                                                   "minecraft:cobbled_deepslate"])
    write_tag("block", f"{NS}:roost_ceiling", ["#minecraft:base_stone_overworld", "minecraft:dripstone_block", "minecraft:calcite",
                                               "minecraft:cobbled_deepslate"])
    write_tag("worldgen/biome", f"{NS}:dwarfs_trees", [rl("tundra"), rl("lichen_woodland")])
    write_tag("worldgen/biome", f"{NS}:has_bat_colonies", ["#minecraft:is_overworld"])
    write_tag("worldgen/biome", f"{NS}:muskox_habitat", [rl("tundra"), rl("lichen_woodland"), "minecraft:snowy_plains", "minecraft:ice_spikes"])
    write_tag("worldgen/biome", "minecraft:is_overworld", [rl("tundra"), rl("lichen_woodland")])
    write_tag("worldgen/biome", "minecraft:spawns_snow_foxes", [rl("tundra"), rl("lichen_woodland")])
    write_tag("worldgen/biome", "minecraft:spawns_white_rabbits", [rl("tundra"), rl("lichen_woodland")])
    write_tag("worldgen/biome", "minecraft:has_structure/igloo", [rl("tundra"), rl("lichen_woodland")])
    write_tag("worldgen/biome", "minecraft:has_structure/mineshaft", [rl("tundra"), rl("lichen_woodland")])
    write_tag("worldgen/biome", "minecraft:has_structure/ruined_portal_standard", [rl("tundra"), rl("lichen_woodland")])
    write_tag("worldgen/biome", "minecraft:stronghold_biased_to", [rl("tundra"), rl("lichen_woodland")])
    write_tag("worldgen/biome", "minecraft:has_structure/trail_ruins", [rl("lichen_woodland")])
    write_tag("worldgen/biome", "minecraft:is_taiga", [rl("lichen_woodland")])
    write_tag("worldgen/biome", "minecraft:spawns_cold_variant_farm_animals", [rl("tundra"), rl("lichen_woodland")])
    write_tag("worldgen/biome", "c:is_cold/overworld", [rl("tundra"), rl("lichen_woodland")])
    write_tag("worldgen/biome", "c:is_snowy", [rl("lichen_woodland")])
    write_tag("worldgen/biome", "c:is_plains", [rl("tundra")])
    write_tag("worldgen/biome", "c:is_coniferous_tree", [rl("lichen_woodland")])


def version_tags(v, write_tag):
    if modern(v):
        write_tag("block", "minecraft:prevents_nearby_leaf_decay", [rl(w) for w in LOGS + CROWNS])
    # 26.x split soil out of #dirt: grass, podzol and moss have their own tags, and plants check #supports_vegetation.
    if modern(v):
        # 26.x moved a bunch of block properties into tags.
        write_tag("block", "minecraft:blocks_motion_no_leaves", [rl(b) for b in ["guano_block", "aged_guano_block", "bat_roost",
                  "mushroom_farm", "smooth_basalt_iron_ore", "white_ice", "white_packed_ice"] + MOSSES])
        write_tag("block", "minecraft:washed_away_by_fluids", [rl(b) for b in SHRUBS + CARPETS + ["sprouted_glow_lichen", "guano_layer",
                  "bolting_wheat", "bolting_carrots", "bolting_potatoes", "bolting_beetroots"]])
        write_tag("block", "minecraft:shears_minor_breaking_speed", [rl("sprouted_glow_lichen")])
        write_tag("block", "minecraft:cannot_support_snow_layer", [rl("white_ice"), rl("white_packed_ice")])
        write_tag("block", "minecraft:moss_blocks", [rl(m) for m in MOSSES])
        soil = ["#minecraft:substrate_overworld"]
    else:
        write_tag("block", "minecraft:dirt", [rl(m) for m in MOSSES])
        soil = ["#minecraft:dirt"]
    write_tag("block", f"{NS}:shrub_soil", soil + [f"#{NS}:moss", "minecraft:farmland", "minecraft:gravel", "minecraft:deepslate",
                                                   "minecraft:cobbled_deepslate", "minecraft:tuff", "minecraft:smooth_basalt"])
    write_tag("block", f"{NS}:dwarf_spruce_soil", soil + ["minecraft:snow_block"])
    write_tag("block", f"{NS}:greenstone_replaceable", soil + ["#minecraft:base_stone_overworld", "minecraft:gravel",
                                                               "minecraft:cobbled_deepslate"])


# --------------------------------------------------------------------------- worldgen

FEATURES = ["dwarf_spruce", "greenstone_belt", "tundra_flora", "surface_lichen", "white_ice", "bat_colony", "moss_patch", "moss_succession", "snow_patch", "cave_mouth_dripstone"]


def placed(feature, placement):
    return {"feature": rl(feature), "placement": placement}


def heightmap(kind="WORLD_SURFACE_WG"):
    return {"type": "minecraft:heightmap", "heightmap": kind}


BIOME = {"type": "minecraft:biome"}
MOSS_PATCHES = 3
# Surface noise bands where rock shows (noise / 8.25). Aiming for ~1/4 rock, 1/4 moss, 1/2 grass.
STONE_BANDS = [(-1.0, 0.5)]
SQUARE = {"type": "minecraft:in_square"}

PLACED = {
    "greenstone_belt": placed("greenstone_belt", [{"type": "minecraft:rarity_filter", "chance": 4}, SQUARE, heightmap("OCEAN_FLOOR_WG"), BIOME]),
    "ore_iron_tundra": placed("minecraft:ore_iron", [{"type": "minecraft:count", "count": 14}, SQUARE,
                                                    {"type": "minecraft:height_range", "height": {"type": "minecraft:trapezoid",
                                                     "min_inclusive": {"absolute": -24}, "max_inclusive": {"absolute": 72}}}, BIOME]),
    "dwarf_spruce_trees": placed("dwarf_spruce", [{"type": "minecraft:count", "count": 7}, SQUARE,
                                                  {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
                                                  heightmap("OCEAN_FLOOR"), BIOME]),
    "moss_patch": placed("moss_patch", [{"type": "minecraft:count", "count": MOSS_PATCHES}, SQUARE, heightmap(), BIOME]),
    "moss_succession": placed("moss_succession", []),
    "snow_patches_tundra": placed("snow_patch", [{"type": "minecraft:count", "count": 2}, SQUARE, heightmap(), BIOME]),
    "snow_patches_woodland": placed("snow_patch", [{"type": "minecraft:rarity_filter", "chance": 2}, SQUARE, heightmap(), BIOME]),
    "surface_lichen_woodland": placed("surface_lichen", [{"type": "minecraft:count", "count": 2}, SQUARE, heightmap(), BIOME]),
    "tundra_flora": placed("tundra_flora", [{"type": "minecraft:count", "count": 3}, SQUARE, heightmap(), BIOME]),
    "surface_lichen": placed("surface_lichen", [{"type": "minecraft:count", "count": 1}, SQUARE, heightmap(), BIOME]),
    "white_ice": placed("white_ice", []),
    # dripstone at cave mouths, once per chunk
    "cave_mouth_dripstone": placed("cave_mouth_dripstone", [heightmap("WORLD_SURFACE_WG"), BIOME]),
    "bat_colony": placed("bat_colony", [{"type": "minecraft:rarity_filter", "chance": 96}, SQUARE, heightmap("OCEAN_FLOOR_WG"), BIOME]),
}


def color(v, hex_str):
    return hex_str if modern(v) else int(hex_str[1:], 16)


def strip_features(steps, remove, append_after=None, replace=None, append=None):
    steps = [list(s) for s in steps]
    replace = replace or {}
    for i, step in enumerate(steps):
        new = []
        for f in step:
            if f in replace:
                if replace[f]:
                    new.append(replace[f])
            elif f not in remove:
                new.append(f)
            if append_after and f in append_after:
                new.extend(append_after[f])
        steps[i] = new
    for i, extra in (append or {}).items():
        steps[i] = steps[i] + extra
    return steps


def spawner(v, entity, weight, lo, hi):
    if modern(v):
        count = lo if lo == hi else {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}
        return {"type": entity, "count": count, "weight": weight}
    return {"type": entity, "weight": weight, "minCount": lo, "maxCount": hi}


def set_spawns(v, biome, category, entries, keep=None):
    if modern(v):
        attr = biome["attributes"]["minecraft:gameplay/natural_mob_spawns"]
        sp = attr["argument"]["spawns_by_category"] if "argument" in attr else attr["spawns_by_category"]
    else:
        sp = biome["spawners"]
    old = sp.get(category, [])
    sp[category] = [e for e in old if keep and e["type"] in keep] + entries


def set_visual(v, biome, sky, grass, foliage, particle=None):
    if modern(v):
        biome["attributes"]["minecraft:visual/sky_color"] = sky
        biome["effects"]["grass_color"] = grass
        biome["effects"]["foliage_color"] = foliage
        if particle:
            biome["attributes"]["minecraft:visual/ambient_particles"] = [{"particle": {"type": particle[0]}, "probability": particle[1]}]
    else:
        biome["effects"]["sky_color"] = color(v, sky)
        biome["effects"]["grass_color"] = color(v, grass)
        biome["effects"]["foliage_color"] = color(v, foliage)
        if particle:
            biome["effects"]["particle"] = {"options": {"type": particle[0]}, "probability": particle[1]}


def biomes(v):
    tundra = vanilla_biome(v, "snowy_plains")
    tundra["has_precipitation"] = False
    tundra["downfall"] = 0.0
    tundra["features"] = strip_features(
        tundra["features"],
        remove={"minecraft:trees_snowy", "minecraft:flower_default", "minecraft:patch_pumpkin", "minecraft:patch_sugar_cane",
                "minecraft:patch_firefly_bush_near_water", "minecraft:patch_firefly_bush"},
        append_after={"minecraft:freeze_top_layer": [rl("white_ice"), rl("snow_patches_tundra")]},
        append={2: [rl("greenstone_belt")], 6: [rl("ore_iron_tundra")], 9: [rl("moss_patch"), rl("moss_succession"), rl("surface_lichen"), rl("tundra_flora")]})
    # no ambient particle, flurries come from client/Flurries.java
    set_visual(v, tundra, "#c3d5ee", "#a2a77c", "#8c9a6a")
    set_spawns(v, tundra, "creature", [spawner(v, rl("muskox"), 8, 4, 8), spawner(v, "minecraft:rabbit", 4, 2, 3),
                                       spawner(v, "minecraft:polar_bear", 1, 1, 2)])

    woodland = vanilla_biome(v, "snowy_taiga")
    # Just warm enough that snow doesn't settle.
    woodland["temperature"] = 0.2
    woodland["features"] = strip_features(
        woodland["features"],
        remove={"minecraft:patch_pumpkin", "minecraft:patch_sugar_cane", "minecraft:patch_firefly_bush_near_water"},
        replace={"minecraft:trees_taiga": rl("dwarf_spruce_trees")},
        append_after={"minecraft:freeze_top_layer": [rl("snow_patches_woodland")]},
        append={9: [rl("surface_lichen_woodland"), rl("tundra_flora")]})
    set_visual(v, woodland, "#a3bdf4", "#8a8c55", "#6f8a55")
    set_spawns(v, woodland, "creature", [spawner(v, rl("muskox"), 4, 4, 8)],
               keep={"minecraft:wolf", "minecraft:rabbit", "minecraft:fox", "minecraft:sheep"})
    return {"tundra": tundra, "lichen_woodland": woodland}


# Surface rules, written once and rendered into each version's format.
def state(v, name):
    return name if modern(v) else {"Name": name}


def cond(c, then):
    return {"type": "minecraft:condition", "if_true": c, "then_run": then}


def seq(*rules):
    return {"type": "minecraft:sequence", "sequence": list(rules)}


def block(v, name):
    return {"type": "minecraft:block", "result_state": state(v, name)}


def biome_is(v, name):
    return {"type": "minecraft:biome", "biome_is": rl(name) if modern(v) else [rl(name)]}


def noise(name, lo, hi):
    return {"type": "minecraft:noise_threshold", "noise": name, "min_threshold": lo, "max_threshold": hi}


ON_FLOOR = {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "floor"}
ABOVE_WATER = {"type": "minecraft:water", "offset": -1, "surface_depth_multiplier": 0, "add_stone_depth": False}
ABOVE_BEDROCK = {"type": "minecraft:y_above", "anchor": {"above_bottom": 5}, "surface_depth_multiplier": 0, "add_stone_depth": False}
BIG = 1.0e9


def surface_rules(v):
    rock = seq(cond(noise("minecraft:gravel", 0.6, BIG), block(v, "minecraft:gravel")), block(v, "minecraft:deepslate"))
    bands = [cond(noise("minecraft:surface", lo / 8.25, hi / 8.25), rock) for lo, hi in STONE_BANDS]
    tundra = cond(biome_is(v, "tundra"), cond(ABOVE_BEDROCK, seq(
        # No soil layer, grass sits right on the rock. Moss gets added later by features.
        cond(ON_FLOOR, cond(ABOVE_WATER, seq(*bands, block(v, "minecraft:grass_block")))),
        # Deepslate all the way from bedrock to the surface.
        block(v, "minecraft:deepslate"))))
    woodland = cond(biome_is(v, "lichen_woodland"), cond(ON_FLOOR, cond(ABOVE_WATER, seq(
        # rock showing through
        cond(noise("minecraft:surface", -0.5 / 8.25, 0.4 / 8.25), block(v, "minecraft:deepslate")),
        cond(noise("minecraft:surface", 0.2, BIG), block(v, "minecraft:podzol")),
        cond(noise("minecraft:surface", -BIG, -0.45), block(v, "minecraft:coarse_dirt"))))))
    return seq(tundra, woodland)


def worldgen(v, out):
    wg = out / f"data/{NS}/worldgen"
    for f in FEATURES:
        if modern(v):
            write(wg / f"feature/{f}.json", {"type": rl(f)})
        else:
            write(wg / f"configured_feature/{f}.json", {"type": rl(f), "config": {}})
    for name, data in PLACED.items():
        write(wg / f"placed_feature/{name}.json", data)
    for name, data in biomes(v).items():
        write(wg / f"biome/{name}.json", data)
    if modern(v):
        write(wg / "material_rule/overworld.json", surface_rules(v))
    else:
        write(out / f"{NS}/surface_rules.json", surface_rules(v))
    mods = out / f"data/{NS}/neoforge/biome_modifier"
    write(mods / "bat_colonies.json", {"type": "neoforge:add_features", "biomes": f"#{NS}:has_bat_colonies",
                                       "features": [rl("cave_mouth_dripstone"), rl("bat_colony")], "step": "underground_decoration"})
    write(mods / "muskox_in_snowy_plains.json", {"type": "neoforge:add_spawns", "biomes": ["minecraft:snowy_plains", "minecraft:ice_spikes"],
                                                 "spawners": [spawner(v, rl("muskox"), 2, 4, 8)]})
