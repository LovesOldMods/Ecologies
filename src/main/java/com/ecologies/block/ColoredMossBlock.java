package com.ecologies.block;

import com.ecologies.registry.EcoTags;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.5
import net.minecraft.world.level.block.BonemealSource;

public class ColoredMossBlock extends Block implements BonemealableBlock {
    private final Supplier<? extends Block> carpet;

    public ColoredMossBlock(Properties properties, Supplier<? extends Block> carpet) {
        super(properties);
        this.carpet = carpet;
    }

    private static boolean open(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.above()).isAir();
    }

    //? if >=1.21.5 {
    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        return open(level, pos);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        spread(level, random, pos);
    }
    //?} else {
    /*@Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return open(level, pos);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        spread(level, random, pos);
    }
    *///?}

    private void spread(ServerLevel level, RandomSource random, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-3, -1, -3), pos.offset(3, 1, 3))) {
            double d = Math.sqrt(p.distSqr(pos));
            if (random.nextDouble() * 3.5 < d) continue;
            BlockState s = level.getBlockState(p);
            if (!open(level, p)) continue;
            if (s.is(EcoTags.MOSS_SPREADS_ONTO)) {
                level.setBlock(p, defaultBlockState(), 3);
                s = defaultBlockState();
            }
            if (s.is(this) && random.nextInt(6) == 0) {
                level.setBlock(p.above(), carpet.get().defaultBlockState(), 3);
            }
        }
    }
}
