package com.ecologies;

import com.ecologies.worldgen.TundraMoss;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class EcoConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue PREDATION = BUILDER
            .comment("Predators only hunt when they're hungry and take a break after a kill (data/<ns>/ecologies/predation).")
            .define("predation", true);
    public static final ModConfigSpec.BooleanValue CROP_BOLTING = BUILDER
            .comment("Ripe crops left too long will bolt, you get less food, some of it's poisonous and it spreads.")
            .define("cropBolting", true);
    public static final ModConfigSpec.BooleanValue BAT_COLONIES = BUILDER
            .comment("Bats live in colonies now so normal bats stop spawning. Fewer monsters spawn near bats",
                    "and phantoms won't spawn near them at all.")
            .define("batColonies", true);
    public static final ModConfigSpec.BooleanValue LICHEN_BREAKDOWN = BUILDER
            .comment("Sprouted glow lichen slowly breaks down whatever it's growing on and spreads.")
            .define("lichenBreakdown", true);
    public static final ModConfigSpec.BooleanValue HERDS = BUILDER
            .comment("Animals grazing, sticking together in herds and breeding back on their own (data/<ns>/ecologies/grazing, herding, natural_breeding).")
            .define("herds", true);

    public static final ModConfigSpec.EnumValue<TundraMoss.Style> TUNDRA_MOSS = BUILDER
            .comment("How moss gets laid out in new tundra chunks. SUCCESSION is basically what the design video does,",
                    "moss rings the bare rock and its colours slowly shift across the land.",
                    "PATCHES just scatters ragged moss patches all over the grass instead.")
            .defineEnum("tundraMoss", TundraMoss.Style.SUCCESSION);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private EcoConfig() {}

    // just treat it as on until the config loads
    public static boolean on(ModConfigSpec.BooleanValue value) {
        return !SPEC.isLoaded() || value.get();
    }
}
