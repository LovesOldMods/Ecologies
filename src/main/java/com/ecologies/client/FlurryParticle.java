package com.ecologies.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
//? if <26.1 {
/*import net.minecraft.client.particle.TextureSheetParticle;
*///?}

public class FlurryParticle extends
        //? if >=26.1 {
        SingleQuadParticle {
        //?} else
        /*TextureSheetParticle {*/
    private final double fall, windX, windZ;
    private final float swayPhase, swaySpeed;
    private int landedTicks;

    //? if >=26.1 {
    FlurryParticle(ClientLevel level, double x, double y, double z, double windX, double windZ, SpriteSet sprites,
                   RandomSource random) {
        super(level, x, y, z, sprites.get(random));
    //?} else {
    /*FlurryParticle(ClientLevel level, double x, double y, double z, double windX, double windZ, SpriteSet sprites) {
        super(level, x, y, z);
        pickSprite(sprites);
    *///?}
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.fall = 0.035 + this.random.nextFloat() * 0.03;
        this.windX = windX;
        this.windZ = windZ;
        this.swayPhase = this.random.nextFloat() * Mth.TWO_PI;
        this.swaySpeed = 0.06F + this.random.nextFloat() * 0.06F;
        this.quadSize = 0.06F + this.random.nextFloat() * 0.05F;
        this.lifetime = 200 + this.random.nextInt(120);
        this.xd = windX;
        this.yd = -fall;
        this.zd = windZ;
        setColor(0.95F, 0.97F, 1.0F);
    }

    //? if >=26.1 {
    @Override
    public Layer getLayer() {
        return Layer.TRANSLUCENT;
    }
    //?} else {
    /*@Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
    *///?}

    @Override
    public void tick() {
        float sway = Mth.sin(age * swaySpeed + swayPhase);
        if (onGround) {
            // landed, stop drifting and fade out
            xd = 0;
            zd = 0;
            if (++landedTicks > 30) remove();
            setAlpha(1.0F - landedTicks / 30.0F);
        } else {
            xd = windX + sway * 0.025;
            zd = windZ + Mth.cos(age * swaySpeed * 0.7F + swayPhase) * 0.02;
            yd = -fall * (0.85 + 0.15 * sway); // flakes flutter: a touch slower at each turn of the sway
        }
        if (age > lifetime - 20) setAlpha(Math.max(0.0F, (lifetime - age) / 20.0F));
        super.tick();
        // super.tick() messes with the speeds so put ours back
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        //? if >=26.1 {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double windX, double unused, double windZ, RandomSource random) {
            return new FlurryParticle(level, x, y, z, windX, windZ, sprites, random);
        }
        //?} else {
        /*@Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double windX, double unused, double windZ) {
            return new FlurryParticle(level, x, y, z, windX, windZ, sprites);
        }
        *///?}
    }
}
