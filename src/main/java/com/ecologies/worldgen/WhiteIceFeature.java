package com.ecologies.worldgen;

import com.ecologies.registry.EcoBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockState;

public class WhiteIceFeature extends EcoFeature {
    @Override
    public boolean generate(WorldGenLevel level, RandomSource random, BlockPos origin) {
        BlockPos.MutableBlockPos top = new BlockPos.MutableBlockPos();
        int cx = origin.getX() & ~15, cz = origin.getZ() & ~15;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = cx + dx, z = cz + dz;
                top.set(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
                Holder<Biome> biome = level.getBiome(top);
                if (!EcoBiomes.isTundra(biome)) continue;
                // Clear snow from the neighbouring biomes' freeze pass, it runs before this.
                for (int dy = 0; dy >= -1; dy--) {
                    BlockPos p = top.offset(0, dy, 0);
                    if (level.getBlockState(p).is(Blocks.SNOW)) {
                        set(level, p, Blocks.AIR.defaultBlockState());
                        BlockState under = level.getBlockState(p.below());
                        if (under.hasProperty(BlockStateProperties.SNOWY)) set(level, p.below(), under.setValue(BlockStateProperties.SNOWY, false));
                    }
                }
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                BlockPos below = new BlockPos(x, y - 1, z);
                BlockState surface = level.getBlockState(below);
                if (surface.is(Blocks.ICE) || biome.value().shouldFreeze(level, below, false)) {
                    set(level, below, EcoBlocks.WHITE_ICE.get().defaultBlockState());
                } else if (surface.is(Blocks.PACKED_ICE)) {
                    set(level, below, EcoBlocks.WHITE_PACKED_ICE.get().defaultBlockState());
                }
            }
        }
        return true;
    }
}
