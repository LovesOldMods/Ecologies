package com.ecologies.registry;

import com.ecologies.Ecologies;
import com.ecologies.block.entity.BatRoostBlockEntity;
import com.ecologies.block.entity.MushroomFarmBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EcoBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Ecologies.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MushroomFarmBlockEntity>> MUSHROOM_FARM =
            TYPES.register("mushroom_farm", () ->
                    //? if >=1.21.2 {
                    new BlockEntityType<>(MushroomFarmBlockEntity::new, EcoBlocks.MUSHROOM_FARM.get())
                    //?} else
                    /*BlockEntityType.Builder.of(MushroomFarmBlockEntity::new, EcoBlocks.MUSHROOM_FARM.get()).build(null)*/
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatRoostBlockEntity>> BAT_ROOST =
            TYPES.register("bat_roost", () ->
                    //? if >=1.21.2 {
                    new BlockEntityType<>(BatRoostBlockEntity::new, EcoBlocks.BAT_ROOST.get())
                    //?} else
                    /*BlockEntityType.Builder.of(BatRoostBlockEntity::new, EcoBlocks.BAT_ROOST.get()).build(null)*/
            );

    private EcoBlockEntities() {}

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
