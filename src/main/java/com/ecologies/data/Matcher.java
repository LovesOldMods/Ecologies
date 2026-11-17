package com.ecologies.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

// Plain strings, checked on use. Keeps the JSON the same across versions.
public final class Matcher {
    public static final Matcher NONE = new Matcher(List.of());
    public static final Codec<Matcher> CODEC = Codec.either(Codec.STRING, Codec.STRING.listOf()).xmap(
            either -> new Matcher(either.map(List::of, list -> list)),
            matcher -> Either.right(matcher.raw));

    private final List<String> raw;
    private final Set<Identifier> ids = new HashSet<>();
    private final List<Identifier> tags = new ArrayList<>();

    public Matcher(List<String> entries) {
        this.raw = List.copyOf(entries);
        for (String entry : entries) {
            if (entry.startsWith("#")) tags.add(Identifier.parse(entry.substring(1)));
            else ids.add(Identifier.parse(entry));
        }
    }

    public boolean isEmpty() {
        return raw.isEmpty();
    }

    public boolean test(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (ids.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()))) return true;
        for (Identifier tag : tags) if (stack.is(TagKey.create(Registries.ITEM, tag))) return true;
        return false;
    }

    public boolean test(BlockState state) {
        if (ids.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) return true;
        for (Identifier tag : tags) if (state.is(TagKey.create(Registries.BLOCK, tag))) return true;
        return false;
    }

    public boolean test(EntityType<?> type) {
        if (ids.contains(BuiltInRegistries.ENTITY_TYPE.getKey(type))) return true;
        for (Identifier tag : tags) if (type.builtInRegistryHolder().is(TagKey.create(Registries.ENTITY_TYPE, tag))) return true;
        return false;
    }

    public Item firstItem() {
        for (String entry : raw) {
            if (!entry.startsWith("#")) {
                return BuiltInRegistries.ITEM.getOptional(Identifier.parse(entry)).orElse(null);
            }
        }
        return null;
    }

    public Block firstBlock() {
        for (String entry : raw) {
            if (!entry.startsWith("#")) {
                return BuiltInRegistries.BLOCK.getOptional(Identifier.parse(entry)).orElse(null);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return raw.toString();
    }
}
