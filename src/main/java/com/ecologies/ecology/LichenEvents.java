package com.ecologies.ecology;

import com.ecologies.block.SproutedGlowLichenBlock;
import com.ecologies.registry.EcoBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;

public final class LichenEvents {
    private LichenEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(LichenEvents::onBonemeal);
    }

    private static void onBonemeal(BonemealEvent event) {
        if (!event.getState().is(Blocks.GLOW_LICHEN)) return;
        if (event.getLevel() instanceof ServerLevel level) {
            SproutedGlowLichenBlock sprouted = EcoBlocks.SPROUTED_GLOW_LICHEN.get();
            level.setBlockAndUpdate(event.getPos(), sprouted.copyFrom(event.getState()));
            level.playSound(null, event.getPos(), SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.levelEvent(1505, event.getPos(), 15); // bone meal particles
            event.getStack().shrink(1);
        }
        event.setSuccessful(true);
    }
}
