package com.ecologies.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

// ecologies/predation. Fields are in DATAPACKS.md.
public record PredatorRules(Matcher predator, Matcher prey, int hungerCooldown, boolean prefersBabies,
                            boolean eatsKill, boolean addsHunting, float preyFleeRange, double chaseSpeed,
                            double sprint, boolean stalks, int giveUpAfter, int packSize, float rushRange) {
    public static final Codec<PredatorRules> CODEC = RecordCodecBuilder.create(i -> i.group(
            Matcher.CODEC.fieldOf("predator").forGetter(PredatorRules::predator),
            Matcher.CODEC.fieldOf("prey").forGetter(PredatorRules::prey),
            Codec.intRange(0, 10_000_000).optionalFieldOf("hunger_cooldown", 24000).forGetter(PredatorRules::hungerCooldown),
            Codec.BOOL.optionalFieldOf("prefers_babies", true).forGetter(PredatorRules::prefersBabies),
            Codec.BOOL.optionalFieldOf("eats_kill", true).forGetter(PredatorRules::eatsKill),
            Codec.BOOL.optionalFieldOf("adds_hunting", false).forGetter(PredatorRules::addsHunting),
            Codec.floatRange(0, 64).optionalFieldOf("prey_flee_range", 10F).forGetter(PredatorRules::preyFleeRange),
            Codec.doubleRange(0, 4).optionalFieldOf("chase_speed", 0.0).forGetter(PredatorRules::chaseSpeed),
            Codec.doubleRange(0.5, 3).optionalFieldOf("sprint", 1.0).forGetter(PredatorRules::sprint),
            Codec.BOOL.optionalFieldOf("stalks", false).forGetter(PredatorRules::stalks),
            Codec.intRange(20, 100_000).optionalFieldOf("give_up_after", 200).forGetter(PredatorRules::giveUpAfter),
            Codec.intRange(0, 16).optionalFieldOf("pack_size", 0).forGetter(PredatorRules::packSize),
            Codec.floatRange(0, 16).optionalFieldOf("rush_range", 0F).forGetter(PredatorRules::rushRange)
    ).apply(i, PredatorRules::new));
}
