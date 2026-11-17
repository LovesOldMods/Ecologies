package com.ecologies.worldgen;

import com.ecologies.registry.EcoBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;

// 1.21.1: use the other red shrub if something already adds one.
public final class RedShrub {
    private RedShrub() {}

    //? if >=26.1 {
    public static boolean backported() {
        return false;
    }

    public static Block block() {
        return Blocks.RED_SHRUB;
    }
    //?} else {
    /*private static Block backport;
    private static boolean checked;

    private static Block backport() {
        if (!checked && ModList.get() != null) {
            checked = true;
            // Not in vanilla 1.21.1, if it's here something added it.
            var id = Identifier.fromNamespaceAndPath("minecraft", "red_shrub");
            if (BuiltInRegistries.BLOCK.containsKey(id)) {
                backport = BuiltInRegistries.BLOCK.get(id);
            }
        }
        return backport;
    }

    public static boolean backported() {
        return backport() != null;
    }

    public static Block block() {
        Block b = backport();
        return b != null ? b : EcoBlocks.RED_SHRUB.get();
    }
    *///?}
}
