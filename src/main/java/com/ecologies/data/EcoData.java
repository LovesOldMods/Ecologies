package com.ecologies.data;

import com.ecologies.Ecologies;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class EcoData<T> extends SimplePreparableReloadListener<Map<Identifier, T>> {
    private final String folder;
    private final Codec<T> codec;
    private volatile Map<Identifier, T> entries = Map.of();

    public EcoData(String folder, Codec<T> codec) {
        this.folder = folder;
        this.codec = codec;
    }

    public String folder() {
        return folder;
    }

    public Collection<T> values() {
        return entries.values();
    }

    public Map<Identifier, T> entries() {
        return entries;
    }

    public T get(Identifier id) {
        return entries.get(id);
    }

    @Override
    protected Map<Identifier, T> prepare(ResourceManager manager, ProfilerFiller profiler) {
        FileToIdConverter files = FileToIdConverter.json("ecologies/" + folder);
        Map<Identifier, T> out = new HashMap<>();
        for (Map.Entry<Identifier, Resource> file : files.listMatchingResources(manager).entrySet()) {
            Identifier id = files.fileToId(file.getKey());
            try (Reader reader = file.getValue().openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                codec.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(error -> Ecologies.LOGGER.error("ecologies/{} file {} has a problem: {}", folder, id, error))
                        .ifPresent(value -> out.put(id, value));
            } catch (Exception e) {
                Ecologies.LOGGER.error("Couldn't read ecologies/{} file {}", folder, id, e);
            }
        }
        return out;
    }

    @Override
    protected void apply(Map<Identifier, T> prepared, ResourceManager manager, ProfilerFiller profiler) {
        entries = Map.copyOf(prepared);
        Ecologies.LOGGER.info("Loaded {} ecologies/{} entries", entries.size(), folder);
    }
}
