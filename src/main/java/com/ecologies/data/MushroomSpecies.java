package com.ecologies.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

// ecologies/mushroom_farm. Fields are in DATAPACKS.md.
public record MushroomSpecies(Matcher input, Identifier result, Optional<BlockState> display, int growthTime,
                              int minYield, int maxYield, List<Accelerator> accelerators, List<Extra> extras) {

    public record Extra(Identifier item, float chance, int min, int max) {
        public static final Codec<Extra> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("item").forGetter(Extra::item),
                Codec.floatRange(0, 1).optionalFieldOf("chance", 1F).forGetter(Extra::chance),
                Codec.intRange(0, 64).optionalFieldOf("min", 1).forGetter(Extra::min),
                Codec.intRange(0, 64).optionalFieldOf("max", 1).forGetter(Extra::max)
        ).apply(i, Extra::new));
    }

    // Decomposers only.
    public record Accelerator(Matcher items, int ticks) {
        public static final Codec<Accelerator> CODEC = RecordCodecBuilder.create(i -> i.group(
                Matcher.CODEC.fieldOf("items").forGetter(Accelerator::items),
                Codec.intRange(1, 1_000_000).fieldOf("ticks").forGetter(Accelerator::ticks)
        ).apply(i, Accelerator::new));
    }

    public static final List<Accelerator> DEFAULT_ACCELERATORS = List.of(
            new Accelerator(new Matcher(List.of("minecraft:rotten_flesh")), 2400),
            new Accelerator(new Matcher(List.of("#minecraft:logs")), 1200),
            new Accelerator(new Matcher(List.of("minecraft:stick")), 300));

    public static final Codec<MushroomSpecies> CODEC = RecordCodecBuilder.create(i -> i.group(
            Matcher.CODEC.fieldOf("input").forGetter(MushroomSpecies::input),
            Identifier.CODEC.fieldOf("result").forGetter(MushroomSpecies::result),
            BlockStateString.CODEC.optionalFieldOf("display").forGetter(MushroomSpecies::display),
            Codec.intRange(20, 10_000_000).optionalFieldOf("growth_time", 12000).forGetter(MushroomSpecies::growthTime),
            Codec.intRange(0, 64).optionalFieldOf("min_yield", 2).forGetter(MushroomSpecies::minYield),
            Codec.intRange(0, 64).optionalFieldOf("max_yield", 4).forGetter(MushroomSpecies::maxYield),
            Accelerator.CODEC.listOf().optionalFieldOf("accelerators", DEFAULT_ACCELERATORS).forGetter(MushroomSpecies::accelerators),
            Extra.CODEC.listOf().optionalFieldOf("extras", List.of()).forGetter(MushroomSpecies::extras)
    ).apply(i, MushroomSpecies::new));

    public Item resultItem() {
        return BuiltInRegistries.ITEM.getOptional(result).orElse(Items.AIR);
    }

    public BlockState displayState() {
        if (display.isPresent()) return display.get();
        Item item = input.firstItem();
        if (item instanceof BlockItem blockItem) return blockItem.getBlock().defaultBlockState();
        return Blocks.AIR.defaultBlockState();
    }

    public int accelerationFor(ItemStack stack) {
        for (Accelerator accelerator : accelerators) {
            if (accelerator.items().test(stack)) return accelerator.ticks();
        }
        return 0;
    }
}
