package com.ecologies.registry;

import com.ecologies.Ecologies;
import com.ecologies.item.GuanoItem;
import com.ecologies.item.PhosphorusItem;
import com.ecologies.worldgen.RedShrub;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
//? if <1.21.5 {
/*import net.neoforged.neoforge.common.DeferredSpawnEggItem;
*///?}
//? if >=1.21.2 {
import net.minecraft.world.item.component.Consumables;
//?}

public final class EcoItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Ecologies.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Ecologies.MOD_ID);

    public static final DeferredItem<GuanoItem> GUANO = ITEMS.registerItem("guano", GuanoItem::new);
    public static final DeferredItem<PhosphorusItem> PHOSPHORUS = ITEMS.registerItem("phosphorus", PhosphorusItem::new);

    public static final DeferredItem<? extends Item> MUSKOX_SPAWN_EGG = ITEMS.registerItem("muskox_spawn_egg",
            //? if >=1.21.5 {
            SpawnEggItem::new, props -> props.spawnEgg(EcoEntities.MUSKOX.get()));
            //?} else
            /*props -> new DeferredSpawnEggItem(EcoEntities.MUSKOX, 0x4a3426, 0xd9cdb8, props));*/

    public static final DeferredItem<Item> RUSTED_WHEAT = ITEMS.registerSimpleItem("rusted_wheat");
    public static final DeferredItem<Item> POISONOUS_CARROT = ITEMS.registerItem("poisonous_carrot", Item::new,
            //? if >=1.21.2 {
            props -> props.food(Foods.POISONOUS_POTATO, Consumables.POISONOUS_POTATO));
            //?} else
            /*new Item.Properties().food(Foods.POISONOUS_POTATO));*/
    public static final DeferredItem<Item> MOLDY_BEETROOT = ITEMS.registerItem("moldy_beetroot", Item::new,
            //? if >=1.21.2 {
            props -> props.food(Foods.POISONOUS_POTATO, Consumables.POISONOUS_POTATO));
            //?} else
            /*new Item.Properties().food(Foods.POISONOUS_POTATO));*/

    public static final DeferredItem<BlockItem> GUANO_LAYER = ITEMS.registerSimpleBlockItem("guano_layer", EcoBlocks.GUANO_LAYER);
    public static final DeferredItem<BlockItem> GUANO_BLOCK = ITEMS.registerSimpleBlockItem("guano_block", EcoBlocks.GUANO_BLOCK);
    public static final DeferredItem<BlockItem> AGED_GUANO_BLOCK = ITEMS.registerSimpleBlockItem("aged_guano_block", EcoBlocks.AGED_GUANO_BLOCK);
    public static final DeferredItem<BlockItem> BAT_ROOST = ITEMS.registerSimpleBlockItem("bat_roost", EcoBlocks.BAT_ROOST);
    public static final DeferredItem<BlockItem> SPROUTED_GLOW_LICHEN = ITEMS.registerSimpleBlockItem("sprouted_glow_lichen", EcoBlocks.SPROUTED_GLOW_LICHEN);
    public static final DeferredItem<BlockItem> MUSHROOM_FARM = ITEMS.registerSimpleBlockItem("mushroom_farm", EcoBlocks.MUSHROOM_FARM);

    public static final DeferredItem<BlockItem> SMOOTH_BASALT_IRON_ORE = ITEMS.registerSimpleBlockItem("smooth_basalt_iron_ore", EcoBlocks.SMOOTH_BASALT_IRON_ORE);
    public static final DeferredItem<BlockItem> DWARF_SPRUCE_SAPLING = ITEMS.registerSimpleBlockItem("dwarf_spruce_sapling", EcoBlocks.DWARF_SPRUCE_SAPLING);
    public static final DeferredItem<BlockItem> DWARF_SPRUCE_LOG = ITEMS.registerSimpleBlockItem("dwarf_spruce_log", EcoBlocks.DWARF_SPRUCE_LOG);
    public static final DeferredItem<BlockItem> STRIPPED_DWARF_SPRUCE_LOG = ITEMS.registerSimpleBlockItem("stripped_dwarf_spruce_log", EcoBlocks.STRIPPED_DWARF_SPRUCE_LOG);
    public static final DeferredItem<BlockItem> DWARF_SPRUCE_LOG_POST_CROWN = ITEMS.registerSimpleBlockItem("dwarf_spruce_log_post_crown", EcoBlocks.DWARF_SPRUCE_LOG_POST_CROWN);
    public static final DeferredItem<BlockItem> STRIPPED_DWARF_SPRUCE_LOG_POST_CROWN = ITEMS.registerSimpleBlockItem("stripped_dwarf_spruce_log_post_crown", EcoBlocks.STRIPPED_DWARF_SPRUCE_LOG_POST_CROWN);
    //? if <26.1
    /*public static final DeferredItem<BlockItem> RED_SHRUB = ITEMS.registerSimpleBlockItem("red_shrub", EcoBlocks.RED_SHRUB);*/
    public static final DeferredItem<BlockItem> STUNTED_RED_SHRUB = ITEMS.registerSimpleBlockItem("stunted_red_shrub", EcoBlocks.STUNTED_RED_SHRUB);
    public static final DeferredItem<BlockItem> ORANGE_SHRUB = ITEMS.registerSimpleBlockItem("orange_shrub", EcoBlocks.ORANGE_SHRUB);
    public static final DeferredItem<BlockItem> YELLOW_SHRUB = ITEMS.registerSimpleBlockItem("yellow_shrub", EcoBlocks.YELLOW_SHRUB);
    public static final DeferredItem<BlockItem> RED_MOSS_BLOCK = ITEMS.registerSimpleBlockItem("red_moss_block", EcoBlocks.RED_MOSS_BLOCK);
    public static final DeferredItem<BlockItem> RED_MOSS_CARPET = ITEMS.registerSimpleBlockItem("red_moss_carpet", EcoBlocks.RED_MOSS_CARPET);
    public static final DeferredItem<BlockItem> ORANGE_MOSS_BLOCK = ITEMS.registerSimpleBlockItem("orange_moss_block", EcoBlocks.ORANGE_MOSS_BLOCK);
    public static final DeferredItem<BlockItem> ORANGE_MOSS_CARPET = ITEMS.registerSimpleBlockItem("orange_moss_carpet", EcoBlocks.ORANGE_MOSS_CARPET);
    public static final DeferredItem<BlockItem> YELLOW_MOSS_BLOCK = ITEMS.registerSimpleBlockItem("yellow_moss_block", EcoBlocks.YELLOW_MOSS_BLOCK);
    public static final DeferredItem<BlockItem> YELLOW_MOSS_CARPET = ITEMS.registerSimpleBlockItem("yellow_moss_carpet", EcoBlocks.YELLOW_MOSS_CARPET);
    public static final DeferredItem<BlockItem> WHITE_ICE = ITEMS.registerSimpleBlockItem("white_ice", EcoBlocks.WHITE_ICE);
    public static final DeferredItem<BlockItem> WHITE_PACKED_ICE = ITEMS.registerSimpleBlockItem("white_packed_ice", EcoBlocks.WHITE_PACKED_ICE);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ecologies"))
            .icon(() -> GUANO_BLOCK.get().getDefaultInstance())
            .displayItems((params, out) -> {
                for (DeferredHolder<Item, ? extends Item> item : ITEMS.getEntries()) {
                    // Hide ours if something else adds the red shrub.
                    if (item.getId().getPath().equals("red_shrub") && RedShrub.backported()) continue;
                    out.accept(item.get());
                }
            })
            .build());

    private EcoItems() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}
