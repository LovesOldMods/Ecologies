package com.ecologies.client;

import com.ecologies.Ecologies;
import com.ecologies.entity.Muskox;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
//? if <1.21.2 {
/*import net.minecraft.client.renderer.MultiBufferSource;
*///?}
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
//?}

public class MuskoxRenderer
        //? if >=1.21.2 {
        extends AgeableMobRenderer<Muskox, MuskoxRenderState, MuskoxModel> {
        //?} else
        /*extends MobRenderer<Muskox, MuskoxModel<Muskox>> {*/
    private static final Identifier BROWN = Ecologies.id("textures/entity/muskox/muskox_brown.png");
    private static final Identifier BLACK = Ecologies.id("textures/entity/muskox/muskox_black.png");
    private static final Identifier BABY = Ecologies.id("textures/entity/muskox/muskox_baby.png");

    private static Identifier texture(boolean baby, int variant) {
        return baby ? BABY : variant == Muskox.BLACK ? BLACK : BROWN;
    }

    //? if >=1.21.2 {
    public MuskoxRenderer(EntityRendererProvider.Context context) {
        super(context, new MuskoxModel(context.bakeLayer(MuskoxModel.ADULT)), new MuskoxModel(context.bakeLayer(MuskoxModel.BABY)), 0.9F);
    }

    @Override
    public MuskoxRenderState createRenderState() {
        return new MuskoxRenderState();
    }

    @Override
    public void extractRenderState(Muskox muskox, MuskoxRenderState state, float partialTicks) {
        super.extractRenderState(muskox, state, partialTicks);
        state.variant = muskox.getVariant();
        state.headDown = muskox.headDownAmount(partialTicks);
        state.nibble = muskox.grazeNibble(partialTicks);
        state.ram = muskox.ramAmount(partialTicks);
    }

    @Override
    public Identifier getTextureLocation(MuskoxRenderState state) {
        return texture(state.isBaby, state.variant);
    }
    //?} else {
    /*private final MuskoxModel<Muskox> adult;
    private final MuskoxModel<Muskox> baby;

    public MuskoxRenderer(EntityRendererProvider.Context context) {
        super(context, new MuskoxModel<>(context.bakeLayer(MuskoxModel.ADULT)), 0.9F);
        this.adult = this.model;
        this.baby = new MuskoxModel<>(context.bakeLayer(MuskoxModel.BABY));
    }

    @Override
    public void render(Muskox muskox, float entityYaw, float partialTicks, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        this.model = muskox.isBaby() ? baby : adult;
        super.render(muskox, entityYaw, partialTicks, pose, buffers, light);
    }

    @Override
    public Identifier getTextureLocation(Muskox muskox) {
        return texture(muskox.isBaby(), muskox.getVariant());
    }
    *///?}
}
