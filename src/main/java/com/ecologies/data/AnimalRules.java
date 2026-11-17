package com.ecologies.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class AnimalRules {
    private AnimalRules() {}

    // ecologies/grazing. Fields are in DATAPACKS.md.
    public record Grazing(Matcher animal, Matcher food, int eatCooldown, int searchRange, Optional<Identifier> drop,
                          List<Integer> dropCounts, Optional<Identifier> habitat, int overcrowdedAt, int firstDropDelay) {
        public static final Codec<Grazing> CODEC = RecordCodecBuilder.create(i -> i.group(
                Matcher.CODEC.fieldOf("animal").forGetter(Grazing::animal),
                Matcher.CODEC.fieldOf("food").forGetter(Grazing::food),
                Codec.intRange(20, 1_000_000).optionalFieldOf("eat_cooldown", 2400).forGetter(Grazing::eatCooldown),
                Codec.intRange(1, 32).optionalFieldOf("search_range", 10).forGetter(Grazing::searchRange),
                Identifier.CODEC.optionalFieldOf("drop").forGetter(Grazing::drop),
                Codec.intRange(0, 64).listOf().optionalFieldOf("drop_counts", List.of(1, 1, 2, 4, 8)).forGetter(Grazing::dropCounts),
                Identifier.CODEC.optionalFieldOf("habitat").forGetter(Grazing::habitat),
                Codec.intRange(1, 1000).optionalFieldOf("overcrowded_at", 10).forGetter(Grazing::overcrowdedAt),
                Codec.intRange(0, 10_000_000).optionalFieldOf("first_drop_delay", 24000).forGetter(Grazing::firstDropDelay)
        ).apply(i, Grazing::new));
    }

    // ecologies/herding. Fields are in DATAPACKS.md.
    public record Herding(Matcher animal, float range, float spread, double speed) {
        public static final Codec<Herding> CODEC = RecordCodecBuilder.create(i -> i.group(
                Matcher.CODEC.fieldOf("animal").forGetter(Herding::animal),
                Codec.floatRange(2, 64).optionalFieldOf("range", 16F).forGetter(Herding::range),
                Codec.floatRange(1, 32).optionalFieldOf("spread", 6F).forGetter(Herding::spread),
                Codec.doubleRange(0.1, 3).optionalFieldOf("speed", 0.8).forGetter(Herding::speed)
        ).apply(i, Herding::new));
    }

    // ecologies/natural_breeding. Fields are in DATAPACKS.md.
    public record NaturalBreeding(Matcher animal, float range, int loveTicks, int memoryTicks) {
        public static final Codec<NaturalBreeding> CODEC = RecordCodecBuilder.create(i -> i.group(
                Matcher.CODEC.fieldOf("animal").forGetter(NaturalBreeding::animal),
                Codec.floatRange(2, 64).optionalFieldOf("range", 16F).forGetter(NaturalBreeding::range),
                Codec.intRange(20, 1_000_000).optionalFieldOf("love_ticks", 6000).forGetter(NaturalBreeding::loveTicks),
                Codec.intRange(20, 10_000_000).optionalFieldOf("memory_ticks", 24000).forGetter(NaturalBreeding::memoryTicks)
        ).apply(i, NaturalBreeding::new));
    }
}
