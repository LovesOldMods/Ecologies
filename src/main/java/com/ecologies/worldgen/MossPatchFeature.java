package com.ecologies.worldgen;

import com.ecologies.registry.EcoBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class MossPatchFeature extends EcoFeature {
    @Override
    public boolean generate(WorldGenLevel level, RandomSource random, BlockPos origin) {
        // Main layout for PATCHES, just the odd patch for SUCCESSION.
        if (TundraMoss.style() != TundraMoss.Style.PATCHES && random.nextInt(8) != 0) return false;
        Block moss = TundraMoss.colourAt(level.getSeed(), origin.getX(), origin.getZ());
        Block carpet = carpetFor(moss);
        int radius = 2 + random.nextInt(2);
        boolean placed = false;
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > (radius + 1) * (radius + 1)) continue;
                if (d2 > radius * radius && random.nextFloat() > 0.8F) continue; // ragged edge
                BlockPos top = TundraMoss.surface(level, origin.getX() + dx, origin.getZ() + dz);
                var state = level.getBlockState(top);
                if (!(state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT))) continue;
                if (Math.abs(top.getY() - origin.getY()) > 3) continue;
                set(level, top, moss.defaultBlockState());
                if (random.nextFloat() < 0.25F && level.getBlockState(top.above()).isAir()) {
                    set(level, top.above(), carpet.defaultBlockState());
                }
                placed = true;
            }
        }
        return placed;
    }

    static Block carpetFor(Block moss) {
        if (moss == EcoBlocks.RED_MOSS_BLOCK.get()) return EcoBlocks.RED_MOSS_CARPET.get();
        if (moss == EcoBlocks.ORANGE_MOSS_BLOCK.get()) return EcoBlocks.ORANGE_MOSS_CARPET.get();
        if (moss == EcoBlocks.YELLOW_MOSS_BLOCK.get()) return EcoBlocks.YELLOW_MOSS_CARPET.get();
        return Blocks.MOSS_CARPET;
    }
}
