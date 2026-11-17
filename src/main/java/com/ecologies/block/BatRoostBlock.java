package com.ecologies.block;

import com.ecologies.block.entity.BatRoostBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BatRoostBlock extends Block implements EntityBlock {
    public BatRoostBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BatRoostBlockEntity(pos, state);
    }
}
