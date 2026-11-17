package com.ecologies.ecology;

import com.ecologies.Compat;
import com.ecologies.EcoConfig;
import com.ecologies.block.entity.BatRoostBlockEntity;
import com.ecologies.entity.ColonyBat;
import com.ecologies.registry.EcoBlocks;
import com.ecologies.registry.EcoEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
//? if <1.21.2 {
/*import net.minecraft.world.entity.MobSpawnType;
*///?}
//? if >=1.21.2 {
import net.minecraft.world.entity.EntitySpawnReason;
//?}
//? if >=26.1 {
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
//?}

public final class BatEvents {
    private static final int CALM_RANGE = 24;
    private static final int PHANTOM_RANGE = 48;

    private BatEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(BatEvents::onSpawnCheck);
        NeoForge.EVENT_BUS.addListener(BatEvents::onJoin);
        NeoForge.EVENT_BUS.addListener(BatEvents::onBreak);
    }

    private static boolean isNatural(MobSpawnEvent.PositionCheck event) {
        //? if >=1.21.2 {
        return event.getSpawnType() == EntitySpawnReason.NATURAL;
        //?} else
        /*return event.getSpawnType() == MobSpawnType.NATURAL;*/
    }

    private static int batsNear(Level level, double x, double y, double z, int range) {
        return level.getEntitiesOfClass(ColonyBat.class, new AABB(x - range, y - range, z - range, x + range, y + range, z + range),
                ColonyBat::isAlive).size();
    }

    private static void onSpawnCheck(MobSpawnEvent.PositionCheck event) {
        if (!EcoConfig.on(EcoConfig.BAT_COLONIES)) return;
        if (!isNatural(event) || !(event.getLevel().getLevel() instanceof Level level)) return;
        var mob = event.getEntity();
        if (Compat.isType(mob, "bat")) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL); // bats live in colonies now
            return;
        }
        if (mob.getType().getCategory() != MobCategory.MONSTER) return;
        int bats = batsNear(level, mob.getX(), mob.getY(), mob.getZ(), CALM_RANGE);
        if (bats == 0) return;
        float keep = bats > 8 ? 0.25F : 0.5F; // half as many monsters near bats, a quarter near a big colony
        if (level.getRandom().nextFloat() >= keep) event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && EcoConfig.on(EcoConfig.BAT_COLONIES)
                && Compat.isType(event.getEntity(), "bat")
                && event.getLevel() instanceof ServerLevel server) {
            // Swap vanilla bats for colony bats.
            event.setCanceled(true);
            var old = (Bat) event.getEntity();
            server.getServer().execute(() -> replace(server, old));
            return;
        }
        if (event.loadedFromDisk() || event.getLevel().isClientSide() || !EcoConfig.on(EcoConfig.BAT_COLONIES)) return;
        var entity = event.getEntity();
        if (Compat.isType(entity, "phantom")
                && batsNear(event.getLevel(), entity.getX(), entity.getY(), entity.getZ(), PHANTOM_RANGE) > 0) {
            event.setCanceled(true); // bats keep the night sky clear
        }
    }

    private static void replace(ServerLevel level, Bat old) {
        //? if >=1.21.2 {
        ColonyBat bat = EcoEntities.COLONY_BAT.get().create(level, EntitySpawnReason.CONVERSION);
        //?} else
        /*ColonyBat bat = EcoEntities.COLONY_BAT.get().create(level);*/
        if (bat == null) return;
        Compat.place(bat, old.getX(), old.getY(), old.getZ(), old.getYRot());
        bat.setResting(old.isResting());
        if (old.hasCustomName()) {
            bat.setCustomName(old.getCustomName());
            bat.setCustomNameVisible(old.isCustomNameVisible());
        }
        if (old.isPersistenceRequired()) bat.setPersistenceRequired();
        bat.setHealth(Math.min(bat.getMaxHealth(), old.getHealth()));
        level.addFreshEntity(bat);
    }

    //? if >=26.1 {
    private static void onBreak(BreakBlockEvent event) {
    //?} else
    /*private static void onBreak(BlockEvent.BreakEvent event) {*/
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) return;
        BlockState state = event.getState();
        int layers;
        if (state.is(EcoBlocks.GUANO_LAYER.get())) layers = state.getValue(SnowLayerBlock.LAYERS);
        else if (state.is(EcoBlocks.GUANO_BLOCK.get()) || state.is(EcoBlocks.AGED_GUANO_BLOCK.get())) layers = 8;
        else return;
        BlockPos pos = event.getPos();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-8, -2, -8), pos.offset(8, 24, 8))) {
            if (level.getBlockEntity(p) instanceof BatRoostBlockEntity roost) {
                roost.recordHarvest(level, layers);
            }
        }
    }
}
