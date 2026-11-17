package com.ecologies.registry;

import com.ecologies.Ecologies;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EcoParticles {
    private static final DeferredRegister<ParticleType<?>> TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, Ecologies.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLURRY = TYPES.register("flurry", () -> new SimpleParticleType(false));

    private EcoParticles() {}

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
