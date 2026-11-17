package com.ecologies.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class MossSuccessionFeature extends EcoFeature {
    @Override
    public boolean generate(WorldGenLevel level, RandomSource random, BlockPos origin) {
        if (TundraMoss.style() != TundraMoss.Style.SUCCESSION) return false;
        int cx = origin.getX() & ~15, cz = origin.getZ() & ~15;
        long seed = level.getSeed();
        boolean placed = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = cx + dx, z = cz + dz;
                BlockPos top = TundraMoss.surface(level, x, z);
                if (!EcoBiomes.isTundra(level.getBiome(top))) continue;
                var state = level.getBlockState(top);
                if (!(state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT))) continue;
                // 3 wide, sometimes 4, sometimes broken, I don't want a clean outline
                int width = random.nextInt(6) == 0 ? 4 : 3;
                if (random.nextInt(10) == 0 || !TundraMoss.nearRock(level, top, width)) continue;
                Block moss = TundraMoss.colourAt(seed, x, z);
                set(level, top, moss.defaultBlockState());
                if (random.nextFloat() < 0.12F && level.getBlockState(top.above()).isAir()) {
                    set(level, top.above(), MossPatchFeature.carpetFor(moss).defaultBlockState());
                }
                placed = true;
            }
        }
        return placed;
    }
}
