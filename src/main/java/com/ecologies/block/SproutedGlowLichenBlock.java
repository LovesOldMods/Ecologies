package com.ecologies.block;

import com.ecologies.Compat;
import com.ecologies.EcoConfig;
import com.ecologies.data.EcoDataTypes;
import com.ecologies.data.SubstrateRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlowLichenBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.List;
//? if <1.21.2 {
/*import net.minecraft.world.ItemInteractionResult;
*///?}
//? if >=1.21.5 {
import net.minecraft.world.level.block.BonemealSource;
//?}

public class SproutedGlowLichenBlock extends GlowLichenBlock {
    public static final BooleanProperty SNIPPED = BooleanProperty.create("snipped");
    private static final int CROWDING_RADIUS = 4;

    public SproutedGlowLichenBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(SNIPPED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SNIPPED);
    }

    public BlockState copyFrom(BlockState lichen) {
        BlockState out = defaultBlockState();
        for (Property<?> property : lichen.getProperties()) {
            if (out.hasProperty(property)) out = copy(lichen, out, property);
        }
        return out;
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return !state.getValue(SNIPPED);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(SNIPPED) || !EcoConfig.on(EcoConfig.LICHEN_BREAKDOWN)) return;
        SubstrateRules rules = EcoDataTypes.substrateRules(state);
        if (rules == null || random.nextFloat() >= rules.tickChance()) return;
        if (rules.needsSky() && (!Compat.isDay(level) || level.getBrightness(LightLayer.SKY, pos) < 12)) return;

        // only the blocks it's on that can still break down
        List<Direction> eating = new ArrayList<>();
        for (Direction face : Direction.values()) {
            if (isAttachedTo(state, face) && rules.isSubstrate(level.getBlockState(pos.relative(face)))) eating.add(face);
        }
        if (eating.isEmpty()) return;  // nothing left to eat

        if (random.nextFloat() < rules.spreadChance()) {
            spread(level, pos, random, rules);
        } else {
            BlockPos target = pos.relative(eating.get(random.nextInt(eating.size())));
            BlockState next = rules.nextStage(level.getBlockState(target));
            if (next != null) {
                level.setBlockAndUpdate(target, next);
                level.levelEvent(2001, target, Block.getId(next)); // crumbling particles
            }
        }
    }

    private void spread(ServerLevel level, BlockPos pos, RandomSource random, SubstrateRules rules) {
        if (countLichen(level, pos) >= rules.crowdingLimit()) return;
        BlockState spreadState = rules.spreadBlock();
        if (!(spreadState.getBlock() instanceof MultifaceBlock)) return;
        int r = rules.spreadRadius();
        for (int attempt = 0; attempt < 8; attempt++) {
            BlockPos picked = pos.offset(random.nextInt(2 * r + 1) - r, random.nextInt(2 * r + 1) - r, random.nextInt(2 * r + 1) - r);
            // try the picked spot and its neighbours instead of the whole area
            if (tryPlace(level, picked, rules, spreadState)) return;
            for (Direction d : Direction.values()) {
                if (tryPlace(level, picked.relative(d), rules, spreadState)) return;
            }
        }
    }

    private static boolean tryPlace(ServerLevel level, BlockPos target, SubstrateRules rules, BlockState spreadState) {
        BlockState existing = level.getBlockState(target);
        boolean isSame = existing.is(spreadState.getBlock());
        if (!existing.isAir() && !isSame && !existing.is(Blocks.WATER)) return false;
        for (Direction face : Direction.values()) {
            BlockPos against = target.relative(face);
            BlockState substrate = level.getBlockState(against);
            if (!rules.isSubstrate(substrate) || !substrate.isFaceSturdy(level, against, face.getOpposite())) continue;
            BlockState base = isSame ? existing : spreadState;
            BooleanProperty faceProperty = MultifaceBlock.getFaceProperty(face);
            if (!base.hasProperty(faceProperty) || base.getValue(faceProperty)) continue;
            BlockState placed = base.setValue(faceProperty, true);
            if (existing.is(Blocks.WATER) && placed.hasProperty(BlockStateProperties.WATERLOGGED)) {
                placed = placed.setValue(BlockStateProperties.WATERLOGGED, true);
            }
            level.setBlockAndUpdate(target, placed);
            return true;
        }
        return false;
    }

    private static int countLichen(ServerLevel level, BlockPos pos) {
        int count = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-CROWDING_RADIUS, -CROWDING_RADIUS, -CROWDING_RADIUS),
                pos.offset(CROWDING_RADIUS, CROWDING_RADIUS, CROWDING_RADIUS))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof GlowLichenBlock) count++;
        }
        return count;
    }

    private static boolean isAttachedTo(BlockState state, Direction face) {
        BooleanProperty property = MultifaceBlock.getFaceProperty(face);
        return state.hasProperty(property) && state.getValue(property);
    }

    // shears stop it, same as cave vines

    private boolean snip(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (state.getValue(SNIPPED) || !(stack.is(Items.SHEARS) || stack.is(Tags.Items.TOOLS_SHEAR))) return false;
        if (!level.isClientSide()) {
            level.setBlockAndUpdate(pos, state.setValue(SNIPPED, true));
            level.playSound(null, pos, SoundEvents.GROWING_PLANT_CROP, SoundSource.BLOCKS, 1.0F, 1.0F);
            //? if >=1.21.2 {
            stack.hurtAndBreak(1, player, hand);
            //?} else
            /*stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));*/
        }
        return true;
    }

    //? if >=1.21.2 {
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                                              InteractionHand hand, BlockHitResult hit) {
        return snip(stack, state, level, pos, player, hand)
                ? InteractionResult.SUCCESS
                : super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
    //?} else {
    /*@Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                                  Player player, InteractionHand hand, BlockHitResult hit) {
        return snip(stack, state, level, pos, player, hand)
                ? ItemInteractionResult.sidedSuccess(level.isClientSide())
                : super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
    *///?}

    // no bone meal, it already spreads on its own and it'd be way too cheap

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
