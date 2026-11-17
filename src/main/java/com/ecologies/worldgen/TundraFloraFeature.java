package com.ecologies.worldgen;

import com.ecologies.registry.EcoBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public class TundraFloraFeature extends EcoFeature {
    @Override
    public boolean generate(WorldGenLevel level, RandomSource random, BlockPos origin) {
        boolean placed = false;
        for (int i = 0; i < 24; i++) {
            int x = origin.getX() + random.nextInt(9) - 4;
            int z = origin.getZ() + random.nextInt(9) - 4;
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z), z);
            if (!level.getBlockState(pos).isAir()) {
                TundraMoss.clearPlantOnRock(level, pos); // e.g. a vanilla mushroom patch that landed on bare rock
                continue;
            }
            BlockState under = level.getBlockState(pos.below());
            BlockState plant = level.getBiome(pos).is(EcoBiomes.LICHEN_WOODLAND) && random.nextInt(3) == 0
                    && (under.is(Blocks.GRASS_BLOCK) || under.is(Blocks.PODZOL)) ? dryGrass(random) : pick(under, random);
            if (plant == null || !plant.canSurvive(level, pos)) continue;
            set(level, pos, plant);
            placed = true;
        }
        return placed;
    }

    private static final int NORMAL = 6, SEMI_RARE = 2, RARE = 1;

    private static BlockState pick(BlockState ground, RandomSource random) {
        Block g = ground.getBlock();
        boolean carpet = random.nextInt(4) == 0;
        Block red = RedShrub.block(), orange = EcoBlocks.ORANGE_SHRUB.get(), yellow = EcoBlocks.YELLOW_SHRUB.get();
        if (g == EcoBlocks.RED_MOSS_BLOCK.get()) {
            return carpet ? EcoBlocks.RED_MOSS_CARPET.get().defaultBlockState() : weighted(random, red, NORMAL, orange, RARE, yellow, RARE);
        }
        if (g == EcoBlocks.ORANGE_MOSS_BLOCK.get()) {
            return carpet ? EcoBlocks.ORANGE_MOSS_CARPET.get().defaultBlockState() : weighted(random, red, SEMI_RARE, orange, NORMAL, yellow, SEMI_RARE);
        }
        if (g == EcoBlocks.YELLOW_MOSS_BLOCK.get()) {
            return carpet ? EcoBlocks.YELLOW_MOSS_CARPET.get().defaultBlockState() : weighted(random, red, 0, orange, SEMI_RARE, yellow, NORMAL);
        }
        if (g == Blocks.MOSS_BLOCK) {
            return carpet ? Blocks.MOSS_CARPET.defaultBlockState() : weighted(random, red, RARE, orange, SEMI_RARE, yellow, SEMI_RARE);
        }
        if (g == Blocks.GRASS_BLOCK) {
            // Grass mostly stays bare.
            return random.nextInt(4) == 0 ? weighted(random, red, 0, orange, RARE, yellow, SEMI_RARE) : null;
        }
        if (g == Blocks.DEEPSLATE || g == Blocks.GRAVEL || g == Blocks.TUFF || g == Blocks.SMOOTH_BASALT || g == Blocks.COARSE_DIRT) {
            // rock only gets lichen (SurfaceLichenFeature handles that) and the odd bit of snow
            return random.nextInt(6) == 0 ? Blocks.SNOW.defaultBlockState()
                    .setValue(SnowLayerBlock.LAYERS, 1 + random.nextInt(2)) : null;
        }
        return null;
    }

    // 1.21.1 doesn't have dry grass so dead bushes it is
    private static BlockState dryGrass(RandomSource random) {
        String id = random.nextInt(4) == 0 ? "tall_dry_grass" : "short_dry_grass";
        return BuiltInRegistries.BLOCK
                .getOptional(Identifier.fromNamespaceAndPath("minecraft", id))
                .orElse(Blocks.DEAD_BUSH).defaultBlockState();
    }

    private static BlockState weighted(RandomSource random, Block red, int redWeight, Block orange, int orangeWeight,
                                       Block yellow, int yellowWeight) {
        int r = random.nextInt(redWeight + orangeWeight + yellowWeight);
        if (r < redWeight) return red.defaultBlockState();
        if (r < redWeight + orangeWeight) return orange.defaultBlockState();
        return yellow.defaultBlockState();
    }
}
