package com.ecologies.ecology;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public final class EntityData {
    private EntityData() {}

    public static long getLong(Entity entity, String key, long fallback) {
        CompoundTag tag = entity.getPersistentData();
        //? if >=1.21.5 {
        return tag.getLongOr(key, fallback);
        //?} else
        /*return tag.contains(key) ? tag.getLong(key) : fallback;*/
    }

    public static void putLong(Entity entity, String key, long value) {
        entity.getPersistentData().putLong(key, value);
    }
}
