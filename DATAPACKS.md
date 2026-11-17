# Datapacks

Files go in `data/<namespace>/ecologies/<folder>/<name>.json`.

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

A file at the same path as one of ours replaces it, like `data/ecologies/ecologies/predation/wolf.json`.

### Types

| Type | What it looks like |
|---|---|
| ID | `"minecraft:wolf"` |
| ID list | An id, a tag or a list of them: `["minecraft:sheep", "#c:crops"]` |
| Block state | Written like in commands: `"minecraft:glow_lichen[down=true]"` |
| Ticks | 20 is a second, 24000 is a day |
| Chance | `0` to `1` |

## Predation

Predators will hunt when they're hungry and then take breaks after a kill, their gameplay loop is pretty simple for the most part.

`ecologies/predation/`

### Fields

| Field | Type | Default | Description |
|---|---|---|---|
| `predator` | ID list | | The mob that hunts. |
| `prey` | ID list | | The mobs it hunts. |
| `hunger_cooldown` | Ticks | `24000` | How long after a kill before it's hungry again. |
| `prefers_babies` | Boolean | `true` | Goes for young prey nearby before adults. |
| `eats_kill` | Boolean | `true` | Kills don't drop food. |
| `adds_hunting` | Boolean | `false` | Adds a hunting goal for mobs that don't hunt in vanilla. |
| `prey_flee_range` | Number | `10` | How close it gets before prey run. `0` turns it off. |
| `chase_speed` | Number | `0` | Chases and bites on the run at this speed instead of using its own attack. `0` turns it off. |
| `sprint` | Number | `1.0` | Speed multiplier while chasing. |
| `stalks` | Boolean | `false` | Sneaks up on prey. |
| `give_up_after` | Ticks | `200` | How long it chases before giving up. Packs last 3x as long. |
| `pack_size` | Integer | `0` | How many have to hunt together to count as a pack. `0` turns it off. |
| `rush_range` | Number | `0` | Distance where a stalker stops sneaking and rushes in. `0` turns it off. |

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

## Defense

Animals that ram back when they or their young get attacked, They'll run from predators instead of attacking if the herd's outnumbered. (I'll probably make changes to this when I expand on it later since not all animals will just ram back their attackers but since this works for now oh well)

`ecologies/defense/`

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

## Grazing

Animals will eat blocks registered as food and then drop an item some time after. They'll drop more when more welfare checks are met. Things such as: open space, a few of their kind nearby, open sky, and being in their home biome. (I'll expand on this later as well)

`ecologies/grazing/`

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

## Herding

Keeps animals of the same kind together.

`ecologies/herding/`

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

## Natural breeding

When one of the herd gets killed the rest of the herd will go into woohoo mode at their next meal. Needs a [grazing](#grazing) file too.

`ecologies/natural_breeding/`

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

## Roost

Bat colonies home block basically, if you take too much guano at once the colony leaves.

`ecologies/roost/`

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

## Substrate

This is really only for lichen but if you'd like to add other fungi that naturally break down blocks, kind of like an immersive weathering type of feature, you can define them as a substrate.

`ecologies/substrate/`

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

## Mushroom farm

Fungi and related foliage/items the mushroom farm can grow.

`ecologies/mushroom_farm/`

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

Default accelerators are rotten flesh (2400), `#minecraft:logs` (1200) and sticks (300).

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

## Tags

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
