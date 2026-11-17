package com.ecologies.worldgen;

import com.ecologies.Ecologies;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EcoFeatures {
    public static final DwarfSpruceFeature DWARF_SPRUCE = new DwarfSpruceFeature();
    public static final GreenstoneBeltFeature GREENSTONE_BELT = new GreenstoneBeltFeature();
    public static final TundraFloraFeature TUNDRA_FLORA = new TundraFloraFeature();
    public static final SurfaceLichenFeature SURFACE_LICHEN = new SurfaceLichenFeature();
    public static final WhiteIceFeature WHITE_ICE = new WhiteIceFeature();
    public static final BatColonyFeature BAT_COLONY = new BatColonyFeature();
    public static final MossPatchFeature MOSS_PATCH = new MossPatchFeature();
    public static final MossSuccessionFeature MOSS_SUCCESSION = new MossSuccessionFeature();
    public static final SnowPatchFeature SNOW_PATCH = new SnowPatchFeature();
    public static final CaveMouthFeature CAVE_MOUTH = new CaveMouthFeature();

    //? if >=26.1 {
    private static final DeferredRegister<MapCodec<? extends Feature>> TYPES =
            DeferredRegister.create(Registries.FEATURE_TYPE, Ecologies.MOD_ID);

    private static void add(String name, EcoFeature feature) {
        TYPES.register(name, feature::typeCodec);
    }
    //?} else {
    /*private static final DeferredRegister<Feature<?>> TYPES =
            DeferredRegister.create(Registries.FEATURE, Ecologies.MOD_ID);

    private static void add(String name, EcoFeature feature) {
        TYPES.register(name, () -> feature);
    }
    *///?}

    static {
        add("dwarf_spruce", DWARF_SPRUCE);
        add("greenstone_belt", GREENSTONE_BELT);
        add("tundra_flora", TUNDRA_FLORA);
        add("surface_lichen", SURFACE_LICHEN);
        add("white_ice", WHITE_ICE);
        add("bat_colony", BAT_COLONY);
        add("moss_patch", MOSS_PATCH);
        add("moss_succession", MOSS_SUCCESSION);
        add("snow_patch", SNOW_PATCH);
        add("cave_mouth_dripstone", CAVE_MOUTH);
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }

    private EcoFeatures() {}
}
