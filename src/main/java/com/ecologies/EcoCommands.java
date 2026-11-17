package com.ecologies;

import com.ecologies.worldgen.EcoBiomes;
import com.ecologies.worldgen.TundraMoss;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

// Testing commands.
public final class EcoCommands {
    private EcoCommands() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EcoCommands::onRegister);
    }

    private static void onRegister(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("ecologies")
                //? if >=1.21.11 {
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
                //?} else
                /*.requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS));*/
        root.then(Commands.literal("randomtick")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(ctx -> randomTick(ctx, 1))
                        .then(Commands.argument("times", IntegerArgumentType.integer(1, 10000))
                                .executes(ctx -> randomTick(ctx, IntegerArgumentType.getInteger(ctx, "times"))))));
        root.then(Commands.literal("biomes")
                .then(Commands.argument("radius", IntegerArgumentType.integer(64, 16384))
                        .executes(EcoCommands::biomes)));
        root.then(Commands.literal("survey")
                .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                                        .executes(EcoCommands::survey)))));
        root.then(Commands.literal("find")
                .then(Commands.argument("block", IdentifierArgument.id())
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 128))
                                                .executes(EcoCommands::find))))));
        event.getDispatcher().register(root);
    }

    private static int survey(CommandContext<CommandSourceStack> ctx) {
        int cx = IntegerArgumentType.getInteger(ctx, "x"), cz = IntegerArgumentType.getInteger(ctx, "z");
        int r = IntegerArgumentType.getInteger(ctx, "radius");
        ServerLevel level = ctx.getSource().getLevel();
        Map<String, Integer> ground = new TreeMap<>(), plants = new TreeMap<>(),
                rock = new TreeMap<>(), biomes = new TreeMap<>(), special = new TreeMap<>(),
                tundraGround = new TreeMap<>(), onRock = new TreeMap<>();
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                level.getChunk(x >> 4, z >> 4);
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos g = new BlockPos(x, top - 1, z);
                count(ground, level.getBlockState(g));
                if (level.getBiome(g).is(EcoBiomes.TUNDRA)) count(tundraGround, level.getBlockState(g));
                count(plants, level.getBlockState(g.above()));
                if (TundraMoss.isRock(level.getBlockState(g))) count(onRock, level.getBlockState(g.above()));
                count(rock, level.getBlockState(g.below(12)));
                biomes.merge(level.getBiome(g).unwrapKey().map(k -> k.toString()).orElse("?"), 1, Integer::sum);
                for (int y = top + 8; y > top - 48; y--) {
                    var s = level.getBlockState(new BlockPos(x, y, z));
                    String id = BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString();
                    if (id.startsWith("ecologies:") || id.contains("iron") || id.equals("minecraft:smooth_basalt")
                            || id.equals("minecraft:podzol") || id.equals("minecraft:spruce_leaves") || id.contains("dripstone")
                            || id.equals("minecraft:snow")) special.merge(id, 1, Integer::sum);
                }
            }
        }
        var box = new AABB(cx - r, Compat.minY(level), cz - r, cx + r + 1, 400, cz + r + 1);
        Map<String, Integer> mobs = new TreeMap<>();
        for (var e : level.getEntitiesOfClass(Mob.class, box)) {
            mobs.merge(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString(), 1, Integer::sum);
        }
        send(ctx, "Survey biomes " + biomes);
        send(ctx, "Survey ground " + ground);
        send(ctx, "Survey plants " + plants);
        if (!tundraGround.isEmpty()) send(ctx, "Survey tundra-ground " + tundraGround);
        send(ctx, "Survey on-rock " + onRock);
        send(ctx, "Survey rock(-12) " + rock);
        send(ctx, "Survey special " + special);
        send(ctx, "Survey mobs " + mobs);
        return 1;
    }

    // Reads the biome source directly, doesn't make chunks.
    private static int biomes(CommandContext<CommandSourceStack> ctx) {
        int r = IntegerArgumentType.getInteger(ctx, "radius"), step = 64, n = r * 2 / step + 1;
        ServerLevel level = ctx.getSource().getLevel();
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        //? if >=26.1 {
        var resolver = source.createCachingResolver(level.getChunkSource().randomState());
        //?} else {
        /*var sampler = level.getChunkSource().randomState().sampler();
        BiomeResolver resolver = (qx, qy, qz, unused) -> source.getNoiseBiome(qx, qy, qz, sampler);
        *///?}
        String[][] grid = new String[n][n];
        Map<String, Integer> counts = new TreeMap<>();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int x = -r + i * step, z = -r + j * step;
                //? if >=26.1 {
                var biome = resolver.getNoiseBiome(x >> 2, 63 >> 2, z >> 2);
                //?} else
                /*var biome = resolver.getNoiseBiome(x >> 2, 63 >> 2, z >> 2, null);*/
                String id = biome.unwrapKey().map(k -> Compat.path(k)).orElse("?");
                grid[i][j] = id;
                counts.merge(id, 1, Integer::sum);
            }
        }
        StringBuilder out = new StringBuilder("Biomes (" + n * n + " samples):");
        for (String ours : new String[] {"tundra", "lichen_woodland", "snowy_plains", "snowy_taiga", "taiga"}) {
            // Biggest patch, counted in 64x64 samples.
            boolean[][] seen = new boolean[n][n];
            int biggest = 0, patches = 0;
            for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
                if (seen[i][j] || !ours.equals(grid[i][j])) continue;
                patches++;
                int size = 0;
                ArrayDeque<int[]> queue = new ArrayDeque<>();
                queue.add(new int[] {i, j});
                seen[i][j] = true;
                while (!queue.isEmpty()) {
                    int[] c = queue.poll();
                    size++;
                    for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                        int a = c[0] + d[0], b = c[1] + d[1];
                        if (a >= 0 && b >= 0 && a < n && b < n && !seen[a][b] && ours.equals(grid[a][b])) {
                            seen[a][b] = true;
                            queue.add(new int[] {a, b});
                        }
                    }
                }
                biggest = Math.max(biggest, size);
            }
            out.append(" ").append(ours).append("=").append(counts.getOrDefault(ours, 0))
                    .append(" (patches ").append(patches).append(", biggest ").append(biggest).append(")");
        }
        send(ctx, out.toString());
        return 1;
    }

    private static int find(CommandContext<CommandSourceStack> ctx) {
        var id = IdentifierArgument.getId(ctx, "block");
        int cx = IntegerArgumentType.getInteger(ctx, "x"), cz = IntegerArgumentType.getInteger(ctx, "z");
        int r = IntegerArgumentType.getInteger(ctx, "radius");
        ServerLevel level = ctx.getSource().getLevel();
        Map<Long, Integer> hits = new HashMap<>();
        Map<Long, Integer> heights = new HashMap<>();
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                level.getChunk(x >> 4, z >> 4);
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                for (int y = top; y > top - 4; y--) {
                    var s = level.getBlockState(new BlockPos(x, y, z));
                    if (BuiltInRegistries.BLOCK.getKey(s.getBlock()).equals(id)) {
                        long key = BlockPos.asLong(x >> 2, 0, z >> 2);
                        hits.merge(key, 1, Integer::sum);
                        heights.put(key, y);
                    }
                }
            }
        }
        var best = hits.entrySet().stream().max(Map.Entry.comparingByValue());
        if (best.isEmpty()) {
            send(ctx, "Find " + id + " none");
            return 0;
        }
        BlockPos cell = BlockPos.of(best.get().getKey());
        send(ctx, "Find " + id + " at " + (cell.getX() * 4 + 2) + " " + heights.get(best.get().getKey()) + " " + (cell.getZ() * 4 + 2)
                + " (" + best.get().getValue() + " in a 4x4 patch, " + hits.size() + " patches)");
        return 1;
    }

    private static void count(Map<String, Integer> map, BlockState s) {
        map.merge(BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath(), 1, Integer::sum);
    }

    private static void send(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
    }

    private static int randomTick(CommandContext<CommandSourceStack> ctx, int times) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        ServerLevel level = ctx.getSource().getLevel();
        int ticked = 0;
        for (int i = 0; i < times; i++) {
            var state = level.getBlockState(pos);
            if (!state.isRandomlyTicking()) break;
            state.randomTick(level, pos, level.getRandom());
            ticked++;
        }
        int done = ticked;
        ctx.getSource().sendSuccess(() -> Component.literal("Random-ticked " + pos.toShortString() + " " + done + " times"), true);
        return done;
    }
}
