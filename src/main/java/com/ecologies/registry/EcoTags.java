package com.ecologies.registry;

import com.ecologies.Ecologies;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

public final class EcoTags {
    public static final TagKey<Block> SHRUB_SOIL = block("shrub_soil");
    public static final TagKey<Block> MOSS = block("moss");
    public static final TagKey<Block> MOSS_SPREADS_ONTO = block("moss_spreads_onto");
    public static final TagKey<Block> DWARF_SPRUCE_SOIL = block("dwarf_spruce_soil");
    public static final TagKey<Block> GREENSTONE_REPLACEABLE = block("greenstone_replaceable");
    public static final TagKey<Block> ROOST_CEILING = block("roost_ceiling");

    public static final TagKey<Biome> DWARFS_TREES = biome("dwarfs_trees");
    public static final TagKey<Biome> HAS_BAT_COLONIES = biome("has_bat_colonies");

    private static TagKey<Block> block(String name) {
        return TagKey.create(Registries.BLOCK, Ecologies.id(name));
    }

    private static TagKey<Biome> biome(String name) {
        return TagKey.create(Registries.BIOME, Ecologies.id(name));
    }

    private EcoTags() {}
}
