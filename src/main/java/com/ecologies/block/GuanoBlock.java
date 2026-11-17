package com.ecologies.block;

import com.ecologies.entity.ColonyBat;
import com.ecologies.registry.EcoBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class GuanoBlock extends Block {
    public static final int AGING_CHANCE = 24;

    public GuanoBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(AGING_CHANCE) != 0) return;
        // stays fresh while a colony's still living above it
        boolean colonyAbove = !level.getEntitiesOfClass(ColonyBat.class,
                new AABB(pos).inflate(6, 24, 6), Entity::isAlive).isEmpty();
        if (!colonyAbove) {
            level.setBlockAndUpdate(pos, EcoBlocks.AGED_GUANO_BLOCK.get().defaultBlockState());
        }
    }
}
