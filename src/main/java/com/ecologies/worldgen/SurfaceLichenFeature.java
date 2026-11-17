package com.ecologies.worldgen;

import com.ecologies.registry.EcoBlocks;
import com.ecologies.registry.EcoTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public class SurfaceLichenFeature extends EcoFeature {
    @Override
    public boolean generate(WorldGenLevel level, RandomSource random, BlockPos origin) {
        Block lichen = random.nextInt(4) == 0 ? Blocks.GLOW_LICHEN : EcoBlocks.SPROUTED_GLOW_LICHEN.get();
        boolean placed = false;
        int size = 3 + random.nextInt(6);
        boolean tundra = EcoBiomes.isTundra(level.getBiome(origin));
        for (int i = 0; i < size * 4; i++) {
            int x = origin.getX() + random.nextInt(7) - 3;
            int z = origin.getZ() + random.nextInt(7) - 3;
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z), z);
            if (!level.getBlockState(pos).isAir()) continue;
            BlockPos below = pos.below();
            BlockState ground = level.getBlockState(below);
            if (!ground.isFaceSturdy(level, below, Direction.UP)) continue;
            if (tundra) {
                // tundra lichen mostly stays on the rock, only a bit spills onto the edge
                if (!TundraMoss.isRock(ground) && (random.nextInt(4) != 0 || !TundraMoss.nearRock(level, below, 1))) continue;
            } else if (!(ground.is(BlockTags.BASE_STONE_OVERWORLD) || ground.is(Blocks.SMOOTH_BASALT) || ground.is(BlockTags.DIRT)
                    || ground.is(EcoTags.MOSS) || ground.is(Blocks.GRAVEL) || ground.is(Blocks.COBBLED_DEEPSLATE))) continue;
            set(level, pos, lichen.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true));
            placed = true;
        }
        return placed;
    }
}
