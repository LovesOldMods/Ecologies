package com.ecologies.client;

import com.ecologies.block.MushroomFarmBlock;
import com.ecologies.block.entity.MushroomFarmBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
//? if <1.21.9 {
/*import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
*///?}
//? if >=1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
//?}

//? if >=1.21.9 {
public class MushroomFarmRenderer implements BlockEntityRenderer<MushroomFarmBlockEntity, MushroomFarmRenderer.State> {
    public static class State extends BlockEntityRenderState {
        public MovingBlockRenderState block;
    }

    public MushroomFarmRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MushroomFarmBlockEntity farm, State state, float partialTicks, Vec3 camera,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(farm, state, partialTicks, camera, breakProgress);
        state.block = null;
        BlockState display = farm.display();
        if (farm.getBlockState().getValue(MushroomFarmBlock.STAGE) != MushroomFarmBlock.FULL || display.isAir()) return;
        if (!(farm.getLevel() instanceof ClientLevel level)) return;
        var block = new MovingBlockRenderState();
        block.randomSeedPos = farm.getBlockPos();
        block.blockPos = farm.getBlockPos().above(); // lit like the space above the farm
        block.blockState = display;
        block.biome = level.getBiome(farm.getBlockPos());
        block.cardinalLighting = level.cardinalLighting();
        block.lightEngine = level.getLightEngine();
        state.block = block;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        if (state.block == null) return;
        pose.pushPose();
        placeOnTop(pose);
        collector.submitMovingBlock(pose, state.block, 0);
        pose.popPose();
    }
//?} else {
/*public class MushroomFarmRenderer implements BlockEntityRenderer<MushroomFarmBlockEntity> {
    private final BlockRenderDispatcher blocks;

    public MushroomFarmRenderer(BlockEntityRendererProvider.Context context) {
        this.blocks = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(MushroomFarmBlockEntity farm, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        BlockState display = farm.display();
        if (farm.getBlockState().getValue(MushroomFarmBlock.STAGE) != MushroomFarmBlock.FULL || display.isAir()) return;
        int lightAbove = farm.getLevel() == null ? light
                : LevelRenderer.getLightColor(farm.getLevel(), farm.getBlockPos().above());
        pose.pushPose();
        placeOnTop(pose);
        blocks.renderSingleBlock(display, pose, buffers, lightAbove, overlay);
        pose.popPose();
    }
*///?}

    private static void placeOnTop(PoseStack pose) {
        float scale = 0.75F;
        pose.translate(0.5F - scale / 2, 15F / 16F, 0.5F - scale / 2);
        pose.scale(scale, scale, scale);
    }
}
