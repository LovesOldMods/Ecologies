package com.ecologies.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;

public class CaveMouthFeature extends EcoFeature {
    @Override
    public boolean generate(WorldGenLevel level, RandomSource random, BlockPos origin) {
        // About 3 in 4 mouths get dripstone.
        if (random.nextFloat() >= 0.75F) return false;
        CaveMouth.Mouth mouth = CaveMouth.find(level, random, origin, 6);
        if (mouth == null) return false;
        CaveMouth.decorate(level, random, mouth);
        // Most of those get a colony.
        if (random.nextFloat() < 0.75F) BatColonyFeature.roostInMouth(level, random, mouth);
        return true;
    }
}
