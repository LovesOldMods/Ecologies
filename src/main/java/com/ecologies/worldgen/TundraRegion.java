package com.ecologies.worldgen;

import com.ecologies.Ecologies;
import com.mojang.datafixers.util.Pair;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

public class TundraRegion extends Region {
    public static final int WEIGHT = 12;

    public TundraRegion() {
        super(Ecologies.id("overworld"), RegionType.OVERWORLD, WEIGHT);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        // Snowy plains and ice spikes become tundra, the dry half of snowy taiga becomes lichen woodland.
        addModifiedVanillaOverworldBiomes(pair -> {
            ResourceKey<Biome> biome = pair.getSecond();
            Climate.ParameterPoint p = pair.getFirst();
            if (biome == Biomes.SNOWY_PLAINS || biome == Biomes.ICE_SPIKES) {
                mapper.accept(Pair.of(p, EcoBiomes.TUNDRA));
            } else if (biome == Biomes.SNOWY_TAIGA) {
                long min = p.humidity().min(), max = p.humidity().max(), mid = (min + max) / 2;
                mapper.accept(Pair.of(withHumidity(p, min, mid), EcoBiomes.LICHEN_WOODLAND));
                mapper.accept(Pair.of(withHumidity(p, mid, max), Biomes.SNOWY_TAIGA));
            } else {
                mapper.accept(pair);
            }
        }, builder -> {});
    }

    private static Climate.ParameterPoint withHumidity(Climate.ParameterPoint p, long min, long max) {
        return new Climate.ParameterPoint(p.temperature(), new Climate.Parameter(min, max), p.continentalness(), p.erosion(),
                p.depth(), p.weirdness(), p.offset());
    }
}
