package com.ecologies.mixin;

import com.ecologies.ecology.Crops;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// basically keeps ripe crops ticking so they can bolt
@Mixin(CropBlock.class)
public abstract class CropBlockMixin {
    @Inject(method = "isRandomlyTicking", at = @At("HEAD"), cancellable = true)
    private void ecologies$keepTickingWhenRipe(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (Crops.canBolt(state)) cir.setReturnValue(true);
    }

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void ecologies$bolt(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (Crops.onRipeTick((CropBlock) (Object) this, state, level, pos, random)) ci.cancel();
    }
}
