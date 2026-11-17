# Ecologies datapack guide

Most of what Ecologies does can be changed with a datapack: who hunts what, which animals fight back, what
herds eat, how bat colonies behave, what lichen breaks down, and what the mushroom farm grows.

This isn't a general datapack tutorial. If you've never made one, start with the
[Minecraft Wiki's datapack page](https://minecraft.wiki/w/Data_pack).

## The basics

Every file goes in its own folder under `ecologies`:

```
data/<namespace>/ecologies/<folder>/<name>.json
```

`<namespace>` is your pack's namespace and `<name>` can be anything. The folders are:

| Folder | What it does |
|---|---|
| [`predation`](#predation) | Who hunts what, and how they hunt. |
| [`defense`](#defense) | Animals that ram back when attacked. |
| [`grazing`](#grazing) | What an animal eats and what it drops after a meal. |
| [`herding`](#herding) | Animals sticking together in herds. |
| [`natural_breeding`](#natural-breeding) | Herds breeding back after losing one. |
| [`roost`](#roost) | Bat colonies and their roosts. |
| [`substrate`](#substrate) | Blocks that break down what they grow on. |
| [`mushroom_farm`](#mushroom-farm) | What the mushroom farm can grow. |

To change one of ours, put a file at the same path in your pack, like
`data/ecologies/ecologies/predation/wolf.json`. Yours replaces ours completely, so copy the whole file
and change what you want. Run `/reload` to see changes without restarting.

Fields marked *optional* can be left out. Anything with a default uses that when it's left out.

### Types

| Type | What it looks like |
|---|---|
| ID | A namespaced id, like `"minecraft:wolf"`. |
| ID list | One id or tag, or a list of them. Tags start with `#`. `"minecraft:wolf"`, `"#minecraft:logs"` and `["minecraft:sheep", "#c:crops"]` all work. |
| Block state | A block id with optional properties, written like in commands: `"minecraft:glow_lichen[down=true]"`. |
| Ticks | Game ticks. 20 ticks is 1 second and 24000 is a full day. |
| Chance | A number from `0` to `1`. `0.25` is 25%. |

---

## Predation

Predators hunt when they're hungry, eat what they catch, then take a break. Prey only run from a
predator while it's hungry.

**Folder:** `data/<namespace>/ecologies/predation/`

### Fields

| Field | Type | Default | Description |
|---|---|---|---|
| `predator` | ID list | | The mob that hunts. |
| `prey` | ID list | | The mobs it hunts. |
| `hunger_cooldown` | Ticks | `24000` | How long after a kill before it's hungry again. |
| `prefers_babies` | Boolean | `true` | Goes for young prey nearby before adults. |
| `eats_kill` | Boolean | `true` | Its kills don't drop food (it ate it). Things like leather still drop. |
| `adds_hunting` | Boolean | `false` | Gives it a hunting goal if it doesn't hunt in vanilla. |
| `prey_flee_range` | Number | `10` | How close it can get before prey run. `0` means they don't run. |
| `chase_speed` | Number | `0` | Above `0`, it chases and bites on the run at this speed instead of using its own attack. Polar bears use this since their own attack stops them dead. |
| `sprint` | Number | `1.0` | Speed multiplier while chasing. |
| `stalks` | Boolean | `false` | Sneaks up on prey. Prey don't notice it until it's within 3 blocks or stops crouching. |
| `give_up_after` | Ticks | `200` | How long it'll chase out in the open before it tires and gives up. Sneaking doesn't count. Packs last three times as long. |
| `pack_size` | Integer | `0` | Above `0`, this many of its kind hunting together become a pack. They flank and wear the prey out instead of sprinting it down. |
| `rush_range` | Number | `0` | Above `0`, a stalker stops sneaking within this many blocks and dashes in. |

### Example

```json
{
  "predator": "minecraft:fox",
  "prey": ["minecraft:chicken", "minecraft:rabbit", "minecraft:cod", "minecraft:salmon"],
  "hunger_cooldown": 12000,
  "prey_flee_range": 8,
  "stalks": true,
  "sprint": 1.25,
  "rush_range": 5
}
```

Foxes get hungry every half day. They sneak up on chickens, rabbits and fish, then dash in once they're
5 blocks away.

---

## Defense

Animals that stand their ground. They ram whatever attacks them, or goes after their young, once, then back
off. If the herd's outnumbered they run instead. A predator that gets rammed while it has young of its
own nearby gives up and goes back to them.

**Folder:** `data/<namespace>/ecologies/defense/`

### Fields

| Field | Type | Default | Description |
|---|---|---|---|
| `animal` | ID list | | The mob that fights back. |
| `damage` | Number | `4.0` | How much a ram hurts. |
| `knockback` | Number | `1.6` | How hard a ram throws the attacker back. |
| `protects_young` | Boolean | `true` | Also rams whatever goes after a young one nearby. |
| `range` | Number | `12` | How far it looks for attackers and for the rest of its herd. |

### Example

```json
{
  "animal": "ecologies:muskox",
  "damage": 4.0,
  "knockback": 1.8,
  "range": 12
}
```

Muskox ram for 4 damage with a big shove, and look out for calves within 12 blocks.

---

## Grazing

The animal walks over to food blocks, eats them, and drops an item a while after, like chickens laying
eggs. It drops more when it's living the way it would in the wild.

There are 4 welfare checks: open space around it, a few of its kind nearby (but not a crowd), open sky,
and being in its home biome. The more it meets, the more it drops.

**Folder:** `data/<namespace>/ecologies/grazing/`

### Fields

| Field | Type | Default | Description |
|---|---|---|---|
| `animal` | ID list | | The mob that grazes. |
| `food` | ID list | | Blocks it eats. Blocks with faces, like glow lichen, lose one face per bite. |
| `eat_cooldown` | Ticks | `2400` | Time between meals. |
| `search_range` | Integer | `10` | How far it looks for food. |
| `drop` | ID | *optional* | The item it drops after eating. Leave it out for no drop. |
| `drop_counts` | List of integers | `[1, 1, 2, 4, 8]` | How many it drops when it meets 0, 1, 2, 3 or 4 of the welfare checks. |
| `habitat` | ID | *optional* | A biome tag for its home biome. |
| `overcrowded_at` | Integer | `10` | With this many of its kind within 8 blocks it drops nothing. |
| `first_drop_delay` | Ticks | `24000` | A new or newborn animal won't drop anything for this long. |

### Example

```json
{
  "animal": "ecologies:muskox",
  "food": "minecraft:glow_lichen",
  "drop": "minecraft:string",
  "habitat": "ecologies:muskox_habitat"
}
```

Muskox graze glow lichen and drop string, from 1 when they're cooped up to 8 when they're happy and out on
the tundra.

---

## Herding

A light pull toward others of the same kind so they stick together instead of wandering off.

**Folder:** `data/<namespace>/ecologies/herding/`

### Fields

| Field | Type | Default | Description |
|---|---|---|---|
| `animal` | ID list | | The mob that herds. |
| `range` | Number | `16` | How far it looks for the rest of its herd. |
| `spread` | Number | `6` | How far it can wander from the middle of the herd before it heads back. |
| `speed` | Number | `0.8` | How fast it walks back. |

### Example

```json
{
  "animal": "ecologies:muskox",
  "range": 16,
  "spread": 6,
  "speed": 0.8
}
```

---

## Natural breeding

When one of the herd gets killed and there aren't any young around, the others go into love mode at
their next meal, so herds bounce back from small losses on their own. The animal needs a
[grazing](#grazing) file too, since this is set off by eating.

**Folder:** `data/<namespace>/ecologies/natural_breeding/`

### Fields

| Field | Type | Default | Description |
|---|---|---|---|
| `animal` | ID list | | The mob that breeds back. |
| `range` | Number | `16` | How close a death has to be for the herd to notice. |
| `love_ticks` | Ticks | `6000` | How long they stay in love mode. |
| `memory_ticks` | Ticks | `24000` | How long they remember the loss. |

### Example

```json
{
  "animal": "ecologies:muskox",
  "range": 16,
  "love_ticks": 6000,
  "memory_ticks": 24000
}
```

---

## Roost

How a colony mob lives around its roost. Bats sleep at the roost during the day, feed over crops at night
and drop guano under the roost when they get back. Take too much guano too fast and the colony leaves.

**Folder:** `data/<namespace>/ecologies/roost/`

### Fields

| Field | Type | Default | Description |
|---|---|---|---|
| `mob` | ID list | | The colony mob. |
| `roost` | ID list | | The roost block it lives at. |
| `lure` | ID list | `"minecraft:spider_eye"` | The item that lures and breeds it. |
| `forage` | ID list | `"#ecologies:bat_forage_crops"` | Blocks it looks for and feeds over at night. |
| `overharvest_limit` | Integer | `32` | Guano layers taken in a row before the colony leaves. |
| `recovery_ticks_per_layer` | Ticks | `3000` | How long it takes for one taken layer to be forgotten. |
| `abandon_ticks` | Ticks | `72000` | How long a roost stays empty after the colony leaves. |

### Example

```json
{
  "mob": "ecologies:colony_bat",
  "roost": "ecologies:bat_roost",
  "lure": "minecraft:spider_eye",
  "forage": "#ecologies:bat_forage_crops",
  "overharvest_limit": 32
}
```

To have bats feed over more things, add to the `#ecologies:bat_forage_crops` block tag instead of
changing this file.

---

## Substrate

A block that slowly breaks down whatever it's growing on, and spreads. Only blocks listed under `from`
count, and once there's none left around it, it stops.

**Folder:** `data/<namespace>/ecologies/substrate/`

### Fields

| Field | Type | Default | Description |
|---|---|---|---|
| `actor` | ID list | | The block doing the breaking down. |
| `needs_sky` | Boolean | `true` | Only works under open sky during the day. |
| `tick_chance` | Chance | `0.25` | Chance a random tick does anything. A block gets one about every 68 seconds. |
| `spread_chance` | Chance | `0.5` | When it does something, the chance it spreads instead of breaking a block down. |
| `spread_block` | Block state | | What it places when it spreads. |
| `spread_radius` | Integer | `8` | How far it can spread. |
| `crowding_limit` | Integer | `12` | It won't spread with this many glow lichen blocks (normal or sprouted) within 4 blocks. |
| `breakdown` | List of steps | | What turns into what. Each step has a `from` (ID list) and a `to` (block state). |

### Example

```json
{
  "actor": "ecologies:sprouted_glow_lichen",
  "spread_block": "minecraft:glow_lichen",
  "breakdown": [
    { "from": "minecraft:stone", "to": "minecraft:cobblestone" },
    { "from": ["minecraft:cobblestone", "minecraft:andesite"], "to": "minecraft:gravel" }
  ]
}
```

Sprouted glow lichen turns stone into cobblestone, then cobblestone and andesite into gravel, and
spreads normal glow lichen around it.

---

## Mushroom farm

One fungus the mushroom farm can grow. Using the `input` item on an empty farm plants it.

**Folder:** `data/<namespace>/ecologies/mushroom_farm/`

### Fields

| Field | Type | Default | Description |
|---|---|---|---|
| `input` | ID list | | The item that plants it. |
| `result` | ID | | The item you get when you harvest. |
| `display` | Block state | *optional* | The block shown on top when it's done. Uses the `input` item's block when left out. |
| `growth_time` | Ticks | `12000` | How long it takes to grow. |
| `min_yield` | Integer | `2` | Fewest `result` items per harvest. |
| `max_yield` | Integer | `4` | Most `result` items per harvest. |
| `accelerators` | List | rotten flesh, logs, sticks | Items you can feed it to speed it up. Each has `items` (ID list) and `ticks` (how much growth it skips). |
| `extras` | List | `[]` | Bonus drops, each rolled on its own at every harvest. Each has `item` (ID), `chance` (Chance, default `1`), and `min` and `max` (default `1`). |

The default accelerators are rotten flesh (2400 ticks), anything in `#minecraft:logs` (1200) and sticks
(300).

### Example

```json
{
  "input": "minecraft:crimson_fungus",
  "result": "minecraft:crimson_fungus",
  "min_yield": 1,
  "max_yield": 2,
  "extras": [
    { "item": "minecraft:nether_wart_block", "chance": 0.6, "min": 1, "max": 2 },
    { "item": "minecraft:shroomlight", "chance": 0.25 }
  ]
}
```

Crimson fungus gives 1 or 2 back each harvest, usually some nether wart blocks, and a shroomlight a quarter
of the time.

---

## Tags

These block and biome tags can be changed like any other tag.

| Tag | What it's for |
|---|---|
| `#ecologies:bat_forage_crops` | Blocks bats feed over at night. |
| `#ecologies:bat_planted_crops` | Crops bats can plant on bare farmland while they feed. |
| `#ecologies:guano` | Guano blocks and layers. |
| `#ecologies:roost_ceiling` | Rock a bat roost can hang from. |
| `#ecologies:shrub_soil` | What tundra shrubs grow on. |
| `#ecologies:moss` | Every moss block, vanilla and coloured. |
| `#ecologies:moss_spreads_onto` | What coloured moss spreads over with bone meal. |
| `#ecologies:dwarf_spruce_soil` | Ground a dwarfed spruce can grow on. |
| `#ecologies:greenstone_replaceable` | Rock a greenstone belt can replace. |
| `#ecologies:dwarfs_trees` (biome) | Biomes where spruce saplings grow into dwarfed spruces. |
| `#ecologies:has_bat_colonies` (biome) | Biomes where bat colonies can generate. |
| `#ecologies:muskox_habitat` (biome) | The muskox's home biomes, for grazing. |
