package com.ecologies.worldgen;

import com.ecologies.registry.EcoBlocks;
import com.ecologies.registry.EcoTags;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public class GreenstoneBeltFeature extends EcoFeature {
    private static final int REACH = 14; // stays inside the 3x3 chunks a feature's allowed to touch

    @Override
    public boolean generate(WorldGenLevel level, RandomSource random, BlockPos origin) {
        float angle = random.nextFloat() * Mth.TWO_PI;
        int length = 14 + random.nextInt(14);
        float halfWidth = 1.0F + random.nextFloat() * 1.2F;
        // Start behind the origin so it runs through it.
        float x = origin.getX() - Mth.cos(angle) * length / 2F;
        float z = origin.getZ() - Mth.sin(angle) * length / 2F;
        boolean placed = false;
        BlockPos middle = null;
        for (int i = 0; i < length; i++) {
            angle += (random.nextFloat() - 0.5F) * 0.5F;
            x += Mth.cos(angle);
            z += Mth.sin(angle);
            if (Math.abs(x - origin.getX()) > REACH || Math.abs(z - origin.getZ()) > REACH) break;
            int r = Mth.ceil(halfWidth);
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > halfWidth * halfWidth + 0.5F) continue;
                    int bx = Mth.floor(x) + dx, bz = Mth.floor(z) + dz;
                    if (Math.abs(bx - origin.getX()) > REACH + 1 || Math.abs(bz - origin.getZ()) > REACH + 1) continue;
                    int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, bx, bz) - 1;
                    int depth = 1 + random.nextInt(3);
                    for (int dy = 0; dy < depth; dy++) {
                        BlockPos p = new BlockPos(bx, top - dy, bz);
                        if (!level.getBlockState(p).is(EcoTags.GREENSTONE_REPLACEABLE)) continue;
                        set(level, p, rock(random, 0.03F, 0.0005F)); // ore halved, raw iron blocks an eighth of what they were
                        placed = true;
                    }
                    // Neighbour chunk might have already put plants here.
                    TundraMoss.clearPlantOnRock(level, new BlockPos(bx, top + 1, bz));
                }
            }
            if (i == length / 2) middle = new BlockPos(Mth.floor(x), 0, Mth.floor(z));
        }
        if (middle != null) vein(level, random, middle);
        return placed;
    }

    private static BlockState rock(RandomSource random, float ore, float raw) {
        float f = random.nextFloat();
        if (f < raw) return Blocks.RAW_IRON_BLOCK.defaultBlockState();
        if (f < raw + ore) return EcoBlocks.SMOOTH_BASALT_IRON_ORE.get().defaultBlockState();
        return Blocks.SMOOTH_BASALT.defaultBlockState();
    }

    private static void vein(WorldGenLevel level, RandomSource random, BlockPos column) {
        int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, column.getX(), column.getZ());
        BlockPos center = new BlockPos(column.getX(), top - 6 - random.nextInt(6), column.getZ());
        int rx = 2 + random.nextInt(2), ry = 2, rz = 2 + random.nextInt(2);
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-rx, -ry, -rz), center.offset(rx, ry, rz))) {
            float nx = (p.getX() - center.getX()) / (float) rx, ny = (p.getY() - center.getY()) / (float) ry,
                    nz = (p.getZ() - center.getZ()) / (float) rz;
            if (nx * nx + ny * ny + nz * nz > 1.0F + random.nextFloat() * 0.3F) continue;
            if (!level.getBlockState(p).is(BlockTags.BASE_STONE_OVERWORLD)) continue;
            float f = random.nextFloat();
            BlockState s = f < 0.00375F ? Blocks.RAW_IRON_BLOCK.defaultBlockState()
                    : f < 0.55F ? Blocks.DEEPSLATE_IRON_ORE.defaultBlockState()
                    : Blocks.SMOOTH_BASALT.defaultBlockState();
            set(level, p, s);
        }
    }
}
