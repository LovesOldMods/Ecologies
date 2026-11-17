package com.ecologies.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import com.ecologies.registry.EcoTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
//? if >=1.21.5
import net.minecraft.world.level.block.BonemealSource;
//? if >=1.21.5 {
import net.minecraft.world.level.block.VegetationBlock;
//?}

public class ShrubBlock extends
        //? if >=1.21.5 {
        VegetationBlock
        //?} else
        /*BushBlock*/
        implements BonemealableBlock {
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 10, 15);

    public ShrubBlock(Properties properties) {
        super(properties);
    }

    //? if <1.21.2 {
    /*public static final MapCodec<ShrubBlock> CODEC = simpleCodec(ShrubBlock::new);

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }
    *///?}

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(EcoTags.SHRUB_SOIL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    private BlockPos freeSpot(LevelReader level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 8; i++) {
            BlockPos p = pos.offset(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
            if (level.getBlockState(p).isAir() && defaultBlockState().canSurvive(level, p)) return p;
        }
        return null;
    }

    private boolean hasFreeSpot(LevelReader level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (level.getBlockState(p).isAir() && defaultBlockState().canSurvive(level, p)) return true;
        }
        return false;
    }

    //? if >=1.21.5 {
    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        return hasFreeSpot(level, pos);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        grow(level, random, pos);
    }
    //?} else {
    /*@Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return hasFreeSpot(level, pos);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        grow(level, random, pos);
    }
    *///?}

    private void grow(ServerLevel level, RandomSource random, BlockPos pos) {
        BlockPos p = freeSpot(level, pos, random);
        if (p != null) level.setBlock(p, defaultBlockState(), 3);
    }
}
