package com.ecologies.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
//? if <26.1 {
/*import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
*///?}

// 26.x: feature and feature type. 1.21.1: configured feature and feature.
//? if >=26.1 {
public abstract class EcoFeature implements Feature {
    private final MapCodec<EcoFeature> codec = MapCodec.unit(this);

    public MapCodec<EcoFeature> typeCodec() {
        return codec;
    }

    @Override
    public MapCodec<? extends Feature> codec() {
        return codec;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        return generate(level, random, origin);
    }
//?} else {
/*public abstract class EcoFeature extends Feature<NoneFeatureConfiguration> {
    protected EcoFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return generate(context.level(), context.random(), context.origin());
    }
*///?}

    public abstract boolean generate(WorldGenLevel level, RandomSource random, BlockPos origin);

    // No neighbour shape updates.
    protected static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 2 | 16);
    }
}
