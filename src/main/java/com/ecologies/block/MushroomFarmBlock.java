package com.ecologies.block;

import com.ecologies.block.entity.MushroomFarmBlockEntity;
import com.ecologies.registry.EcoBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
//? if <1.21.2 {
/*import net.minecraft.world.ItemInteractionResult;
*///?}

public class MushroomFarmBlock extends Block implements EntityBlock {
    // composter style bin, 2px floor and walls
    private static final VoxelShape INSIDE = Block.box(2, 2, 2, 14, 16, 14);
    private static final VoxelShape SHAPE =
            Shapes.join(Shapes.block(), INSIDE,
                    BooleanOp.ONLY_FIRST);

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level,
                                                                   BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    public static final int EMPTY = 0, GROWING = 1, FULL = 2;
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", EMPTY, FULL);
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 7);

    public MushroomFarmBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, EMPTY).setValue(FILL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE, FILL);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MushroomFarmBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != EcoBlockEntities.MUSHROOM_FARM.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<MushroomFarmBlockEntity>) MushroomFarmBlockEntity::serverTick;
    }

    // server decides, client just guesses
    private boolean handleItem(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
        int stage = state.getValue(STAGE);
        if (level.isClientSide()) return stage != FULL;  // the client doesn't have the datapack
        if (!(level.getBlockEntity(pos) instanceof MushroomFarmBlockEntity farm)) return false;
        return switch (stage) {
            case EMPTY -> farm.tryPlant(stack, player);
            case GROWING -> farm.tryFeed(stack, player);
            default -> farm.tryHarvest();
        };
    }

    private boolean handleEmptyHand(BlockState state, Level level, BlockPos pos, Player player) {
        int stage = state.getValue(STAGE);
        boolean wouldAct = stage == FULL || (stage == GROWING && player.isShiftKeyDown());
        if (level.isClientSide()) return wouldAct;
        if (!wouldAct || !(level.getBlockEntity(pos) instanceof MushroomFarmBlockEntity farm)) return false;
        return stage == FULL ? farm.tryHarvest() : farm.tryRemove();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return handleEmptyHand(state, level, pos, player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    //? if >=1.21.2 {
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        return handleItem(stack, state, level, pos, player) ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
    }
    //?} else {
    /*@Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                                  Player player, InteractionHand hand, BlockHitResult hit) {
        return handleItem(stack, state, level, pos, player)
                ? ItemInteractionResult.sidedSuccess(level.isClientSide())
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MushroomFarmBlockEntity farm) {
            farm.dropPlanted();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
    *///?}
}
