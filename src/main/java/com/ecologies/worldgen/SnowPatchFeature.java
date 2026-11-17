package com.ecologies.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SnowPatchFeature extends EcoFeature {
    @Override
    public boolean generate(WorldGenLevel level, RandomSource random, BlockPos origin) {
        int radius = 1 + random.nextInt(2) + (random.nextInt(3) == 0 ? 1 : 0);
        boolean placed = false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius || random.nextInt(3) == 0) continue;
                BlockPos top = TundraMoss.surface(level, origin.getX() + dx, origin.getZ() + dz).above();
                if (Math.abs(top.getY() - origin.getY()) > 2 || !level.getBlockState(top).isAir()) continue;
                // 3 layers in the middle, 2 around, 1 at the edge.
                int d2 = dx * dx + dz * dz;
                int layers = d2 == 0 ? 2 + random.nextInt(2) : d2 <= 2 && random.nextBoolean() ? 2 : 1;
                BlockState snow = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers);
                if (!snow.canSurvive(level, top)) continue;
                set(level, top, snow);
                placed = true;
            }
        }
        return placed;
    }
}
