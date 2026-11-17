package com.ecologies.worldgen;

import com.ecologies.Ecologies;
import com.ecologies.block.Strippable;
import com.ecologies.registry.EcoBlocks;
import com.ecologies.registry.EcoItems;
import com.ecologies.registry.EcoTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.ArrayList;
import java.util.List;
//? if <26.1 {
/*import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
*///?}

public final class TundraEvents {
    private TundraEvents() {}

    //? if >=26.1 {
    private static final ResourceKey<Feature> DWARF_SPRUCE =
            ResourceKey.create(Registries.FEATURE, Ecologies.id("dwarf_spruce"));
    //?} else
    /*private static final ResourceKey<ConfiguredFeature<?, ?>> DWARF_SPRUCE = ResourceKey.create(Registries.CONFIGURED_FEATURE, Ecologies.id("dwarf_spruce"));*/

    public static void register() {
        NeoForge.EVENT_BUS.addListener(TundraEvents::onToolUse);
        NeoForge.EVENT_BUS.addListener(TundraEvents::onSaplingGrow);
        NeoForge.EVENT_BUS.addListener(TundraEvents::onLeafDrops);
        //? if <26.1
        /*NeoForge.EVENT_BUS.addListener(TundraEvents::onChunkLoad);*/
    }

    //? if >=26.1 {
    // 26.x strips with data block transformers, ours get done here
    private static void onToolUse(PlayerInteractEvent.RightClickBlock event) {
        var stack = event.getItemStack();
        if (!stack.is(ItemTags.AXES)) return;
        var level = event.getLevel();
        var pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof Strippable s) || s.stripped() == null) return;
        var player = event.getEntity();
        level.playSound(player, pos, SoundEvents.AXE_STRIP.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
        if (!level.isClientSide()) {
            level.setBlock(pos, s.stripped().withPropertiesOf(state), 11);
            stack.hurtAndBreak(1, player, event.getHand());
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
    //?} else {
    /*private static void onToolUse(BlockEvent.BlockToolModificationEvent event) {
        if (event.getItemAbility() != ItemAbilities.AXE_STRIP) return;
        BlockState state = event.getState();
        if (state.getBlock() instanceof Strippable s && s.stripped() != null) {
            event.setFinalState(s.stripped().withPropertiesOf(state));
        }
    }
    *///?}

    private static void onLeafDrops(BlockDropsEvent event) {
        if (!event.getState().is(Blocks.SPRUCE_LEAVES) || !event.getLevel().getBiome(event.getPos()).is(EcoTags.DWARFS_TREES)) return;
        for (var drop : event.getDrops()) {
            var stack = drop.getItem();
            if (stack.is(Blocks.SPRUCE_SAPLING.asItem())) {
                drop.setItem(new ItemStack(EcoItems.DWARF_SPRUCE_SAPLING.get(), stack.getCount()));
            }
        }
    }

    private static void onSaplingGrow(BlockGrowFeatureEvent event) {
        var level = event.getLevel();
        BlockState sapling = level.getBlockState(event.getPos());
        if (sapling.is(EcoBlocks.DWARF_SPRUCE_SAPLING.get())
                || (sapling.is(Blocks.SPRUCE_SAPLING) && level.getBiome(event.getPos()).is(EcoTags.DWARFS_TREES))) {
            event.setFeature(DWARF_SPRUCE);
        }
    }

    //? if <26.1 {
    /*/^* If something else already adds the red shrub, ours in loaded chunks get swapped for it. ^/
    private static void onChunkLoad(ChunkEvent.Load event) {
        if (!RedShrub.backported() || !(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;
        Block ours = EcoBlocks.RED_SHRUB.get();
        List<BlockPos> found = new ArrayList<>();
        var sections = chunk.getSections();
        for (int i = 0; i < sections.length; i++) {
            var section = sections[i];
            if (section == null || section.hasOnlyAir() || !section.maybeHas(s -> s.is(ours))) continue;
            int baseY = chunk.getMinBuildHeight() + i * 16;
            for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
                if (section.getBlockState(x, y, z).is(ours)) found.add(chunk.getPos().getBlockAt(x, baseY + y, z));
            }
        }
        if (found.isEmpty()) return;
        BlockState theirs = RedShrub.block().defaultBlockState();
        level.getServer().execute(() -> {
            for (var pos : found) if (level.getBlockState(pos).is(ours)) level.setBlock(pos, theirs, 2);
        });
    }
    *///?}
}
