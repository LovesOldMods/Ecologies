package com.ecologies;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.5 {
import net.minecraft.world.level.block.BonemealSource;
//?}

public final class Compat {
    private Compat() {}

    public static boolean isDay(Level level) {
        //? if >=1.21.6 {
        return level.isBrightOutside();
        //?} else
        /*return level.isDay();*/
    }

    public static void hurt(ServerLevel level, Entity target,
                            DamageSource source, float amount) {
        //? if >=1.21.2 {
        target.hurtServer(level, source, amount);
        //?} else
        /*target.hurt(source, amount);*/
    }

    // checking by id since the EntityType constants got moved around in 26.x
    public static boolean isType(Entity entity, String vanillaId) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key.getNamespace().equals("minecraft") && key.getPath().equals(vanillaId);
    }

    public static int minY(Level level) {
        //? if >=1.21.2 {
        return level.getMinY();
        //?} else
        /*return level.getMinBuildHeight();*/
    }

    public static void place(Entity entity, double x, double y, double z, float yaw) {
        //? if >=1.21.5 {
        entity.snapTo(x, y, z, yaw, 0.0F);
        //?} else
        /*entity.moveTo(x, y, z, yaw, 0.0F);*/
    }

    public static boolean bonemeal(ServerLevel level, BlockPos pos,
                                   BlockState state) {
        if (!(state.getBlock() instanceof BonemealableBlock block)) return false;
        //? if >=1.21.5 {
        var source = BonemealSource.MOB;
        if (!block.isValidBonemealTarget(level, pos, state, source)) return false;
        block.performBonemeal(level, level.getRandom(), pos, state, source);
        //?} else {
        /*if (!block.isValidBonemealTarget(level, pos, state)) return false;
        block.performBonemeal(level, level.getRandom(), pos, state);
        *///?}
        return true;
    }

    public static BlockBehaviour.Properties noCollision(BlockBehaviour.Properties properties) {
        //? if >=1.21.2 {
        return properties.noCollision();
        //?} else
        /*return properties.noCollission();*/
    }

    public static String path(ResourceKey<?> key) {
        //? if >=26.1 {
        return key.identifier().getPath();
        //?} else
        /*return key.location().getPath();*/
    }
}
