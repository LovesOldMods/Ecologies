package com.ecologies.ecology;

import com.ecologies.EcoConfig;
import com.ecologies.Ecologies;
import com.ecologies.block.BoltingCropBlock;
import com.ecologies.registry.EcoBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class Crops {
    // About every 27 min.
    private static final int BOLT_CHANCE = 24;

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Ecologies.MOD_ID);
    public static final Supplier<AttachmentType<List<Long>>> FROZEN = ATTACHMENTS.register("frozen_crops", () ->
            AttachmentType.<List<Long>>builder(() -> List.of())
                    //? if >=1.21.9 {
                    .serialize(Codec.LONG.listOf().fieldOf("positions"))
                    //?} else
                    /*.serialize(Codec.LONG.listOf())*/
                    .build());

    private Crops() {}

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        NeoForge.EVENT_BUS.addListener(Crops::onCropGrow);
        NeoForge.EVENT_BUS.addListener(Crops::onBonemeal);
        NeoForge.EVENT_BUS.addListener(Crops::onRightClick);
    }

    private static Map<Block, Block> boltsInto() {
        return Map.of(Blocks.WHEAT, EcoBlocks.BOLTING_WHEAT.get(), Blocks.CARROTS, EcoBlocks.BOLTING_CARROTS.get(),
                Blocks.POTATOES, EcoBlocks.BOLTING_POTATOES.get(), Blocks.BEETROOTS, EcoBlocks.BOLTING_BEETROOTS.get());
    }

    // Runs during block state setup, registries might not be ready.
    public static boolean canBolt(BlockState state) {
        Block block = state.getBlock();
        return (block == Blocks.WHEAT || block == Blocks.CARROTS || block == Blocks.POTATOES || block == Blocks.BEETROOTS)
                && block instanceof CropBlock crop && crop.isMaxAge(state);
    }

    public static boolean onRipeTick(CropBlock crop, BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!crop.isMaxAge(state) || isFrozen(level, pos) || !EcoConfig.on(EcoConfig.CROP_BOLTING)) return false;
        Block bolting = boltsInto().get(crop);
        if (bolting == null) return false;
        if (random.nextInt(BOLT_CHANCE) == 0) {
            level.setBlock(pos, bolting.defaultBlockState().setValue(CropBlock.AGE, BoltingCropBlock.RIPE), Block.UPDATE_CLIENTS);
        }
        return true;
    }

    public static boolean isFrozen(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        return chunk.hasData(FROZEN) && chunk.getData(FROZEN).contains(pos.asLong());
    }

    private static void setFrozen(Level level, BlockPos pos, boolean frozen) {
        LevelChunk chunk = level.getChunkAt(pos);
        Set<Long> set = new HashSet<>(chunk.getData(FROZEN));
        // Drop frozen spots with no crop.
        set.removeIf(p -> !(level.getBlockState(BlockPos.of(p)).getBlock() instanceof CropBlock));
        if (frozen) set.add(pos.asLong());
        else set.remove(pos.asLong());
        chunk.setData(FROZEN, List.copyOf(set));
        //? if >=1.21.2 {
        chunk.markUnsaved();
        //?} else
        /*chunk.setUnsaved(true);*/
    }

    private static void onCropGrow(CropGrowEvent.Pre event) {
        if (event.getLevel() instanceof Level level && isFrozen(level, event.getPos())) {
            event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
        }
    }

    private static void onBonemeal(BonemealEvent event) {
        if (event.getState().getBlock() instanceof CropBlock && isFrozen(event.getLevel(), event.getPos())) {
            event.setSuccessful(false);
        }
    }

    private static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        var stack = event.getItemStack();
        if (!(stack.is(Items.SHEARS) || stack.is(Tags.Items.TOOLS_SHEAR))) return;
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CropBlock) || state.getBlock() instanceof BoltingCropBlock) return;
        if (!level.isClientSide()) {
            boolean freeze = !isFrozen(level, pos);
            setFrozen(level, pos, freeze);
            level.playSound(null, pos, freeze ? SoundEvents.GROWING_PLANT_CROP : SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
