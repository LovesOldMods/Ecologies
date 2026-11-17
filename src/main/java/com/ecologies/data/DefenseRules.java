package com.ecologies.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

// ecologies/defense. Fields are in DATAPACKS.md.
public record DefenseRules(Matcher animal, float damage, float knockback, boolean protectsYoung, float range) {
    public static final Codec<DefenseRules> CODEC = RecordCodecBuilder.create(i -> i.group(
            Matcher.CODEC.fieldOf("animal").forGetter(DefenseRules::animal),
            Codec.floatRange(0, 100).optionalFieldOf("damage", 4F).forGetter(DefenseRules::damage),
            Codec.floatRange(0, 10).optionalFieldOf("knockback", 1.6F).forGetter(DefenseRules::knockback),
            Codec.BOOL.optionalFieldOf("protects_young", true).forGetter(DefenseRules::protectsYoung),
            Codec.floatRange(2, 64).optionalFieldOf("range", 12F).forGetter(DefenseRules::range)
    ).apply(i, DefenseRules::new));
}
