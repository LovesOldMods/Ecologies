package com.ecologies.registry;

import com.ecologies.Ecologies;
import com.ecologies.entity.ColonyBat;
import com.ecologies.entity.Muskox;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EcoEntities {
    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Ecologies.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<Muskox>> MUSKOX = TYPES.register("muskox", () ->
            EntityType.Builder.of(Muskox::new, MobCategory.CREATURE).sized(1.4F, 1.6F).clientTrackingRange(10)
                    //? if >=1.21.2 {
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Ecologies.id("muskox"))));
                    //?} else
                    /*.build("muskox"));*/

    public static final DeferredHolder<EntityType<?>, EntityType<ColonyBat>> COLONY_BAT = TYPES.register("colony_bat", () ->
            EntityType.Builder.of(ColonyBat::new, MobCategory.AMBIENT).sized(0.5F, 0.9F).clientTrackingRange(5)
                    //? if >=1.21.2 {
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Ecologies.id("colony_bat"))));
                    //?} else
                    /*.build("colony_bat"));*/

    private EcoEntities() {}

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        modBus.addListener(EcoEntities::onAttributes);
        modBus.addListener(EcoEntities::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(MUSKOX.get(), Muskox.createAttributes().build());
        event.put(COLONY_BAT.get(), ColonyBat.createColonyAttributes().build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(MUSKOX.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Muskox::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
