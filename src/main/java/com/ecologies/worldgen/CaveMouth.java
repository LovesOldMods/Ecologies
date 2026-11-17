package com.ecologies.worldgen;

import java.util.Collections;
import java.util.Random;
import com.ecologies.registry.EcoTags;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
//? if >=26.1 {
import net.minecraft.world.level.block.state.properties.SpeleothemThickness;
//?} else
/*import net.minecraft.world.level.block.state.properties.DripstoneThickness;*/

public final class CaveMouth {
    public record Mouth(BlockPos inside, Direction inward) {}

    private static final int WALK = 6, RADIUS = 6, MIN_HEIGHT = 5;

    private CaveMouth() {}

    public static Mouth find(WorldGenLevel level, RandomSource random, BlockPos origin, int tries) {
        // Every other column, random corner first.
        int cx = origin.getX() & ~15, cz = origin.getZ() & ~15;
        int ox = random.nextInt(2), oz = random.nextInt(2);
        List<int[]> columns = new ArrayList<>();
        for (int dx = ox; dx < 16; dx += 2) for (int dz = oz; dz < 16; dz += 2) columns.add(new int[] {cx + dx, cz + dz});
        Collections.shuffle(columns, new Random(random.nextLong()));
        for (int[] column : columns) {
            int x = column[0], z = column[1];
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            BlockPos start = new BlockPos(x, y, z);
            if (!level.getBlockState(start).isAir() || level.getBlockState(start.below()).isAir()) continue;
            List<Direction> dirs = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
            Collections.shuffle(dirs, new Random(random.nextLong()));
            for (Direction dir : dirs) {
                BlockPos p = start;
                for (int i = 1; i <= WALK; i++) {
                    BlockPos next = p.relative(dir);
                    // Follow the ground a block up or down as it goes.
                    if (!level.getBlockState(next).isAir()) {
                        if (level.getBlockState(next.above()).isAir()) next = next.above();
                        else break;
                    } else if (level.getBlockState(next.below()).isAir() && !level.getBlockState(next.below(2)).isAir()) {
                        next = next.below();
                    }
                    p = next;
                    if (level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, p.getX(), p.getZ()) <= p.getY()) continue; // still open sky
                    // Mouths can narrow at the lip, check further in.
                    BlockPos q = p;
                    for (int d = 0; d < 4; d++) {
                        if (bigEnough(level, q, dir)) {
                            return new Mouth(q, dir);
                        }
                        BlockPos in = q.relative(dir);
                        if (level.getBlockState(in).isAir()) q = in;
                        else if (level.getBlockState(in.above()).isAir()) q = in.above();
                        else if (level.getBlockState(in.below()).isAir()) q = in.below();
                        else break;
                    }
                    break;
                }
            }
        }
        return null;
    }

    private static boolean bigEnough(WorldGenLevel level, BlockPos mouth, Direction inward) {
        BlockPos ceiling = ceilingAbove(level, mouth, 8);
        // Rock or dirt overhead is fine. Dripstone only goes on rock.
        if (ceiling == null || ceiling.getY() - mouth.getY() < MIN_HEIGHT || !level.getBlockState(ceiling).isCollisionShapeFullBlock(level, ceiling)) return false;
        // 2 wide minimum, solid floor.
        Direction side = inward.getClockWise();
        boolean wide = false;
        for (int s = -1; s <= 1; s += 2) {
            boolean clear = true;
            for (int h = 0; h < 2; h++) if (!level.getBlockState(mouth.relative(side, s).above(h)).isAir()) clear = false;
            wide |= clear;
        }
        for (int h = 0; h < MIN_HEIGHT; h++) if (!level.getBlockState(mouth.above(h)).isAir()) return false;
        if (!wide || level.getBlockState(mouth.below()).isAir()) return false;
        for (int d = 1; d <= 2; d++) {
            for (int h = 0; h < 2; h++) if (!level.getBlockState(mouth.relative(inward, d).above(h)).isAir()) return false;
        }
        return true;
    }

    static BlockPos ceilingAbove(WorldGenLevel level, BlockPos pos, int reach) {
        for (int dy = 1; dy <= reach; dy++) {
            if (!level.getBlockState(pos.above(dy)).isAir()) return pos.above(dy);
        }
        return null;
    }

    static BlockPos rockAbove(WorldGenLevel level, BlockPos pos, int reach) {
        for (int dy = 1; dy <= reach; dy++) {
            BlockState s = level.getBlockState(pos.above(dy));
            if (!s.isAir() && !s.is(Blocks.POINTED_DRIPSTONE)) return pos.above(dy);
        }
        return null;
    }

    public static void decorate(WorldGenLevel level, RandomSource random, Mouth mouth) {
        BlockPos center = mouth.inside().relative(mouth.inward());
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > RADIUS * RADIUS) continue;
                // Solid in the middle, ragged at the edge.
                if (random.nextFloat() < (d2 <= 9 ? 0.05F : d2 <= 20 ? 0.3F : 0.6F)) continue;
                BlockPos column = center.offset(dx, 0, dz);
                if (!level.getBlockState(column).isAir()) {
                    if (level.getBlockState(column.above()).isAir()) column = column.above();
                    else if (level.getBlockState(column.below()).isAir()) column = column.below();
                    else continue;
                }
                BlockPos ceiling = ceilingAbove(level, column, 7);
                if (ceiling != null && level.getBlockState(ceiling).is(EcoTags.ROOST_CEILING)) {
                    EcoFeature.set(level, ceiling, Blocks.DRIPSTONE_BLOCK.defaultBlockState());
                    if (random.nextInt(3) == 0 && level.getBlockState(ceiling.above()).is(EcoTags.ROOST_CEILING)) {
                        EcoFeature.set(level, ceiling.above(), Blocks.DRIPSTONE_BLOCK.defaultBlockState());
                    }
                    if (random.nextInt(5) < 2) hang(level, random, ceiling.below(), 1 + random.nextInt(d2 <= 9 ? 4 : 2));
                }
                BlockPos floor = column;
                for (int dy = 0; dy < 4 && level.getBlockState(floor).isAir(); dy++) floor = floor.below();
                if (!level.getBlockState(floor).isAir() && level.getBlockState(floor).is(EcoTags.ROOST_CEILING)
                        && level.getBlockState(floor.above()).isAir() && random.nextFloat() < 0.45F) {
                    EcoFeature.set(level, floor, Blocks.DRIPSTONE_BLOCK.defaultBlockState());
                    if (random.nextInt(5) == 0) stand(level, random, floor.above(), 1 + random.nextInt(3));
                }
            }
        }
    }

    public static void hang(WorldGenLevel level, RandomSource random, BlockPos top, int length) {
        point(level, top, length, Direction.DOWN);
    }

    public static void stand(WorldGenLevel level, RandomSource random, BlockPos bottom, int length) {
        point(level, bottom, length, Direction.UP);
    }

    private static void point(WorldGenLevel level, BlockPos start, int length, Direction tip) {
        // Leave a gap, don't touch the other side.
        for (int i = 0; i < length; i++) {
            if (!level.getBlockState(start.relative(tip, i)).isAir() || !level.getBlockState(start.relative(tip, i + 1)).isAir()) {
                length = i;
                break;
            }
        }
        for (int i = 0; i < length; i++) {
            String thickness = i == length - 1 ? "TIP" : i == length - 2 ? "FRUSTUM" : "MIDDLE";
            if (length >= 3 && i == 0) thickness = "BASE";
            BlockState drip = Blocks.POINTED_DRIPSTONE.defaultBlockState().setValue(BlockStateProperties.VERTICAL_DIRECTION, tip);
            //? if >=26.1 {
            drip = drip.setValue(BlockStateProperties.SPELEOTHEM_THICKNESS, SpeleothemThickness.valueOf(thickness));
            //?} else
            /*drip = drip.setValue(BlockStateProperties.DRIPSTONE_THICKNESS, DripstoneThickness.valueOf(thickness));*/
            EcoFeature.set(level, start.relative(tip, i), drip);
        }
    }
}
