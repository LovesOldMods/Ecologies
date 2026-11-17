package com.ecologies.worldgen;

import com.ecologies.EcoConfig;
import com.ecologies.registry.EcoBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
//? if >=26.1 {
import net.minecraft.world.level.block.VegetationBlock;
//?}

public final class TundraMoss {
    public enum Style {
        PATCHES,
        SUCCESSION
    }

    private TundraMoss() {}

    public static Style style() {
        return EcoConfig.SPEC.isLoaded() ? EcoConfig.TUNDRA_MOSS.get() : Style.SUCCESSION;
    }

    public static void clearPlantOnRock(WorldGenLevel level, BlockPos pos) {
        BlockState plant = level.getBlockState(pos);
        if (plant.isAir() || plant.is(Blocks.SNOW) || plant.getBlock() instanceof MultifaceBlock) return;
        if (!isRock(level.getBlockState(pos.below()))) return;
        if (!(plant.getBlock() instanceof CarpetBlock) && !isPlant(plant)) return;
        level.setBlock(pos, plant.getFluidState().createLegacyBlock(), 2);
        // Top half of tall plants.
        if (level.getBlockState(pos.above()).is(plant.getBlock())) level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
    }

    private static boolean isPlant(BlockState state) {
        //? if >=26.1 {
        return state.getBlock() instanceof VegetationBlock;
        //?} else
        /*return state.getBlock() instanceof BushBlock;*/
    }

    public static boolean isRock(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.GRAVEL) || state.is(Blocks.SMOOTH_BASALT)
                || state.is(Blocks.COBBLED_DEEPSLATE) || state.is(EcoBlocks.SMOOTH_BASALT_IRON_ORE.get())
                || state.is(Blocks.RAW_IRON_BLOCK) || state.is(Blocks.DEEPSLATE_COAL_ORE) || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.COPPER_ORES);
    }

    public static BlockPos surface(WorldGenLevel level, int x, int z) {
        return new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1, z);
    }

    public static boolean nearRock(WorldGenLevel level, BlockPos top, int range) {
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                if (dx * dx + dz * dz > range * range) continue;
                BlockPos p = surface(level, top.getX() + dx, top.getZ() + dz);
                if (Math.abs(p.getY() - top.getY()) <= 2 && isRock(level.getBlockState(p))) return true;
            }
        }
        return false;
    }

    // Value noise, cells around 40 blocks.
    public static Block colourAt(long seed, int x, int z) {
        double n = noise(seed, x / 40.0, z / 40.0) * 0.7 + noise(seed + 1, x / 13.0, z / 13.0) * 0.3;
        if (n < 0.30) return EcoBlocks.RED_MOSS_BLOCK.get();
        if (n < 0.55) return EcoBlocks.ORANGE_MOSS_BLOCK.get();
        if (n < 0.85) return EcoBlocks.YELLOW_MOSS_BLOCK.get();
        return Blocks.MOSS_BLOCK;
    }

    private static double noise(long seed, double x, double z) {
        int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
        double fx = smooth(x - x0), fz = smooth(z - z0);
        double a = hash(seed, x0, z0), b = hash(seed, x0 + 1, z0), c = hash(seed, x0, z0 + 1), d = hash(seed, x0 + 1, z0 + 1);
        return lerp(fz, lerp(fx, a, b), lerp(fx, c, d));
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double hash(long seed, int x, int z) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return (h >>> 11) / (double) (1L << 53);
    }
}
