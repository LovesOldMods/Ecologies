package com.ecologies.worldgen;

import com.ecologies.Ecologies;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public final class EcoBiomes {
    public static final ResourceKey<Biome> TUNDRA = ResourceKey.create(Registries.BIOME, Ecologies.id("tundra"));
    public static final ResourceKey<Biome> LICHEN_WOODLAND = ResourceKey.create(Registries.BIOME, Ecologies.id("lichen_woodland"));

    public static boolean isTundra(Holder<Biome> biome) {
        return biome.is(TUNDRA);
    }

    private EcoBiomes() {}
}
