#!/usr/bin/env python3
"""Writes all the JSON resources for both versions.

Shared stuff goes in src/main/resources, version-specific stuff in src/main/resources-<version>.
Change the tables here, not the generated files."""
import json
import gen_tundra
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
NS = "ecologies"
VERSIONS = ["1.21.1", "26.3"]
SHARED = ROOT / "src/main/resources"
GENERATED_MARK = "_generated_by_gen_resources"


def per_version(v):
    return ROOT / f"src/main/resources-{v}"


def is_modern(v):
    """26.x formats."""
    return not v.startswith("1.21.1")


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def rl(name):
    return name if ":" in name else f"{NS}:{name}"


# --------------------------------------------------------------------------- content

LANG = {
    "itemGroup.ecologies": "Ecologies",
    "block.ecologies.guano_layer": "Guano",
    "block.ecologies.guano_block": "Guano Block",
    "block.ecologies.aged_guano_block": "Aged Guano Block",
    "block.ecologies.bat_roost": "Bat Roost",
    "block.ecologies.mushroom_farm": "Mushroom Farm",
    "block.ecologies.sprouted_glow_lichen": "Sprouted Glow Lichen",
    "item.ecologies.guano": "Guano Pile",
    "item.ecologies.phosphorus": "Phosphorus",
    "entity.ecologies.muskox": "Muskox",
    "item.ecologies.muskox_spawn_egg": "Muskox Spawn Egg",
    "entity.ecologies.colony_bat": "Bat",
    "block.ecologies.bolting_wheat": "Bolting Wheat",
    "block.ecologies.bolting_carrots": "Bolting Carrots",
    "block.ecologies.bolting_potatoes": "Bolting Potatoes",
    "block.ecologies.bolting_beetroots": "Bolting Beetroots",
    "item.ecologies.rusted_wheat": "Rusted Wheat",
    "item.ecologies.poisonous_carrot": "Poisonous Carrot",
    "item.ecologies.moldy_beetroot": "Moldy Beetroot",
}

# Blocks: model kind, textures, loot, mineable tool.
BLOCKS = {
    "guano_block": {"model": "cube_all", "tex": {"all": "block/guano"}, "loot": "self", "tool": "shovel"},
    "aged_guano_block": {"model": "cube_all", "tex": {"all": "block/guano_aged"}, "loot": "self", "tool": "shovel"},
    "bat_roost": {
        "model": "cube_bottom_top",
        "tex": {"top": "block/bat_roost_top", "side": "block/bat_roost_side", "bottom": "minecraft:block/dripstone_block"},
        "loot": "none",  # worldgen only; breaking it drops nothing, like budding amethyst
        "tool": "pickaxe",
    },
    "guano_layer": {"model": "layers", "tex": "block/guano", "full": "guano_block", "loot": "layers:guano", "tool": "shovel"},
    "mushroom_farm": {"model": "mushroom_farm", "loot": "self", "tool": "axe"},
    # Klei's glow lichen models, from art/pack.
    "sprouted_glow_lichen": {"model": "pack_lichen", "loot": "multiface", "tool": None, "item_texture": "block/sprouted_glow_lichen"},
}

PACK = ROOT / "art/pack/assets/minecraft"
TEMPLATES = ROOT / "tools/templates"

# Entity loot: (template, {vanilla item: our item})
ENTITY_LOOT = {"muskox": ("cow_loot", {"minecraft:beef": "minecraft:mutton"})}

# Spawn eggs: 1.21.1 tints vanilla's template; 26.x needs a texture per egg (ours, or a vanilla egg's model).
SPAWN_EGGS = {"muskox_spawn_egg": None}

# Flat (generated) item models.
ITEMS = {"guano": "item/guano", "phosphorus": "item/phosphorus", "rusted_wheat": "item/rusted_wheat",
         "poisonous_carrot": "item/poisonous_carrot", "moldy_beetroot": "item/moldy_beetroot"}

# Bolting crops: vanilla crop -> (ripe model, ripe drops x2, bolted drops). Drops: (item, min, max).
BOLTING = {
    "bolting_wheat": ("minecraft:block/wheat_stage7",
                      [("minecraft:wheat", 2, 2), ("minecraft:wheat_seeds", 2, 4)],
                      [("minecraft:wheat", 1, 1), ("ecologies:rusted_wheat", 1, 2), ("minecraft:wheat_seeds", 1, 3)]),
    "bolting_carrots": ("minecraft:block/carrots_stage3",
                        [("minecraft:carrot", 4, 8)],
                        [("minecraft:carrot", 1, 1), ("ecologies:poisonous_carrot", 1, 2)]),
    "bolting_potatoes": ("minecraft:block/potatoes_stage3",
                         [("minecraft:potato", 4, 8)],
                         [("minecraft:potato", 1, 1), ("minecraft:poisonous_potato", 1, 2)]),
    "bolting_beetroots": ("minecraft:block/beetroots_stage3",
                          [("minecraft:beetroot", 2, 2), ("minecraft:beetroot_seeds", 2, 4)],
                          [("minecraft:beetroot", 1, 1), ("ecologies:moldy_beetroot", 1, 2), ("minecraft:beetroot_seeds", 1, 2)]),
}


def bolting_loot(v, block, ripe, bolted):
    """Ripe is age 0, bolted is 1+."""
    pools = []
    for drops, ages in ((ripe, ["0"]), (bolted, [str(a) for a in range(1, 8)])):
        for item, lo, hi in drops:
            count = lo if lo == hi else {"type": "minecraft:uniform", "min": lo, "max": hi}
            if is_modern(v):
                cond = {"type": "minecraft:any_of", "terms": [{"type": "minecraft:match_block", "blocks": rl(block), "state": {"age": a}} for a in ages]}
                pools.append({"rolls": 1, "condition": cond,
                              "entries": [{"type": "minecraft:item", "name": item, "modifier": {"type": "minecraft:set_count", "count": count}}]})
            else:
                cond = {"condition": "minecraft:any_of", "terms": [{"condition": "minecraft:block_state_property", "block": rl(block), "properties": {"age": a}} for a in ages]}
                if isinstance(count, dict):
                    count = {"type": "minecraft:uniform", "min": float(lo), "max": float(hi)}
                else:
                    count = float(count)
                pools.append({"rolls": 1.0, "bonus_rolls": 0.0, "conditions": [cond],
                              "entries": [{"type": "minecraft:item", "name": item,
                                           "functions": [{"function": "minecraft:set_count", "count": count, "add": False}]}]})
    return {"type": "minecraft:block", "pools": pools, "random_sequence": f"{NS}:blocks/{block}"}


def recipes(v):
    """Tiny recipe DSL, written out per version."""
    return {
        "guano_block": ("shaped", ["##", "##"], {"#": "guano"}, "guano_block", 1),
        "guano_layer": ("shaped", ["###"], {"#": "guano_block"}, "guano_layer", 6),
        "mushroom_farm": ("shaped", ["S S", "SGS", "SSS"], {"S": "#minecraft:wooden_slabs", "G": "guano_block"}, "mushroom_farm", 1),
        "brown_dye_from_guano": ("shapeless", ["guano"], "minecraft:brown_dye", 1),
        "phosphorus_from_glow_ink_sac": ("shapeless", ["minecraft:glow_ink_sac"], "phosphorus", 1),
        "glow_item_frame_from_phosphorus": ("shapeless", ["minecraft:item_frame", "phosphorus"], "minecraft:glow_item_frame", 1),
        "gunpowder_from_smelting_guano": ("smelting", "guano", "minecraft:gunpowder", 0.1),
        "phosphorus_from_smelting_glow_lichen": ("smelting", "minecraft:glow_lichen", "phosphorus", 0.1),
    }


# --------------------------------------------------------------------------- renderers

def ingredient(v, ref):
    tag = ref.startswith("#")
    name = rl(ref[1:] if tag else ref)
    if is_modern(v):
        return ("#" if tag else "") + name
    return {"tag": name} if tag else {"item": name}


def result(v, item, count):
    r = {"id": rl(item)}
    if count != 1 or not is_modern(v):
        r["count"] = count
    return r


def render_recipe(v, spec):
    kind = spec[0]
    if kind == "shaped":
        _, pattern, key, out, count = spec
        return {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
                "key": {k: ingredient(v, x) for k, x in key.items()}, "result": result(v, out, count)}
    if kind == "shapeless":
        _, ins, out, count = spec
        return {"type": "minecraft:crafting_shapeless", "category": "misc",
                "ingredients": [ingredient(v, x) for x in ins], "result": result(v, out, count)}
    if kind == "smelting":
        _, ins, out, xp = spec
        return {"type": "minecraft:smelting", "category": "misc", "cookingtime": 200, "experience": xp,
                "ingredient": ingredient(v, ins), "result": {"id": rl(out)}}
    raise ValueError(kind)


def cond_survives_explosion(v):
    return {"type": "minecraft:survives_explosion"} if is_modern(v) else {"condition": "minecraft:survives_explosion"}


def render_loot(v, block, loot):
    seq = {"random_sequence": f"{NS}:blocks/{block}"}
    if loot == "none":
        return {"type": "minecraft:block", "pools": [], **seq}
    if loot == "self":
        pool = {"rolls": 1, "entries": [{"type": "minecraft:item", "name": rl(block)}]}
        if is_modern(v):
            pool["condition"] = cond_survives_explosion(v)
        else:
            pool["bonus_rolls"] = 0.0
            pool["conditions"] = [cond_survives_explosion(v)]
        return {"type": "minecraft:block", "pools": [pool], **seq}
    if loot.startswith("layers:"):
        drop = rl(loot.split(":", 1)[1])
        children = []
        for n in range(1, 9):
            if is_modern(v):
                children.append({"type": "minecraft:item", "name": drop,
                                 "condition": {"type": "minecraft:match_block", "blocks": rl(block), "state": {"layers": str(n)}},
                                 "modifier": {"type": "minecraft:set_count", "count": n}})
            else:
                children.append({"type": "minecraft:item", "name": drop,
                                 "conditions": [{"condition": "minecraft:block_state_property", "block": rl(block),
                                                 "properties": {"layers": str(n)}}],
                                 "functions": [{"function": "minecraft:set_count", "count": float(n), "add": False}]})
        pool = {"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": children}]}
        if not is_modern(v):
            pool["bonus_rolls"] = 0.0
        return {"type": "minecraft:block", "pools": [pool], **seq}
    if loot == "multiface":
        # Drops one per face, only with shears (like vanilla glow lichen).
        faces = ["down", "up", "north", "south", "west", "east"]
        if is_modern(v):
            mods = [{"type": "minecraft:set_count", "add": True, "count": 1,
                     "condition": {"type": "minecraft:match_block", "blocks": rl(block), "state": {f: "true"}}} for f in faces]
            mods += [{"type": "minecraft:set_count", "add": True, "count": -1}, {"type": "minecraft:explosion_decay"}]
            entry = {"type": "minecraft:item", "name": rl(block), "condition": "minecraft:tool/can_shear", "modifier": mods}
            return {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [entry]}], **seq}
        funcs = [{"function": "minecraft:set_count", "add": True, "count": 1.0,
                  "conditions": [{"condition": "minecraft:block_state_property", "block": rl(block), "properties": {f: "true"}}]} for f in faces]
        funcs += [{"function": "minecraft:set_count", "add": True, "count": -1.0}, {"function": "minecraft:explosion_decay"}]
        entry = {"type": "minecraft:item", "name": rl(block), "functions": funcs,
                 "conditions": [{"condition": "minecraft:match_tool", "predicate": {"items": "minecraft:shears"}}]}
        return {"type": "minecraft:block", "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [entry]}], **seq}
    raise ValueError(loot)


def tex(t):
    return t if ":" in t else f"{NS}:{t}"


def block_assets(name, spec):
    """Returns (blockstate, {model_name: model}, item_model_parent)."""
    kind = spec["model"]
    if kind == "cube_all":
        return ({"variants": {"": {"model": f"{NS}:block/{name}"}}},
                {name: {"parent": "minecraft:block/cube_all", "textures": {"all": tex(spec["tex"]["all"])}}},
                f"{NS}:block/{name}")
    if kind == "cube_bottom_top":
        t = {k: tex(x) for k, x in spec["tex"].items()}
        return ({"variants": {"": {"model": f"{NS}:block/{name}"}}},
                {name: {"parent": "minecraft:block/cube_bottom_top", "textures": t}},
                f"{NS}:block/{name}")
    if kind == "layers":
        # Reuse vanilla's snow layer shapes, swapping the texture.
        t = tex(spec["tex"])
        models, variants = {}, {}
        for n in range(1, 8):
            m = f"{name}_height{n * 2}"
            models[m] = {"parent": f"minecraft:block/snow_height{n * 2}", "textures": {"texture": t, "particle": t}}
            variants[f"layers={n}"] = {"model": f"{NS}:block/{m}"}
        variants["layers=8"] = {"model": f"{NS}:block/{spec['full']}"}
        return {"variants": variants}, models, f"{NS}:block/{name}_height2"
    if kind == "mushroom_farm":
        # Composter style bin, 2px walls. Fills 1-7 with soil, spotted once it spreads.
        # The block entity renderer draws the species on top when it's done.
        def f(uv, t, cull=None):
            d = {"uv": uv, "texture": t}
            if cull:
                d["cullface"] = cull
            return d
        wall = lambda frm, to, faces: {"from": frm, "to": to, "faces": faces}
        bin_model = {"parent": "minecraft:block/block",
                     "textures": {"particle": tex("block/mushroom_farm_side"), "side": tex("block/mushroom_farm_side"),
                                  "top": tex("block/mushroom_farm_top"), "inner": "minecraft:block/composter_side",
                                  "inside": "minecraft:block/composter_bottom", "bottom": "minecraft:block/composter_bottom"},
                     "elements": [
                         wall([0, 0, 0], [16, 2, 16], {"up": f([0, 0, 16, 16], "#inside"), "down": f([0, 0, 16, 16], "#bottom", "down")}),
                         wall([0, 0, 0], [2, 16, 16], {"up": f([0, 0, 2, 16], "#top", "up"), "north": f([14, 0, 16, 16], "#side", "north"),
                                                      "south": f([0, 0, 2, 16], "#side", "south"), "west": f([0, 0, 16, 16], "#side", "west"),
                                                      "east": f([0, 0, 16, 16], "#inner")}),
                         wall([14, 0, 0], [16, 16, 16], {"up": f([14, 0, 16, 16], "#top", "up"), "north": f([0, 0, 2, 16], "#side", "north"),
                                                        "south": f([14, 0, 16, 16], "#side", "south"), "west": f([0, 0, 16, 16], "#inner"),
                                                        "east": f([0, 0, 16, 16], "#side", "east")}),
                         wall([2, 0, 0], [14, 16, 2], {"up": f([2, 0, 14, 2], "#top", "up"), "north": f([2, 0, 14, 16], "#side", "north"),
                                                      "south": f([2, 0, 14, 16], "#inner")}),
                         wall([2, 0, 14], [14, 16, 16], {"up": f([2, 14, 14, 16], "#top", "up"), "north": f([2, 0, 14, 16], "#inner"),
                                                        "south": f([2, 0, 14, 16], "#side", "south")})]}
        models = {name: bin_model}
        parts = [{"apply": {"model": f"{NS}:block/{name}"}}]
        for n in range(1, 8):
            for spotted in (False, True):
                m = f"{name}_soil{n}" + ("_spotted" if spotted else "")
                soil = tex("block/mushroom_farm_soil_spotted" if spotted else "block/mushroom_farm_soil")
                models[m] = {"textures": {"particle": soil, "soil": soil},
                             "elements": [wall([2, 0, 2], [14, 1 + 2 * n, 14], {"up": f([2, 2, 14, 14], "#soil")})]}
            # bare soil while it's starting, spotted once it spreads
            parts.append({"when": {"fill": str(n)}, "apply": {"model": f"{NS}:block/{name}_soil{n}" + ("_spotted" if n >= 3 else "")}})
        return {"multipart": parts}, models, f"{NS}:block/{name}"
    if kind == "pack_lichen":
        def remap(text):
            # vanilla glow lichen names in the pack -> this block's names
            for prefix in ("minecraft:block/glow_lichen", "block/glow_lichen"):
                text = text.replace(f'"{prefix}', f'"{NS}:block/{name}')
            return text
        state = json.loads(remap((PACK / "blockstates/glow_lichen.json").read_text()))
        models = {}
        for f in sorted((PACK / "models/block").glob("glow_lichen*.json")):
            model = json.loads(remap(f.read_text()))
            model.pop("credit", None)
            model["render_type"] = "minecraft:cutout"
            models[f.stem.replace("glow_lichen", name, 1)] = model
        return state, models, None
    raise ValueError(kind)


def copy_pack_textures(prefix_from, prefix_to):
    """Copies art/pack textures and .mcmeta files over under our names."""
    out = SHARED / f"assets/{NS}/textures/block"
    out.mkdir(parents=True, exist_ok=True)
    for f in sorted((PACK / "textures/block").glob(prefix_from + "*")):
        shutil.copyfile(f, out / f.name.replace(prefix_from, prefix_to, 1))


# --------------------------------------------------------------------------- main

def clean():
    for base in [SHARED / f"assets/{NS}/blockstates", SHARED / f"assets/{NS}/models", SHARED / f"assets/{NS}/lang",
                 SHARED / f"data/{NS}/tags", SHARED / "data/minecraft/tags"]:
        if base.exists():
            shutil.rmtree(base)
    for v in VERSIONS:
        if per_version(v).exists():
            shutil.rmtree(per_version(v))


def main():
    clean()
    assets = SHARED / f"assets/{NS}"
    item_models = {}

    for name, spec in BLOCKS.items():
        state, models, item_parent = block_assets(name, spec)
        write(assets / f"blockstates/{name}.json", state)
        for m, data in models.items():
            write(assets / f"models/block/{m}.json", data)
        if item_parent is None:  # flat item, like vanilla glow lichen
            write(assets / f"models/item/{name}.json",
                  {"parent": "minecraft:item/generated", "textures": {"layer0": tex(spec["item_texture"])}})
        else:
            write(assets / f"models/item/{name}.json", {"parent": item_parent})
        item_models[name] = f"{NS}:item/{name}"

    for name, t in ITEMS.items():
        write(assets / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": tex(t)}})
        item_models[name] = f"{NS}:item/{name}"

    for name, (ripe_model, _, _) in BOLTING.items():
        variants = {"age=0": {"model": ripe_model}}
        for a in range(1, 8):
            variants[f"age={a}"] = {"model": f"{NS}:block/{name}"}
        write(assets / f"blockstates/{name}.json", {"variants": variants})
        write(assets / f"models/block/{name}.json", {"parent": "minecraft:block/crop", "render_type": "minecraft:cutout",
                                                      "textures": {"crop": f"{NS}:block/{name}"}})

    for name, (state, models, item) in gen_tundra.block_assets().items():
        if name in gen_tundra.ONLY_121:
            continue
        write(assets / f"blockstates/{name}.json", state)
        for m, data in models.items():
            write(assets / f"models/block/{m}.json", data)
        write(assets / f"models/item/{name}.json", item)
        item_models[name] = f"{NS}:item/{name}"
    for m, data in gen_tundra.templates().items():
        write(assets / f"models/block/{m}.json", data)

    write(assets / "lang/en_us.json", {**LANG, **gen_tundra.LANG})
    copy_pack_textures("glow_lichen", "sprouted_glow_lichen")

    # Tags are the same format on both versions.
    tools = {}
    for name, spec in BLOCKS.items():
        if spec["tool"]:
            tools.setdefault(spec["tool"], []).append(rl(name))
    tundra_tools = {"pickaxe": ["smooth_basalt_iron_ore", "white_ice", "white_packed_ice"],
                    "axe": gen_tundra.LOGS + gen_tundra.CROWNS,
                    "hoe": gen_tundra.MOSSES + gen_tundra.CARPETS + gen_tundra.SHRUBS}
    for tool, blocks in tundra_tools.items():
        tools.setdefault(tool, []).extend(rl(b) for b in blocks)
    shared_tags = {}
    gen_tundra.shared_tags(lambda kind, tag, values: shared_tags.setdefault((kind, tag), []).extend(values))
    for (kind, tag), values in shared_tags.items():
        ns, path = tag.split(":")
        write(SHARED / f"data/{ns}/tags/{kind}/{path}.json", {"replace": False, "values": values})
    for tool, blocks in tools.items():
        write(SHARED / f"data/minecraft/tags/block/mineable/{tool}.json", {"values": blocks})
    # bat forage. c: tags are optional so it still loads without them
    write(SHARED / f"data/{NS}/tags/block/bat_forage_crops.json", {"values": [
        "#minecraft:crops", "#minecraft:flowers", "minecraft:sweet_berry_bush", "minecraft:cave_vines",
        "minecraft:cave_vines_plant", "minecraft:cocoa", "minecraft:melon", "minecraft:pumpkin",
        "minecraft:flowering_azalea", "minecraft:flowering_azalea_leaves",
        {"id": "#c:crops", "required": False}, {"id": "#c:flowers", "required": False},
        {"id": "#c:fruits", "required": False}, {"id": "#c:fruit_leaves", "required": False},
        {"id": "#c:berry_bushes", "required": False}] + [rl(b) for b in BOLTING]})
    write(SHARED / f"data/{NS}/tags/block/bat_planted_crops.json",
          {"values": ["minecraft:wheat", "minecraft:carrots", "minecraft:potatoes", "minecraft:beetroots"]})
    write(SHARED / f"data/{NS}/tags/block/guano.json",
          {"values": [rl("guano_layer"), rl("guano_block"), rl("aged_guano_block")]})

    for v in VERSIONS:
        out = per_version(v)
        (out / GENERATED_MARK).parent.mkdir(parents=True, exist_ok=True)
        (out / GENERATED_MARK).write_text("Everything in here gets made by tools/gen_resources.py, so change that instead.\n")
        for name, spec in recipes(v).items():
            write(out / f"data/{NS}/recipe/{name}.json", render_recipe(v, spec))
        for name, spec in BLOCKS.items():
            write(out / f"data/{NS}/loot_table/blocks/{name}.json", render_loot(v, name, spec["loot"]))
        tundra_blocks = gen_tundra.SHARED_BLOCKS + (gen_tundra.ONLY_121 if not is_modern(v) else [])
        for name in tundra_blocks:
            write(out / f"data/{NS}/loot_table/blocks/{name}.json", gen_tundra.loot(v, name))
        for name, spec in gen_tundra.recipes().items():
            write(out / f"data/{NS}/recipe/{name}.json", render_recipe(v, spec))
        version_tags = {}
        gen_tundra.version_tags(v, lambda kind, tag, values: version_tags.setdefault((kind, tag), []).extend(values))
        for (kind, tag), values in version_tags.items():
            ns, path = tag.split(":")
            write(out / f"data/{ns}/tags/{kind}/{path}.json", {"replace": False, "values": values})
        gen_tundra.worldgen(v, out)
        if not is_modern(v):
            # 26.x has a vanilla red shrub; only 1.21.1 has ours.
            for name in gen_tundra.ONLY_121:
                state, models, item = gen_tundra.block_assets()[name]
                write(out / f"assets/{NS}/blockstates/{name}.json", state)
                for m, data in models.items():
                    write(out / f"assets/{NS}/models/block/{m}.json", data)
                write(out / f"assets/{NS}/models/item/{name}.json", item)
        for name, (_, ripe, bolted) in BOLTING.items():
            write(out / f"data/{NS}/loot_table/blocks/{name}.json", bolting_loot(v, name, ripe, bolted))
        # bees visit bolted crops: 26.x bees use #bee_attractive, 1.21.1 bees use #flowers
        bee_tag = "bee_attractive" if is_modern(v) else "flowers"
        write(out / f"data/minecraft/tags/block/{bee_tag}.json", {"replace": False, "values": [rl(b) for b in BOLTING]})
        for name, (template, swaps) in ENTITY_LOOT.items():
            text = (TEMPLATES / f"{template}_{v}.json").read_text()
            for a, b in swaps.items():
                text = text.replace(f'"{a}"', f'"{b}"')
            data = json.loads(text)
            data["random_sequence"] = f"{NS}:entities/{name}"
            write(out / f"data/{NS}/loot_table/entities/{name}.json", data)
        for egg, vanilla_model in SPAWN_EGGS.items():
            if is_modern(v) and vanilla_model:
                item_models[egg] = vanilla_model
                continue
            model = ({"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{egg}"}} if is_modern(v)
                     else {"parent": "minecraft:item/template_spawn_egg"})
            write(out / f"assets/{NS}/models/item/{egg}.json", model)
            item_models[egg] = f"{NS}:item/{egg}"
        if is_modern(v):
            # 1.21.4+ needs an item model definition per item.
            for name, model in item_models.items():
                write(out / f"assets/{NS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": model}})

    split_free_rotations(assets)
    print("generated resources for", ", ".join(VERSIONS))


def split_free_rotations(assets):
    """1.21.1 only allows one axis in 22.5 degree steps, so 26.x-only rotations get a rounded 1.21.1 copy."""
    for f in sorted((assets / "models/block").glob("*.json")):
        data = json.loads(f.read_text())
        elements = data.get("elements", [])
        if not any(e.get("rotation") and "axis" not in e["rotation"] for e in elements):
            continue
        legacy = json.loads(json.dumps(data))
        for e in legacy["elements"]:
            r = e.get("rotation")
            if not r or "axis" in r:
                continue
            axis = max("xyz", key=lambda a: abs(r.get(a, 0)))
            angle = max(-45.0, min(45.0, round(r.get(axis, 0) / 22.5) * 22.5))
            e["rotation"] = {"angle": angle, "axis": axis, "origin": r.get("origin", [8, 8, 8])}
        rel = f.relative_to(SHARED)
        write(per_version("1.21.1") / rel, legacy)
        write(per_version("26.3") / rel, data)
        f.unlink()


if __name__ == "__main__":
    main()
