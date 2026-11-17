package com.ecologies;

import com.ecologies.worldgen.TundraMoss;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class EcoConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue PREDATION = BUILDER
            .comment("Predators hunt when they're hungry, then take a break (data/<ns>/ecologies/predation).")
            .define("predation", true);
    public static final ModConfigSpec.BooleanValue CROP_BOLTING = BUILDER
            .comment("Ripe crops left too long will bolt: less food, more poisonous stuff, and they spread.")
            .define("cropBolting", true);
    public static final ModConfigSpec.BooleanValue BAT_COLONIES = BUILDER
            .comment("Normal bats stop spawning since bats live in colonies now. Fewer monsters spawn near bats,",
                    "and phantoms won't spawn near them at all.")
            .define("batColonies", true);
    public static final ModConfigSpec.BooleanValue LICHEN_BREAKDOWN = BUILDER
            .comment("Sprouted glow lichen slowly breaks down the blocks it's on and spreads.")
            .define("lichenBreakdown", true);
    public static final ModConfigSpec.BooleanValue HERDS = BUILDER
            .comment("Grazing, herding and herds breeding on their own (data/<ns>/ecologies/grazing, herding, natural_breeding).")
            .define("herds", true);

    public static final ModConfigSpec.EnumValue<TundraMoss.Style> TUNDRA_MOSS = BUILDER
            .comment("How moss gets laid out in new tundra chunks. SUCCESSION follows the design video: moss rings the",
                    "bare rock (lichen, then moss, then shrubs), its colours slowly shift across the land, and every so",
                    "often there's a loose patch out on the grass.",
                    "PATCHES scatters ragged moss patches all over the grass instead.")
            .defineEnum("tundraMoss", TundraMoss.Style.SUCCESSION);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private EcoConfig() {}

    // Treat it as on until the config loads.
    public static boolean on(ModConfigSpec.BooleanValue value) {
        return !SPEC.isLoaded() || value.get();
    }
}
