package com.ecologies.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;
//? if >=1.21.5 {
import net.minecraft.world.level.block.BonemealSource;
//?}

public class BoltingCropBlock extends CropBlock {
    public static final int RIPE = 0, BOLTED = 1;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);
    private final Supplier<? extends Block> original;
    private final Supplier<? extends ItemLike> seed;

    public BoltingCropBlock(Properties properties, Supplier<? extends Block> original, Supplier<? extends ItemLike> seed) {
        super(properties);
        this.original = original;
        this.seed = seed;
    }

    public static boolean isBolted(BlockState state) {
        return state.getBlock() instanceof BoltingCropBlock && state.getValue(AGE) >= BOLTED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seed.get();
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(AGE) == RIPE) {
            if (random.nextInt(16) == 0) level.setBlock(pos, state.setValue(AGE, BOLTED), Block.UPDATE_CLIENTS);
            return;
        }
        // Bolted crops seed nearby farmland.
        if (random.nextInt(4) != 0) return;
        BlockPos target = pos.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
        BlockState below = level.getBlockState(target.below());
        if (level.isEmptyBlock(target) && below.is(Blocks.FARMLAND)) {
            BlockState sprout = original.get().defaultBlockState();
            if (sprout.canSurvive(level, target)) level.setBlockAndUpdate(target, sprout);
        }
    }

    // Already fully grown.
    //? if >=1.21.5 {
    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state,
                                         BonemealSource source) {
        return false;
    }
    //?} else {
    /*@Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return false;
    }
    *///?}
}
