package com.ecologies.worldgen;

import com.ecologies.block.SkinnyLogBlock;
import com.ecologies.registry.EcoBlocks;
import com.ecologies.registry.EcoTags;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public class DwarfSpruceFeature extends EcoFeature {
    @Override
    public boolean generate(WorldGenLevel level, RandomSource random, BlockPos origin) {
        BlockState ground = level.getBlockState(origin.below());
        if (!ground.is(EcoTags.DWARF_SPRUCE_SOIL)) return false;
        int logs = 2 + random.nextInt(3);          // thick part
        int crown = 5 + random.nextInt(5);         // thin part
        int height = logs + crown;
        for (int y = 0; y <= height + 1; y++) {
            BlockState s = level.getBlockState(origin.above(y));
            if (y == 0 && s.is(BlockTags.SAPLINGS)) continue;
            if (!s.isAir() && !s.canBeReplaced() && !s.is(BlockTags.LEAVES)) return false;
        }

        List<BlockPos> placed = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            BlockState log = (y < logs ? EcoBlocks.DWARF_SPRUCE_LOG : EcoBlocks.DWARF_SPRUCE_LOG_POST_CROWN).get().defaultBlockState();
            set(level, origin.above(y), log);
            placed.add(origin.above(y));
        }
        BlockPos top = origin.above(height - 1);

        // Bare lower trunk, then a ring every other block.
        int firstWhorl = Math.max(logs - 1, height / 3);
        int span = Math.max(1, height - 1 - firstWhorl);
        for (int y = height - 1; y >= firstWhorl; y--) {
            int k = height - 1 - y;          // layers down from the top
            double t = k / (double) span;    // 0 at the top, 1 at the bottom of the crown
            if (k % 2 == 1) continue;        // bare trunk between rings, so the tree stays open and tall
            ring(level, origin.above(y), t < 0.45 ? Ring.PLUS : t < 0.75 ? Ring.SQUARE : Ring.SQUARE_TIPPED);
        }
        // Tip. Taller trees get a second leaf.
        leaf(level, top.above(), 1);
        if (height >= 9) leaf(level, top.above(2), 2);

        // Join the logs after everything's placed.
        for (BlockPos p : placed) set(level, p, SkinnyLogBlock.shape(level, p, level.getBlockState(p)));

        // Podzol in lichen woodlands.
        if (level.getBiome(origin).is(EcoBiomes.LICHEN_WOODLAND)) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > 3 || random.nextInt(3) == 0) continue;
                    BlockPos p = origin.offset(dx, -1, dz);
                    for (int dy = 1; dy >= -2; dy--) {
                        BlockPos q = p.above(dy);
                        BlockState s = level.getBlockState(q);
                        if (s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT) || s.is(Blocks.COARSE_DIRT)) {
                            if (level.getBlockState(q.above()).isFaceSturdy(level, q.above(), Direction.DOWN)) break;
                            set(level, q, Blocks.PODZOL.defaultBlockState());
                            break;
                        }
                    }
                }
            }
        }
        return true;
    }

    private enum Ring {
        PLUS,           // the four sides
        SQUARE,         // the 3x3 around the trunk
        SQUARE_TIPPED   // the 3x3 and one leaf further out on each side
    }

    private static void ring(WorldGenLevel level, BlockPos center, Ring ring) {
        int r = ring == Ring.SQUARE_TIPPED ? 2 : 1;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int ax = Math.abs(dx), az = Math.abs(dz);
                if (ax + az == 0) continue;
                boolean in = switch (ring) {
                    case PLUS -> ax + az == 1;
                    case SQUARE -> true;
                    case SQUARE_TIPPED -> (ax <= 1 && az <= 1) || (ax + az == 2 && (ax == 0 || az == 0));
                };
                if (in) leaf(level, center.offset(dx, 0, dz), Math.max(ax, az) + (ax > 0 && az > 0 ? 1 : 0));
            }
        }
    }

    private static void leaf(WorldGenLevel level, BlockPos pos, int distance) {
        BlockState s = level.getBlockState(pos);
        if (!s.isAir() && !(s.canBeReplaced() && s.getFluidState().isEmpty())) return;
        set(level, pos, Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, Math.max(1, Math.min(6, distance))));
    }
}
