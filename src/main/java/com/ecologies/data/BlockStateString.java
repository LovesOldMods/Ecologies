package com.ecologies.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

// Command style strings. Vanilla's block state JSON changes between versions.
public final class BlockStateString {
    public static final Codec<BlockState> CODEC = Codec.STRING.comapFlatMap(BlockStateString::parse, BlockStateString::write);

    private BlockStateString() {}

    public static DataResult<BlockState> parse(String text) {
        String trimmed = text.trim();
        int bracket = trimmed.indexOf('[');
        String idPart = bracket < 0 ? trimmed : trimmed.substring(0, bracket);
        Identifier id = Identifier.tryParse(idPart);
        if (id == null) return DataResult.error(() -> "Bad block id: " + idPart);
        Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(id);
        if (block.isEmpty()) return DataResult.error(() -> "Unknown block: " + id);
        BlockState state = block.get().defaultBlockState();
        if (bracket < 0) return DataResult.success(state);
        if (!trimmed.endsWith("]")) return DataResult.error(() -> "Missing ] in " + text);
        String inside = trimmed.substring(bracket + 1, trimmed.length() - 1);
        for (String pair : inside.split(",")) {
            if (pair.isBlank()) continue;
            String[] kv = pair.split("=", 2);
            if (kv.length != 2) return DataResult.error(() -> "Bad property '" + pair + "' in " + text);
            Property<?> property = block.get().getStateDefinition().getProperty(kv[0].trim());
            if (property == null) return DataResult.error(() -> "Unknown property '" + kv[0] + "' in " + text);
            DataResult<BlockState> set = with(state, property, kv[1].trim());
            if (set.error().isPresent()) return set;
            state = set.getOrThrow();
        }
        return DataResult.success(state);
    }

    private static <T extends Comparable<T>> DataResult<BlockState> with(BlockState state, Property<T> property, String value) {
        Optional<T> parsed = property.getValue(value);
        return parsed.map(v -> DataResult.success(state.setValue(property, v)))
                .orElseGet(() -> DataResult.error(() -> "Bad value '" + value + "' for " + property.getName()));
    }

    public static String write(BlockState state) {
        StringBuilder out = new StringBuilder(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        if (!state.getProperties().isEmpty()) {
            out.append('[');
            boolean first = true;
            for (Property<?> property : state.getProperties()) {
                if (!first) out.append(',');
                first = false;
                out.append(property.getName()).append('=').append(name(state, property));
            }
            out.append(']');
        }
        return out.toString();
    }

    private static <T extends Comparable<T>> String name(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }
}
