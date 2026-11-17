package com.ecologies.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

// ecologies/roost. Fields are in DATAPACKS.md.
public record RoostRules(Matcher mob, Matcher roost, Matcher lure, Matcher forage, int overharvestLimit,
                         int recoveryTicksPerLayer, int abandonTicks) {
    public static final RoostRules DEFAULT = new RoostRules(new Matcher(List.of("ecologies:colony_bat")),
            new Matcher(List.of("ecologies:bat_roost")), new Matcher(List.of("minecraft:spider_eye")),
            new Matcher(List.of("#ecologies:bat_forage_crops")), 32, 3000, 72000);

    public static final Codec<RoostRules> CODEC = RecordCodecBuilder.create(i -> i.group(
            Matcher.CODEC.fieldOf("mob").forGetter(RoostRules::mob),
            Matcher.CODEC.fieldOf("roost").forGetter(RoostRules::roost),
            Matcher.CODEC.optionalFieldOf("lure", DEFAULT.lure()).forGetter(RoostRules::lure),
            Matcher.CODEC.optionalFieldOf("forage", DEFAULT.forage()).forGetter(RoostRules::forage),
            Codec.intRange(1, 100_000).optionalFieldOf("overharvest_limit", 32).forGetter(RoostRules::overharvestLimit),
            Codec.intRange(1, 10_000_000).optionalFieldOf("recovery_ticks_per_layer", 3000).forGetter(RoostRules::recoveryTicksPerLayer),
            Codec.intRange(0, 100_000_000).optionalFieldOf("abandon_ticks", 72000).forGetter(RoostRules::abandonTicks)
    ).apply(i, RoostRules::new));
}
