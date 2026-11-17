package com.ecologies.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.SnowLayerBlock;
import com.ecologies.Compat;
import com.ecologies.entity.ColonyBat;
import com.ecologies.registry.EcoBlocks;
import com.ecologies.registry.EcoEntities;
import com.ecologies.registry.EcoTags;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
//? if >=26.1 {
import net.minecraft.world.level.block.state.properties.SpeleothemThickness;
//?} else
/*import net.minecraft.world.level.block.state.properties.DripstoneThickness;*/
//? if >=1.21.2 {
import net.minecraft.world.entity.EntitySpawnReason;
//?}

public class BatColonyFeature extends EcoFeature {
    @Override
    public boolean generate(WorldGenLevel level, RandomSource random, BlockPos origin) {
        CaveMouth.Mouth mouth = CaveMouth.find(level, random, origin, 8);
        if (mouth != null) {
            CaveMouth.decorate(level, random, mouth);
            if (roostInMouth(level, random, mouth)) return true;
        }
        return roostInCave(level, random, origin);
    }

    public static boolean roostInMouth(WorldGenLevel level, RandomSource random, CaveMouth.Mouth mouth) {
        BlockPos center = mouth.inside().relative(mouth.inward(), 3);
        BlockPos ledge = null, hang = null;
        double ledgeD = Double.MAX_VALUE, hangD = Double.MAX_VALUE;
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    BlockPos air = center.offset(dx, dy, dz);
                    BlockState a = level.getBlockState(air);
                    if (!a.isAir()) continue;
                    if (!level.getBlockState(air.below()).isAir() && dy > -1) continue; // start from near the floor
                    BlockPos ceiling = CaveMouth.rockAbove(level, air, 9);
                    if (ceiling == null || !level.getBlockState(ceiling).is(EcoTags.ROOST_CEILING)) continue;
                    // rock only, the sky past the mouth doesn't count
                    if (level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, air.getX(), air.getZ()) <= ceiling.getY()) continue;
                    double d = dx * dx + dz * dz + random.nextDouble();
                    if (isLedge(level, ceiling) && clearBelow(level, ceiling, 4)) {
                        if (d < ledgeD) { ledgeD = d; ledge = ceiling; }
                    } else if (clearBelow(level, ceiling.below(), 3) && d < hangD) {
                        hangD = d;
                        hang = ceiling.below();
                    }
                }
            }
        }
        if (ledge != null && roost(level, random, ledge, ledge.below(), ledgeSide(level, ledge))) return true;
        if (hang != null) {
            for (int h = 0; h < 4 && level.getBlockState(hang.below(h)).is(Blocks.POINTED_DRIPSTONE); h++) {
                set(level, hang.below(h), Blocks.AIR.defaultBlockState());
            }
            return roost(level, random, hang, hang.below(), Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }
        return false;
    }

    private static boolean clearBelow(WorldGenLevel level, BlockPos top, int depth) {
        for (int h = 1; h <= depth; h++) {
            BlockState b = level.getBlockState(top.below(h));
            if (!b.isAir() && !b.is(Blocks.POINTED_DRIPSTONE)) return false;
        }
        return true;
    }

    private static boolean isLedge(WorldGenLevel level, BlockPos ceiling) {
        return ledgeSide(level, ceiling) != null;
    }

    private static Direction ledgeSide(WorldGenLevel level, BlockPos ceiling) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos side = ceiling.relative(d);
            if (level.getBlockState(side).isAir() && level.getBlockState(side.below()).isAir()) return d;
        }
        return null;
    }

    private static boolean roostInCave(WorldGenLevel level, RandomSource random, BlockPos origin) {
        int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX(), origin.getZ());
        int minY = Math.max(Compat.minY(level.getLevel()) + 8, surface - 40);
        for (int i = 0; i < 6; i++) {
            int x = origin.getX() + random.nextInt(9) - 4;
            int z = origin.getZ() + random.nextInt(9) - 4;
            for (int y = surface - 5; y > minY; y--) {
                BlockPos ceiling = new BlockPos(x, y + 1, z);
                if (!level.getBlockState(ceiling.below()).isAir()) continue;
                if (tryRoost(level, random, ceiling)) return true;
                if (level.getBlockState(ceiling).is(EcoTags.ROOST_CEILING)) break; // one ceiling per column
            }
        }
        return false;
    }

    // only use half the spots in dripstone caves or they get packed with roosts
    private static boolean tryRoost(WorldGenLevel level, RandomSource random, BlockPos ceiling) {
        if (!level.getBlockState(ceiling).is(EcoTags.ROOST_CEILING)) return false;
        for (int h = 1; h <= 4; h++) {
            BlockState below = level.getBlockState(ceiling.below(h));
            if (!below.isAir() && !(h == 1 && below.is(Blocks.POINTED_DRIPSTONE))) return false;
        }
        int open = 0;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (level.getBlockState(ceiling.offset(dx, -1, dz)).isAir()) open++;
        if (open < 7) return false; // not a narrow shaft
        Direction edge = null;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos side = ceiling.relative(d);
            if (level.getBlockState(side).isAir() && level.getBlockState(side.below()).isAir()) {
                edge = d;
                break;
            }
        }
        if (edge == null) return false;
        if (level.getBiome(ceiling).is(Biomes.DRIPSTONE_CAVES) && random.nextBoolean()) return false;
        return roost(level, random, ceiling, ceiling.below(), edge);
    }

    private static final int SPACING = 15;

    // roosts placed this session, stops chunks next to each other both getting one
    private static final Map<Long, List<BlockPos>> PLACED = new ConcurrentHashMap<>();

    // worldgen only lets us read some chunks, the list covers the rest
    private static boolean spacedOut(WorldGenLevel level, BlockPos pos) {
        List<BlockPos> placed = PLACED.computeIfAbsent(level.getSeed(), k -> new ArrayList<>());
        synchronized (placed) {
            for (BlockPos p : placed) if (p.distSqr(pos) < SPACING * SPACING) return false;
        }
        int minX = pos.getX() - SPACING, maxX = pos.getX() + SPACING, minZ = pos.getZ() - SPACING, maxZ = pos.getZ() + SPACING;
        if (level instanceof WorldGenRegion region) {
            ChunkPos c = region.getCenter();
            minX = Math.max(minX, c.getMinBlockX() - 16); maxX = Math.min(maxX, c.getMinBlockX() + 31);
            minZ = Math.max(minZ, c.getMinBlockZ() - 16); maxZ = Math.min(maxZ, c.getMinBlockZ() + 31);
        }
        for (BlockPos p : BlockPos.betweenClosed(minX, pos.getY() - 10, minZ, maxX, pos.getY() + 10, maxZ)) {
            if (p.distSqr(pos) < SPACING * SPACING && level.getBlockState(p).is(EcoBlocks.BAT_ROOST.get())) return false;
        }
        return true;
    }

    private static void remember(WorldGenLevel level, BlockPos pos) {
        List<BlockPos> placed = PLACED.computeIfAbsent(level.getSeed(), k -> new ArrayList<>());
        synchronized (placed) {
            placed.add(pos.immutable());
            if (placed.size() > 4096) placed.remove(0);
        }
    }

    private static boolean roost(WorldGenLevel level, RandomSource random, BlockPos ceiling, BlockPos air, Direction open) {
        if (!spacedOut(level, ceiling)) return false;
        BlockState was = level.getBlockState(ceiling);
        set(level, ceiling, EcoBlocks.BAT_ROOST.get().defaultBlockState());
        dripstone(level, random, ceiling, ceiling.relative(open));
        if (!enoughDripstone(level, random, ceiling, open)) {
            set(level, ceiling, was);
            return false;
        }
        remember(level, ceiling);
        guano(level, random, air);
        spawnColony(level, random, ceiling);
        return true;
    }

    private static boolean enoughDripstone(WorldGenLevel level, RandomSource random, BlockPos roost, Direction open) {
        int blocks = 0;
        for (Direction d : Direction.values()) if (level.getBlockState(roost.relative(d)).is(Blocks.DRIPSTONE_BLOCK)) blocks++;
        List<Direction> sides = new ArrayList<>(List.of(Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST));
        Collections.shuffle(sides.subList(1, 5), new Random(random.nextLong()));
        for (Direction d : sides) {
            if (blocks >= 2) break;
            if (d == open) continue; // keep the open side open
            BlockPos p = roost.relative(d);
            BlockState s = level.getBlockState(p);
            boolean rock = s.is(EcoTags.ROOST_CEILING) && !s.is(Blocks.DRIPSTONE_BLOCK);
            boolean hangsOff = s.isAir() && d != Direction.UP && level.getBlockState(p.above()).isFaceSturdy(level, p.above(), Direction.DOWN);
            if (rock || hangsOff) {
                set(level, p, Blocks.DRIPSTONE_BLOCK.defaultBlockState());
                blocks++;
            }
        }
        if (blocks < 2) return false;
        for (BlockPos p : BlockPos.betweenClosed(roost.offset(-2, -2, -2), roost.offset(2, 2, 2))) {
            if (level.getBlockState(p).is(Blocks.POINTED_DRIPSTONE)) return true;
        }
        for (BlockPos p : BlockPos.betweenClosed(roost.offset(-2, -1, -2), roost.offset(2, 1, 2))) {
            if (level.getBlockState(p).is(Blocks.DRIPSTONE_BLOCK) && level.getBlockState(p.below()).isAir()
                    && level.getBlockState(p.below(2)).isAir()) {
                hang(level, random, p.below());
                if (level.getBlockState(p.below()).is(Blocks.POINTED_DRIPSTONE)) return true;
            }
        }
        return false;
    }

    private static void dripstone(WorldGenLevel level, RandomSource random, BlockPos roost, BlockPos opening) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 == 0 || d2 > 9) continue;
                if (d2 > 2 && random.nextFloat() < (d2 > 5 ? 0.6F : 0.25F)) continue; // always next to the roost
                // Follow the ceiling up or down a block.
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos p = roost.offset(dx, dy, dz);
                    if (p.equals(opening)) break;
                    BlockState s = level.getBlockState(p);
                    if (!s.is(EcoTags.ROOST_CEILING) || !level.getBlockState(p.below()).isAir()) continue;
                    set(level, p, Blocks.DRIPSTONE_BLOCK.defaultBlockState());
                    if (random.nextInt(4) == 0) hang(level, random, p.below());
                    break;
                }
            }
        }
    }

    private static void guano(WorldGenLevel level, RandomSource random, BlockPos underRoost) {
        BlockPos floor = floorBelow(level, underRoost, 24);
        if (floor == null) return;
        pile(level, floor, 2 + random.nextInt(2)); // under the roost: 2-3 layers
        int piles = 8 + random.nextInt(6);
        for (int i = 0; i < piles; i++) {
            int dx = random.nextInt(9) - 4, dz = random.nextInt(9) - 4;
            int d2 = dx * dx + dz * dz;
            if (d2 > 17) continue;
            BlockPos spot = floorBelow(level, floor.offset(dx, 3, dz), 7);
            if (spot == null) continue;
            pile(level, spot, Math.max(1, (d2 <= 2 ? 3 : d2 <= 5 ? 2 : 1) - random.nextInt(2)));
        }
    }

    private static BlockPos floorBelow(WorldGenLevel level, BlockPos from, int reach) {
        for (int dy = 0; dy < reach; dy++) {
            BlockPos p = from.below(dy);
            BlockState here = level.getBlockState(p);
            if (!here.isAir() && !here.is(Blocks.POINTED_DRIPSTONE) && !here.is(EcoBlocks.GUANO_LAYER.get())) return null;
            if (here.isAir() && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) return p;
            if (here.is(EcoBlocks.GUANO_LAYER.get())) return p;
        }
        return null;
    }

    private static void pile(WorldGenLevel level, BlockPos at, int layers) {
        BlockState here = level.getBlockState(at);
        int have = here.is(EcoBlocks.GUANO_LAYER.get()) ? here.getValue(SnowLayerBlock.LAYERS) : 0;
        if (!here.isAir() && have == 0) return;
        BlockState pile = EcoBlocks.GUANO_LAYER.get().defaultBlockState()
                // max 3 at worldgen, the bats stack it higher on their own later
                .setValue(SnowLayerBlock.LAYERS, Math.min(3, Math.max(have, layers)));
        if (pile.canSurvive(level, at)) set(level, at, pile);
    }

    private static void hang(WorldGenLevel level, RandomSource random, BlockPos top) {
        int length = 1 + random.nextInt(3);
        for (int i = 0; i < length; i++) {
            if (!level.getBlockState(top.below(i)).isAir() || !level.getBlockState(top.below(i + 1)).isAir()) {
                length = i;
                break;
            }
        }
        for (int i = 0; i < length; i++) {
            String thickness = i == length - 1 ? "TIP" : i == length - 2 ? "FRUSTUM" : "MIDDLE";
            if (length >= 3 && i == 0) thickness = "BASE";
            BlockState drip = Blocks.POINTED_DRIPSTONE.defaultBlockState().setValue(BlockStateProperties.VERTICAL_DIRECTION, Direction.DOWN);
            //? if >=26.1 {
            drip = drip.setValue(BlockStateProperties.SPELEOTHEM_THICKNESS, SpeleothemThickness.valueOf(thickness));
            //?} else
            /*drip = drip.setValue(BlockStateProperties.DRIPSTONE_THICKNESS, DripstoneThickness.valueOf(thickness));*/
            set(level, top.below(i), drip);
        }
    }

    private static void spawnColony(WorldGenLevel level, RandomSource random, BlockPos roost) {
        int count = 6 + random.nextInt(5);
        for (int i = 0; i < count; i++) {
            //? if >=1.21.2 {
            ColonyBat bat = EcoEntities.COLONY_BAT.get().create(level.getLevel(), EntitySpawnReason.STRUCTURE);
            //?} else
            /*ColonyBat bat = EcoEntities.COLONY_BAT.get().create(level.getLevel());*/
            if (bat == null) return;
            BlockPos spot = roost.below();
            for (int t = 0; t < 6; t++) {
                BlockPos p = roost.offset(random.nextInt(5) - 2, -1 - random.nextInt(2), random.nextInt(5) - 2);
                if (level.getBlockState(p).isAir()) {
                    spot = p;
                    break;
                }
            }
            Compat.place(bat, spot.getX() + 0.5, spot.getY() + 0.1, spot.getZ() + 0.5, random.nextFloat() * 360F);
            bat.setRoost(roost);
            bat.setResting(true);
            level.addFreshEntityWithPassengers(bat);
        }
    }
}
