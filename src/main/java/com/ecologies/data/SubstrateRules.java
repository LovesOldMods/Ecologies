package com.ecologies.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.level.block.state.BlockState;

// ecologies/substrate. Fields are in DATAPACKS.md.
public record SubstrateRules(Matcher actor, boolean needsSky, float tickChance, float spreadChance, BlockState spreadBlock,
                             int spreadRadius, int crowdingLimit, List<Step> breakdown) {
    public record Step(Matcher from, BlockState to) {
        public static final Codec<Step> CODEC = RecordCodecBuilder.create(i -> i.group(
                Matcher.CODEC.fieldOf("from").forGetter(Step::from),
                BlockStateString.CODEC.fieldOf("to").forGetter(Step::to)
        ).apply(i, Step::new));
    }

    public static final Codec<SubstrateRules> CODEC = RecordCodecBuilder.create(i -> i.group(
            Matcher.CODEC.fieldOf("actor").forGetter(SubstrateRules::actor),
            Codec.BOOL.optionalFieldOf("needs_sky", true).forGetter(SubstrateRules::needsSky),
            Codec.floatRange(0, 1).optionalFieldOf("tick_chance", 0.25F).forGetter(SubstrateRules::tickChance),
            Codec.floatRange(0, 1).optionalFieldOf("spread_chance", 0.5F).forGetter(SubstrateRules::spreadChance),
            BlockStateString.CODEC.fieldOf("spread_block").forGetter(SubstrateRules::spreadBlock),
            Codec.intRange(1, 32).optionalFieldOf("spread_radius", 8).forGetter(SubstrateRules::spreadRadius),
            Codec.intRange(1, 1000).optionalFieldOf("crowding_limit", 12).forGetter(SubstrateRules::crowdingLimit),
            Step.CODEC.listOf().fieldOf("breakdown").forGetter(SubstrateRules::breakdown)
    ).apply(i, SubstrateRules::new));

    public boolean isSubstrate(BlockState state) {
        return nextStage(state) != null;
    }

    public BlockState nextStage(BlockState state) {
        for (Step step : breakdown) {
            if (step.from().test(state)) return step.to();
        }
        return null;
    }
}
