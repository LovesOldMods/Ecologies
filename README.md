# Ecologies

An ecology mod for NeoForge **1.21.1** and **26.3**, where most things can be tweaked with datapacks.
Based on Klei Wright's (Matcha Flavoured) tundra and bat design videos.

## Building

Gradle needs **Java 25** to run (Stonecutter 0.10 won't work without it). The 1.21.1 build still
compiles for Java 21 and the 26.3 one for Java 25. Gradle picks the right compiler on its own as long
as you've got both JDKs installed.

```
./gradlew :26.3:build :1.21.1:build     # the jars end up in versions/<version>/build/libs/
./gradlew :26.3:runClient               # or :1.21.1:runClient, :26.3:runServer, ...
```

Setting up Minecraft the first time needs about 4 GB of free memory for the decompiler.

## One codebase, two versions

[Stonecutter](https://stonecutter.kikugie.dev) builds the same code once for each version. Anything
that's different between versions gets wrapped in version comments:

```java
//? if >=1.21.2 {
return BLOCKS.registerBlock(name, factory, properties);
//?} else {
/*return BLOCKS.registerBlock(name, factory, properties.get());
*///?}
```

The code in git is always set up for 26.3. If you want to work on 1.21.1 in your IDE, run the
`Set active project to 1.21.1` Gradle task, then `Reset active project` before you commit.

Version numbers for each Minecraft version (NeoForge, mappings and libraries) live in
`stonecutter.properties.toml`.

## Resources

- `src/main/resources/` – files that are the same on both versions (block states, models, textures,
  lang, tags).
- `src/main/resources-<version>/` – **generated** files that are written differently on each version
  (loot tables, recipes, 26.3 item model definitions).

Don't edit the generated files by hand. Change the tables in `tools/gen_resources.py` and run it. It reads
vanilla's biome files out of the jars Gradle sets up, so run a build once first:

```
python3 tools/gen_resources.py
python3 tools/check_assets.py 26.3 versions/26.3/build/moddev/artifacts/minecraft-patched-26.3.0.103.jar
```

The tundra's worldgen (the biomes, built off that version's vanilla snowy plains and snowy taiga,
plus the features, placed features, biome modifiers and surface rules) comes from
`tools/gen_tundra.py`, which `gen_resources.py` runs for you. On 26.3 the surface rules are a datapack
material rule. On 1.21.1 it's the same rule, read from `ecologies/surface_rules.json` inside the jar.

`art/pack/` has the original tundra resource pack the textures and lichen models came from.

## Datapacks and config

The behaviours are written in Java, and JSON under `data/<namespace>/ecologies/<folder>/` turns them on
and tunes them. Every folder, field and tag is in [DATAPACKS.md](DATAPACKS.md).

`config/ecologies.toml` is just on/off switches: predation, crop bolting, bat colonies, lichen
breakdown, herds, and which tundra moss style to use.

## Testing tools

- `tools/smoke_server.sh <version>` starts a server and tells you about any errors.
- `tools/scenario.sh <version> <script>` runs a list of server commands (see `tools/scenarios/`). The
  `/ecologies randomtick`, `/ecologies survey` and `/ecologies biomes` op commands are there for these.
- `tools/showcase.sh <version>` builds a display of every block and mob, opens it in the client on a
  virtual display and takes a screenshot (works on 1.21.1; 26.3's client needs a real GPU).

## Credits and license

Designs, textures and models are by **Klei Wright** (Matcha Flavoured). Code by **LovesOldMods**.
Everything's under **CC BY-NC-SA 4.0**, same as Matcha Flavoured's work: credit the authors, share
alike, and no commercial use. See `LICENSE`.
